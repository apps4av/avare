/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Fetches NOTAMs from the FAA production NMS-API the same way AvareX does
 * ({@code notam_cache.dart}): client-credentials OAuth (host without
 * {@code /nmsapi}), then GeoJSON NOTAMs for each location, with AIXM
 * as a fallback.
 *
 * {@link #CLIENT_ID_SECRET} is replaced at build time from the
 * {@code FAA_NMS_API_CLIENT_ID_SECRET} GitHub Actions secret. The value
 * must be {@code clientId:clientSecret} (not already Base64-encoded).
 */
public final class FaaNmsNotams {

    /**
     * Raw {@code clientId:clientSecret}, replaced at build time
     * (matches the AvareX placeholder).
     */
    public static final String CLIENT_ID_SECRET =
            "@@__faa_nms_api_client_id_secret__@@";

    // Auth is on the host without /nmsapi. Data calls use the /nmsapi base path.
    private static final String TOKEN_URL =
            "https://api-nms.aim.faa.gov/v1/auth/token";
    private static final String NOTAM_URL =
            "https://api-nms.aim.faa.gov/nmsapi/v1/notams?location=";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    private static String sAccessToken;
    private static long sAccessTokenExpiresAtMs;

    private FaaNmsNotams() { }

    /**
     * Fetch NOTAMs for a comma-separated list of ICAO ids
     * (e.g. {@code KBOS,KLWM}) and return HTML for the plan brief.
     * Returns {@code null} when the API key is missing or the service
     * cannot be reached.
     */
    public static String fetchHtml(String plan) {
        String text = fetchText(plan);
        if (text == null) {
            return null;
        }
        return text.replaceAll("(\r\n|\n)", "<br />");
    }

    /**
     * Same fetch as AvareX, returning plain text (one NOTAM per line,
     * airports separated by a blank line). {@code null} on auth/network
     * failure; empty string when the query succeeds but finds nothing.
     */
    public static String fetchText(String plan) {
        if (plan == null || plan.trim().isEmpty()) {
            return "";
        }
        if (!hasApiKey()) {
            return null;
        }

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (Exception e) {
            return null;
        }
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }

        String[] icaos = plan.split(",");
        List<String> blocks = new ArrayList<>();
        int failures = 0;
        for (String raw : icaos) {
            String icao = raw.trim();
            if (icao.isEmpty()) {
                continue;
            }
            try {
                String block = fetchLocation(accessToken, icao);
                if (block != null && !block.isEmpty()) {
                    blocks.add(block);
                }
            } catch (UnauthorizedException e) {
                clearAccessToken();
                try {
                    accessToken = getAccessToken();
                    if (accessToken == null || accessToken.isEmpty()) {
                        failures++;
                        continue;
                    }
                    String block = fetchLocation(accessToken, icao);
                    if (block != null && !block.isEmpty()) {
                        blocks.add(block);
                    }
                } catch (Exception retry) {
                    failures++;
                }
            } catch (Exception e) {
                failures++;
            }
        }

        if (blocks.isEmpty()) {
            return failures > 0 ? null : "";
        }
        return join(blocks, "\n\n");
    }

    static boolean hasApiKey() {
        return CLIENT_ID_SECRET != null
                && !CLIENT_ID_SECRET.isEmpty()
                && !CLIENT_ID_SECRET.startsWith("@@");
    }

    private static synchronized String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (sAccessToken != null && now < sAccessTokenExpiresAtMs) {
            return sAccessToken;
        }

        String token = requestAccessToken();
        if (token == null || token.isEmpty()) {
            clearAccessToken();
            return null;
        }
        return token;
    }

    private static synchronized void clearAccessToken() {
        sAccessToken = null;
        sAccessTokenExpiresAtMs = 0;
    }

    private static synchronized void storeAccessToken(String token, int expiresInSec) {
        sAccessToken = token;
        int ttl = expiresInSec > 60 ? expiresInSec - 60 : expiresInSec;
        sAccessTokenExpiresAtMs = System.currentTimeMillis() + ttl * 1000L;
    }

    private static String requestAccessToken() throws Exception {
        HttpURLConnection conn = null;
        try {
            byte[] body = "grant_type=client_credentials"
                    .getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "Basic " + basicAuth());
            conn.setFixedLengthStreamingMode(body.length);
            OutputStream out = conn.getOutputStream();
            out.write(body);
            out.close();

            if (conn.getResponseCode() != 200) {
                return null;
            }
            JSONObject json = new JSONObject(readFully(conn.getInputStream()));
            String token = json.optString("access_token", null);
            if (token == null || token.isEmpty()) {
                return null;
            }
            int expiresIn = 1799;
            if (json.has("expires_in")) {
                expiresIn = json.optInt("expires_in", 1799);
                if (expiresIn <= 0) {
                    try {
                        expiresIn = Integer.parseInt(
                                json.optString("expires_in", "1799"));
                    } catch (NumberFormatException e) {
                        expiresIn = 1799;
                    }
                }
            }
            storeAccessToken(token, expiresIn);
            return token;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String basicAuth() {
        return Base64.encodeToString(
                CLIENT_ID_SECRET.getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP);
    }

    private static String fetchLocation(String accessToken, String icao)
            throws Exception {
        HttpURLConnection conn = null;
        try {
            String url = NOTAM_URL
                    + URLEncoder.encode(icao, "UTF-8");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("nmsResponseFormat", "GEOJSON");

            int code = conn.getResponseCode();
            if (code == 401) {
                throw new UnauthorizedException();
            }
            if (code != 200) {
                throw new Exception("NOTAM HTTP " + code);
            }

            JSONObject root = new JSONObject(readFully(conn.getInputStream()));
            JSONObject data = root.optJSONObject("data");
            if (data == null || data.length() == 0) {
                return "";
            }

            List<String> lines = new ArrayList<>();
            JSONArray geojson = data.optJSONArray("geojson");
            if (geojson != null) {
                for (int i = 0; i < geojson.length(); i++) {
                    JSONObject feature = geojson.optJSONObject(i);
                    String formatted = extractGeoJsonText(feature);
                    if (formatted != null && !formatted.isEmpty()) {
                        lines.add(formatted);
                    }
                }
            }

            if (lines.isEmpty()) {
                JSONArray aixm = data.optJSONArray("aixm");
                if (aixm != null) {
                    for (int i = 0; i < aixm.length(); i++) {
                        String xml = aixm.optString(i, "");
                        String formatted = extractFormattedText(xml);
                        if (formatted != null && !formatted.isEmpty()) {
                            lines.add(formatted);
                        }
                    }
                }
            }
            if (lines.isEmpty()) {
                return "";
            }
            return "<b>" + icao + "</b>\n" + join(lines, "\n\n");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Parses an AIXM NOTAM XML string into one user-readable line per
     * contained NOTAM, matching AvareX {@code extractFormattedText}.
     */
    static String extractFormattedText(String xmlString) {
        if (xmlString == null || xmlString.isEmpty()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            try {
                factory.setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature(
                        "http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature(
                        "http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
            }
            Document doc = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(
                            xmlString.getBytes(StandardCharsets.UTF_8)));

            NodeList notamElements = doc.getElementsByTagNameNS("*", "NOTAM");
            if (notamElements.getLength() == 0) {
                return null;
            }

            List<String> lines = new ArrayList<>();
            for (int i = 0; i < notamElements.getLength(); i++) {
                if (!(notamElements.item(i) instanceof Element)) {
                    continue;
                }
                Element notam = (Element) notamElements.item(i);

                String number = childText(notam, "number");
                String year = childText(notam, "year");
                String type = childText(notam, "type");
                String location = childText(notam, "location");
                String effectiveStart = childText(notam, "effectiveStart");
                String effectiveEnd = childText(notam, "effectiveEnd");
                String body = childText(notam, "text");

                if (body.isEmpty()) {
                    NodeList translations =
                            notam.getElementsByTagNameNS("*", "NOTAMTranslation");
                    for (int t = 0; t < translations.getLength(); t++) {
                        if (!(translations.item(t) instanceof Element)) {
                            continue;
                        }
                        String st = childText(
                                (Element) translations.item(t), "simpleText");
                        if (!st.isEmpty()) {
                            body = st;
                            break;
                        }
                    }
                }

                String classification = "";
                String accountId = "";
                Element eventEl = ancestorNamed(notam, "Event");
                if (eventEl != null) {
                    NodeList exts =
                            eventEl.getElementsByTagNameNS("*", "EventExtension");
                    for (int e = 0; e < exts.getLength(); e++) {
                        if (!(exts.item(e) instanceof Element)) {
                            continue;
                        }
                        Element ext = (Element) exts.item(e);
                        classification = childText(ext, "classification");
                        accountId = childText(ext, "accountId");
                        if (!classification.isEmpty() || !accountId.isEmpty()) {
                            break;
                        }
                    }
                }

                String line = formatNotamLine(number, year, type, location,
                        classification, accountId, effectiveStart, effectiveEnd,
                        body);
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            if (lines.isEmpty()) {
                return null;
            }
            return join(lines, "\n");
        } catch (Exception e) {
            return null;
        }
    }

    static String extractGeoJsonText(JSONObject feature) {
        if (feature == null) {
            return null;
        }
        try {
            JSONObject properties = feature.optJSONObject("properties");
            if (properties == null) {
                return null;
            }
            JSONObject core = properties.optJSONObject("coreNOTAMData");
            if (core == null) {
                return null;
            }
            JSONObject notam = core.optJSONObject("notam");
            if (notam == null) {
                return null;
            }

            String body = notam.optString("text", "").trim();
            if (body.isEmpty()) {
                JSONArray translations = core.optJSONArray("notamTranslation");
                if (translations != null) {
                    for (int t = 0; t < translations.length(); t++) {
                        JSONObject tr = translations.optJSONObject(t);
                        if (tr == null) {
                            continue;
                        }
                        String st = tr.optString("simpleText", "").trim();
                        if (!st.isEmpty()) {
                            body = st;
                            break;
                        }
                    }
                }
            }

            return formatNotamLine(
                    notam.optString("number", ""),
                    notam.optString("year", ""),
                    notam.optString("type", ""),
                    notam.optString("location", ""),
                    notam.optString("classification", ""),
                    notam.optString("accountId", ""),
                    notam.optString("effectiveStart", ""),
                    notam.optString("effectiveEnd", ""),
                    body);
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatNotamLine(String number, String year, String type,
            String location, String classification, String accountId,
            String effectiveStart, String effectiveEnd, String body) {
        if (number == null) {
            number = "";
        }
        if (year == null) {
            year = "";
        }
        if (type == null) {
            type = "";
        }
        if (location == null) {
            location = "";
        }
        if (classification == null) {
            classification = "";
        }
        if (accountId == null) {
            accountId = "";
        }
        if (body == null) {
            body = "";
        }

        List<String> headerBits = new ArrayList<>();
        if (number.contains("/")) {
            headerBits.add("NOTAM " + number);
        } else {
            String yy = year.length() >= 2
                    ? year.substring(year.length() - 2) : year;
            if (!number.isEmpty() || !yy.isEmpty()) {
                String id = !yy.isEmpty() ? yy + "/" + number : number;
                headerBits.add("NOTAM " + id);
            } else {
                headerBits.add("NOTAM");
            }
        }
        if (!location.isEmpty()) {
            headerBits.add(location);
        }
        if (!type.isEmpty()) {
            headerBits.add("[" + type + "]");
        }
        if (!classification.isEmpty()) {
            headerBits.add("(" + classification + ")");
        }
        if (!accountId.isEmpty()) {
            headerBits.add(accountId);
        }

        String start = formatNotamDate(effectiveStart);
        String end = formatNotamDate(effectiveEnd);
        String range = "";
        if (!start.isEmpty() && !end.isEmpty()) {
            range = start + "-" + end;
        } else if (!start.isEmpty()) {
            range = start;
        }

        List<String> parts = new ArrayList<>();
        parts.add(join(headerBits, " "));
        if (!range.isEmpty()) {
            parts.add(range);
        }
        if (!body.isEmpty()) {
            parts.add(body);
        }

        return join(parts, " | ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Converts a NOTAM timestamp into "YYYY-MM-DD HH:MMZ".
     * Accepts 12-digit YYYYMMDDHHMM and ISO-8601 values from NMS-API.
     */
    private static String formatNotamDate(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() == 12) {
            boolean digits = true;
            for (int i = 0; i < trimmed.length(); i++) {
                if (!Character.isDigit(trimmed.charAt(i))) {
                    digits = false;
                    break;
                }
            }
            if (digits) {
                return trimmed.substring(0, 4) + "-" + trimmed.substring(4, 6)
                        + "-" + trimmed.substring(6, 8) + " "
                        + trimmed.substring(8, 10) + ":"
                        + trimmed.substring(10, 12) + "Z";
            }
        }
        String[] patterns = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ssZ"
        };
        TimeZone utc = TimeZone.getTimeZone("UTC");
        for (String pattern : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(pattern, Locale.US);
                in.setTimeZone(utc);
                in.setLenient(false);
                Date d = in.parse(trimmed);
                if (d != null) {
                    SimpleDateFormat out =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm'Z'", Locale.US);
                    out.setTimeZone(utc);
                    return out.format(d);
                }
            } catch (Exception ignored) {
            }
        }
        return trimmed;
    }

    private static String childText(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private static Element ancestorNamed(Element start, String localName) {
        Node n = start.getParentNode();
        while (n != null) {
            if (n instanceof Element
                    && localName.equals(n.getLocalName())) {
                return (Element) n;
            }
            n = n.getParentNode();
        }
        return null;
    }

    private static String readFully(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toString("UTF-8");
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static final class UnauthorizedException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}

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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Apps4Av messages already dismissed on this device. Each entry is a
 * percent-encoded dismiss key (Dart {@code Uri.encodeComponent}). A new or
 * edited message uses a new key. The stored list keeps the newest keys and
 * drops past {@link #CAP}.
 */
public final class DismissedAppNotices {

    public static final String PREF_KEY = "key-dismissed-app-notices";
    public static final int CAP = 40;

    private DismissedAppNotices() {
    }

    public static Set<String> decode(String raw) {
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        if (raw == null || raw.length() == 0) {
            return keys;
        }
        String[] parts = raw.split(",", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() == 0) {
                continue;
            }
            keys.add(decodeComponent(parts[i]));
        }
        return keys;
    }

    public static String dismiss(String raw, String dismissKey) {
        List<String> keys = new ArrayList<String>(decode(raw));
        keys.remove(dismissKey);
        keys.add(dismissKey);
        if (keys.size() > CAP) {
            keys = new ArrayList<String>(keys.subList(keys.size() - CAP, keys.size()));
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(encodeComponent(keys.get(i)));
        }
        return out.toString();
    }

    /**
     * Percent-encode like Dart {@code Uri.encodeComponent}: UTF-8 bytes,
     * leaving RFC 2396 unreserved characters alone.
     */
    static String encodeComponent(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if (isUnreserved(b)) {
                out.append((char) b);
            } else {
                out.append('%');
                out.append(hex(b >> 4));
                out.append(hex(b & 0xF));
            }
        }
        return out.toString();
    }

    /**
     * Inverse of {@link #encodeComponent}. A malformed entry is kept as-is,
     * matching AvareX.
     */
    static String decodeComponent(String value) {
        try {
            return decodeComponentStrict(value);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private static String decodeComponentStrict(String value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length()) {
                    throw new IllegalArgumentException("truncated escape");
                }
                int hi = hexValue(value.charAt(i + 1));
                int lo = hexValue(value.charAt(i + 2));
                bytes.write((hi << 4) | lo);
                i += 2;
            } else {
                if (c > 0x7F) {
                    throw new IllegalArgumentException("non-ascii");
                }
                bytes.write(c);
            }
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean isUnreserved(int b) {
        return (b >= 'A' && b <= 'Z')
                || (b >= 'a' && b <= 'z')
                || (b >= '0' && b <= '9')
                || b == '-' || b == '_' || b == '.' || b == '!'
                || b == '~' || b == '*' || b == '\'' || b == '(' || b == ')';
    }

    private static char hex(int nibble) {
        return "0123456789ABCDEF".charAt(nibble);
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException("bad hex");
    }
}

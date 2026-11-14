/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.logbook;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.ds.avare.content.UserContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logbook rows in user.db, through the user content provider.
 */
public final class LogbookStore {

    private LogbookStore() {
    }

    public static List<LogEntry> all(Context context) {
        List<LogEntry> out = new ArrayList<LogEntry>();
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    UserContract.CONTENT_URI_LOGBOOK, null, null, null, "date DESC");
            if (c == null) {
                return out;
            }
            while (c.moveToNext()) {
                out.add(LogEntry.fromCursor(c));
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return out;
    }

    public static boolean insert(Context context, LogEntry entry) {
        try {
            Uri uri = context.getContentResolver().insert(UserContract.CONTENT_URI_LOGBOOK, entry.toValues());
            if (uri != null) {
                return true;
            }
            // A row with this id is already there (re-import). Replace it.
            return update(context, entry);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean update(Context context, LogEntry entry) {
        try {
            int rows = context.getContentResolver().update(
                    UserContract.CONTENT_URI_LOGBOOK, entry.toValues(), "id=?", new String[]{entry.id});
            return rows > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean delete(Context context, String id) {
        try {
            context.getContentResolver().delete(
                    UserContract.CONTENT_URI_LOGBOOK, "id=?", new String[]{id});
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String toCsv(List<LogEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LogEntry.COLUMNS.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(LogEntry.COLUMNS[i]));
        }
        sb.append('\n');
        for (LogEntry entry : entries) {
            Map<String, String> map = entry.toMap();
            for (int i = 0; i < LogEntry.COLUMNS.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(quote(map.get(LogEntry.COLUMNS[i])));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * @return number of rows saved. Negative when the file has no header row.
     */
    public static int importCsv(Context context, String csv) {
        List<List<String>> rows = parseCsv(csv);
        if (rows.isEmpty()) {
            return -1;
        }
        List<String> header = rows.get(0);
        ContentResolver resolver = context.getContentResolver();
        int saved = 0;
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.isEmpty() || (row.size() == 1 && row.get(0).trim().isEmpty())) {
                continue;
            }
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (int i = 0; i < header.size(); i++) {
                String key = header.get(i) == null ? "" : header.get(i).trim();
                String value = i < row.size() ? row.get(i) : "";
                map.put(key, value);
            }
            try {
                LogEntry entry = LogEntry.fromMap(map);
                if (entry.date.isEmpty()) {
                    continue;
                }
                if (resolver.insert(UserContract.CONTENT_URI_LOGBOOK, entry.toValues()) == null) {
                    resolver.update(UserContract.CONTENT_URI_LOGBOOK, entry.toValues(),
                            "id=?", new String[]{entry.id});
                }
                saved++;
            } catch (Throwable ignored) {
            }
        }
        return saved;
    }

    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        boolean special = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!special) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<List<String>>();
        if (csv == null) {
            return rows;
        }
        String text = csv.replace("\r\n", "\n").replace('\r', '\n');
        List<String> row = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<String>();
            } else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}

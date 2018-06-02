package com.ds.avare.content;

import android.database.Cursor;

public class CursorManager {

    /**
     * Close cursor safely
     * @param c
     */
    public static void close(Cursor c) {
        try {
            if (c != null) {
                if (!c.isClosed()) {
                    c.close();
                }
            }
        }
        catch (Exception e) {

        }
        c = null;
    }

}

package com.ds.avare.content;

import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class GameTfrContract {

    public static final String AUTHORITY = "com.ds.avare.provider.gametfr";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE = "gametfr";

    public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, BASE);

    public static final String TABLE = "gametfr";

    public static final String TIME = "effective";
    public static final String STADIUM = "name";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";

}

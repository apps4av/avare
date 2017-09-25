package com.ds.avare.content;

import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class ObstaclesContract {

    public static final String AUTHORITY = "com.ds.avare.provider.obstacles";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE = "obs";

    public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, BASE);

    public static final String TABLE = "obs";

    public static final String LATITUDE = "ARPLatitude";
    public static final String LONGITUDE = "ARPLongitude";
    public static final String HEIGHT = "Height";


}

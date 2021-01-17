package com.ds.avare.content;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class UserContract {

    public static final String AUTHORITY = "com.ds.avare.provider.user";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE_PLAN = "plan";
    public static final Uri CONTENT_URI_PLAN = Uri.withAppendedPath(AUTHORITY_URI, BASE_PLAN);
    public static final String TABLE_PLAN = "plans";
    public static final String PLAN_COLUMN_ID = "name";
    public static final String PLAN_COLUMN_PATH = "path";

    public static Uri buildPlansUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_PLAN, id);
    }

    public static final String BASE_LIST = "list";
    public static final Uri CONTENT_URI_LIST = Uri.withAppendedPath(AUTHORITY_URI, BASE_LIST);
    public static final String TABLE_LIST = "lists";
    public static final String LIST_COLUMN_ID = "name";
    public static final String LIST_COLUMN_TEXT = "text";

    public static Uri buildListsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_LIST, id);
    }

    public static final String BASE_WNB = "wnb";
    public static final Uri CONTENT_URI_WNB = Uri.withAppendedPath(AUTHORITY_URI, BASE_WNB);
    public static final String TABLE_WNB = "wnbs";
    public static final String WNB_COLUMN_ID = "name";
    public static final String WNB_COLUMN_TEXT = "text";

    public static Uri buildWnbsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_WNB, id);
    }

}

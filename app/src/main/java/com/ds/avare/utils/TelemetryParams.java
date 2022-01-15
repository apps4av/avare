package com.ds.avare.utils;


import android.os.Bundle;

public class TelemetryParams {
    // params
    public static final String CHART_NAME = "chart_name";
    public static final String STATUS = "status";
    public static final String FAILED = "failed";
    public static final String SUCCESS = "success";

    private Bundle mBundle;

    public TelemetryParams() {
        mBundle = new Bundle();
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public void add(String name, String value) {
        mBundle.putString(name, value);
    }
}


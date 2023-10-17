package com.ds.avare.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Telemetry {

    // events
    public static final String CHART_DOWNLOAD = "chart_download";
    public static final String IMPORT = "import";
    public static final String EXPORT = "export";

    private FirebaseAnalytics mInstance;

    public Telemetry(Context ctx) {
        mInstance = FirebaseAnalytics.getInstance(ctx);
    }

    public void sendEvent(String name, TelemetryParams params) {
        mInstance.logEvent(name, params.getBundle());
    }
}

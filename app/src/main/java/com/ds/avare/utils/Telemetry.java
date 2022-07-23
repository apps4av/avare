package com.ds.avare.utils;

import android.content.Context;
import android.os.Bundle;

// FDroid packagers note:
// Google Firebase is not allowed under FDroid FOSS policies

//FOSS-REMOVED:  import com.google.firebase.analytics.FirebaseAnalytics;

public class Telemetry {

    // events
    public static final String CHART_DOWNLOAD = "chart_download";
    public static final String IMPORT = "import";
    public static final String EXPORT = "export";

    //FOSS-REMOVED: private FirebaseAnalytics mInstance;

    public Telemetry(Context ctx) {
        //FOSS-REMOVED: mInstance = FirebaseAnalytics.getInstance(ctx);
    }

    public void sendEvent(String name, TelemetryParams params) {
        //FOSS-REMOVED: mInstance.logEvent(name, params.getBundle());
    }
}

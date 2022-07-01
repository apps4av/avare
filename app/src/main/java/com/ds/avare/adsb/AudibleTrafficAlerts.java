package com.ds.avare.adsb;

import android.content.Context;
import android.media.MediaPlayer;

import com.ds.avare.R;

public class AudibleTrafficAlerts {
    private MediaPlayer mpTrafficNear;

    public AudibleTrafficAlerts(Context ctx) {
        mpTrafficNear = MediaPlayer.create(ctx, R.raw.watch_out);
    }

    public void trafficNearAlert() {
        mpTrafficNear.start();
    }
}

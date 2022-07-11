package com.ds.avare.adsb;

import android.content.Context;
import android.location.Location;
import android.media.MediaPlayer;
import android.provider.MediaStore;

import com.ds.avare.R;

import java.util.ArrayList;
import java.util.List;

public class AudibleTrafficAlerts {
    private MediaPlayer mpTrafficNear;
    private MediaPlayer mpLow, mpHigh, mpLevel;
    private MediaPlayer mpOClock;
    private MediaPlayer[] arrMpClockHours;
    private SequentialMediaPlayer sequentialMediaPlayer;

    public AudibleTrafficAlerts(Context ctx) {
        mpTrafficNear = MediaPlayer.create(ctx, R.raw.watch_out);
        sequentialMediaPlayer = new SequentialMediaPlayer();
        arrMpClockHours = new MediaPlayer[] {
                MediaPlayer.create(ctx, R.raw.one), MediaPlayer.create(ctx, R.raw.two), MediaPlayer.create(ctx, R.raw.three),
                MediaPlayer.create(ctx, R.raw.four), MediaPlayer.create(ctx, R.raw.five), MediaPlayer.create(ctx, R.raw.six),
                MediaPlayer.create(ctx, R.raw.seven), MediaPlayer.create(ctx, R.raw.eight), MediaPlayer.create(ctx, R.raw.nine),
                MediaPlayer.create(ctx, R.raw.ten), MediaPlayer.create(ctx, R.raw.eleven), MediaPlayer.create(ctx, R.raw.twelve)
        };
        mpLow = MediaPlayer.create(ctx, R.raw.low);
        mpHigh = MediaPlayer.create(ctx, R.raw.high);
        mpLevel = MediaPlayer.create(ctx, R.raw.level);
        mpOClock = MediaPlayer.create(ctx, R.raw.oclock);
    }

    public synchronized void  alertTrafficPosition(Traffic traffic, Location myLoc, int ownAltitude) {
            final int clockHour = (int) nearestClockHourFromHeadingAndLocations(
                    myLoc.getLatitude(), myLoc.getLongitude(), traffic.mLat, traffic.mLon, myLoc.getBearing());
            final double altitudeDiff = ownAltitude - traffic.mAltitude;
                sequentialMediaPlayer.setMedia(mpTrafficNear, arrMpClockHours[clockHour - 1],
                        Math.abs(altitudeDiff) < 100 ? mpLevel
                                : (altitudeDiff > 0 ? mpLow : mpHigh));
                sequentialMediaPlayer.play();
    }

    /**
     * Helpler class that uses media event handling to ensure strictly sequential play of a list
     * of media resources
     */
    private class SequentialMediaPlayer implements MediaPlayer.OnCompletionListener {

        private MediaPlayer[] media;
        private boolean isPlaying = false;
        private int mediaIndex = 0;

        /**
         * TODO: Use synchro to wait for current play to finish if called when playing
         * @param media
         */
        public synchronized void  setMedia(MediaPlayer... media) {
            if (!isPlaying) {
                this.media = media;
                this.mediaIndex = 0;
                for (MediaPlayer mp : media)
                    mp.setOnCompletionListener(this);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (++mediaIndex <= media.length-1)
                play();
            else
                this.isPlaying = false;
        }

        public synchronized void play() {
            if (media == null || mediaIndex > media.length-1)
                throw new IllegalStateException("No more media to play; finished sequence or no media set");
            isPlaying = true;
            media[mediaIndex].start();
        }
    }



    protected static double angleFromCoordinate(double lat1, double long1, double lat2,
                                              double long2) {

        final double lat1Rad = Math.toRadians(lat1);
        final double long1Rad = Math.toRadians(long1);
        final double lat2Rad = Math.toRadians(lat2);
        final double long2Rad = Math.toRadians(long2);

        final double dLon = (long2Rad - long1Rad);

        final double y = Math.sin(dLon) * Math.cos(lat2Rad);
        final double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad)
                * Math.cos(lat2Rad) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        return  (Math.toDegrees(brng) + 360) % 360;
    }

    protected static long nearestClockHourFromHeadingAndLocations(
            double lat1, double long1, double lat2, double long2,  double myBearing) {
        final long nearestClockHour = Math.round(relativeBearingFromHeadingAndLocations(lat1, long1, lat2, long2, myBearing)/30.0);
        return nearestClockHour != 0 ? nearestClockHour : 12;
    }

    protected static double relativeBearingFromHeadingAndLocations(double lat1, double long1, double lat2, double long2,  double myBearing) {
        return (angleFromCoordinate(lat1, long1, lat2, long2) - myBearing + 360) % 360;
    }
}

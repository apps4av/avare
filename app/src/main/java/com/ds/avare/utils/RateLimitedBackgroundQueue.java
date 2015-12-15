/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.os.AsyncTask;

import com.ds.avare.StorageService;
import com.ds.avare.position.Coordinate;
import com.ds.avare.weather.Metar;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zkhan on 12/15/15.
 */
public class RateLimitedBackgroundQueue {

    private LinkedList<Object> mQueue;
    private AsyncTask<Void, Void, Void> mProcessTask;

    // Fire every few seconds
    private static final long RUN_TIME = 30 * 1000;

    public RateLimitedBackgroundQueue(final StorageService service) {

        mQueue = new LinkedList<Object>();

        TimerTask timer= new TimerTask() {
            @Override
            public void run() {
                if(mQueue.size() == 0) {
                    return;
                }
                if(mProcessTask != null && mProcessTask.getStatus() == AsyncTask.Status.RUNNING) {
                    return;
                }

                // Do something, run in background
                mProcessTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... vals) {
                        while (mQueue.size() > 0) {
                            Object o = mQueue.removeFirst();
                            // find what to do based on object type
                            if(o instanceof Metar) {
                                // Calculate metar map in background by finding lon/lat of airport

                                Metar m = (Metar)o;
                                Coordinate c = service.getDBResource().findLonLatMetar(m.stationId);
                                if(null != c) {
                                    // update lon/lat in object, catch parse exception (airport not found)
                                    m.lon = c.getLongitude();
                                    m.lat = c.getLatitude();
                                }
                            }
                        }
                        return null;
                    }
                };
                mProcessTask.execute(null, null, null);
            }
        };
        Timer t = new Timer();
        t.scheduleAtFixedRate(timer, 0, RUN_TIME);
    }

    /**
     * Something needs to be done
     * @param obj
     */
    public void insertInQueue(Object obj) {
        mQueue.add(obj);
    }
}

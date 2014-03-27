/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.weather;

import java.util.LinkedList;

import com.ds.avare.StorageService;
import com.ds.avare.shapes.MetShape;


/**
 * 
 * @author zkhan
 *
 */
public class InternetWeatherCache {

    /**
     * Task that would draw tiles on bitmap.
     */
    private WeatherTask                mWeatherTask; 
    private Thread                     mWeatherThread;
    private LinkedList<AirSigMet>      mAirSig;
    private StorageService             mService;
    
    /**
     * 
     * @param root
     */
    public void parse(StorageService service) {
        
        if(service == null) {
            return;
        }
        mService = service;
        /*
         * Do weather parsing in background. It takes a long time.
         */
        if(mWeatherThread != null) {
            if(mWeatherThread.isAlive()) {
                return;
            }
        }
        mWeatherTask =  new WeatherTask();
        mWeatherThread = new Thread(mWeatherTask);
        mWeatherThread.start();
    }
    
    /**
     * 
     * @return
     */
    public LinkedList<AirSigMet> getAirSigMet() {
        return mAirSig;
    }

    private class WeatherTask implements Runnable {

        @Override
        public void run() {
            try {
                
                /*
                 * Create a list of air/sigmets
                 */
                mAirSig = mService.getDBResource().getAirSigMets();
                
                /*
                 * Convert AIRMET/SIGMETS to shapes compatible coordinates
                 */
                for(int i = 0; i < mAirSig.size(); i++) {
                    AirSigMet asm = mAirSig.get(i);
                    /*
                     * Discard none intensity
                     */
                    if(asm.severity.equals("NONE")) {
                        continue;
                    }
                    asm.shape = new MetShape(
                            asm.timeFrom + "-" + asm.timeTo + "\n" +
                            asm.hazard + "\n" +
                            asm.reportType + "\n" +
                            asm.severity + "\n" +
                            asm.rawText);
                    String tokens[] = asm.points.split("[;]");
                    for(int j = 0; j < tokens.length; j++) {
                        String point[] = tokens[j].split("[:]");
                        double lon = Double.parseDouble(point[0]);
                        double lat = Double.parseDouble(point[1]);
                        if(0 == lat || 0 == lon) {
                            continue;
                        }
                        asm.shape.add(lon, lat, false);
                    }
                    asm.shape.makePolygon();
                }
            }
            catch(Exception e) {
            }
        }
    }
}

/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.gps;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;

/**
 * @author zkhan
 *
 */
public class Mock extends AsyncTask<Gps, Integer, Void> {

      /** Keeps track of the currently processed coordinate. */
      public double mLongitude = -72;
      public double mLatitude = 42;
      public double mAltitude = 0;
      public double mBearing = 10;
      public double mSpeed = 10;
      public Gps mGps;

      /**
       * 
       */
      @Override
      protected Void doInBackground(Gps... gps) {         

          mGps = gps[0];

          while (true) {
              // translate to actual GPS location
              publishProgress(0);
                    
              // sleep for a while before providing next location
              try {
                  Thread.sleep(5000);
              } catch (Exception e) {
                  break;
              }
          }
        return null;
      }
              
      /**
       * 
       */
      @Override
      protected void onProgressUpdate(Integer... progress) {     
          mLatitude -= 0.001;
          mLongitude -= 0.01;
          mAltitude += 1;
          mBearing++;
          mSpeed++;
          Location location = new Location(LocationManager.GPS_PROVIDER);
          location.setLatitude(mLatitude);
          location.setLongitude(mLongitude);
          location.setAltitude(mAltitude);
          location.setBearing((float)mBearing % 360);
          location.setSpeed((float)mSpeed);
          location.setTime(System.currentTimeMillis());
          mGps.onLocationChanged(location);          
      }
}


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

import android.content.Context;
import android.telephony.SmsManager;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.storage.Preferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by zkhan on 7/25/16.
 */
public class Emergency {
    public static String declare(Context ctx, final Preferences pref, final StorageService service) {
        String ret = ctx.getString(R.string.Done);

        final GpsParams params = service.getGpsParams();
        if(params == null) {
            return ctx.getString(R.string.SMSFailed);
        }

        /*
         * Send this message on SMS:
         * I Need Help!
         * My current flight on N172EF has an emergency which may force me to land off airport.
         * My GPS coordinates are 42.3939,-71.4272, current altitude is 6300 feet MSL, current time is 1830 Zulu
         * Bill
         */

        String number = pref.getEmergencyNumber();
        if(!number.equals("")) {

            String pilotName = pref.getPilotContact();
            String tailNumber = pref.getAircraftTailNumber();
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd HH:mm:ss");
            String time = formatter.format(Helper.getMillisGMT());

            String message =
                    "I Need Help! " +
                            "My current flight on aircraft " + tailNumber + " has an emergency which may force me to land off airport. " +
                            "My GPS coordinates are " + params.getLatitude() + "," +  params.getLongitude() +
                            ", current altitude (ft MSL) is " + Math.round(params.getAltitude()) +
                            ", current time is " + time + "Zulu. " +
                            pilotName;

            try {
                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(number, null, parts, null, null);
            }
            catch (Exception e) {
                ret = ctx.getString(R.string.SMSFailed);
            }

        }

        // Turn on distance guidance
        pref.showDistanceRingStatic();

        // Emergency checklist
        String checklist = pref.getEmergencyChecklist();
        service.setOverrideListName(checklist);

        // Find airport with min length then go to it
        if(service.getArea() != null) {
            Airport a = service.getArea().getAirport(0);
            if(a != null) {
                Destination d = DestinationFactory.build(service, a.getId(), Destination.BASE);
                d.find();
                service.setDestination(d);
            }
        }

        return ret;
    }
}

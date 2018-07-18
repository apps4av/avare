/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.connections;



import com.ds.avare.adsb.gdl90.BasicReportMessage;
import com.ds.avare.adsb.gdl90.Constants;
import com.ds.avare.adsb.gdl90.FisBuffer;
import com.ds.avare.adsb.gdl90.FisGraphics;
import com.ds.avare.adsb.gdl90.Id11Product;
import com.ds.avare.adsb.gdl90.Id12Product;
import com.ds.avare.adsb.gdl90.Id13Product;
import com.ds.avare.adsb.gdl90.Id413Product;
import com.ds.avare.adsb.gdl90.Id6364Product;
import com.ds.avare.adsb.gdl90.Id8Product;
import com.ds.avare.adsb.gdl90.LongReportMessage;
import com.ds.avare.adsb.gdl90.OwnshipGeometricAltitudeMessage;
import com.ds.avare.adsb.gdl90.OwnshipMessage;
import com.ds.avare.adsb.gdl90.Product;
import com.ds.avare.adsb.gdl90.TrafficReportMessage;
import com.ds.avare.adsb.gdl90.UplinkMessage;
import com.ds.avare.nmea.Ownship;
import com.ds.avare.nmea.RTMMessage;
import com.ds.avare.storage.Preferences;
import com.ds.avare.weather.MetarFlightCategory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

/**
 * 
 * @author zkhan
 *
 */
public class BufferProcessor {

    com.ds.avare.adsb.gdl90.DataBuffer dbuffer =
            new com.ds.avare.adsb.gdl90.DataBuffer(16384);
    com.ds.avare.nmea.DataBuffer nbuffer =
            new com.ds.avare.nmea.DataBuffer(16384);
    com.ds.avare.adsb.gdl90.Decode decode =
            new com.ds.avare.adsb.gdl90.Decode();
    com.ds.avare.nmea.Decode ndecode =
            new com.ds.avare.nmea.Decode();
    com.ds.avare.nmea.Ownship nmeaOwnship =
            new com.ds.avare.nmea.Ownship();

    /**
     * 
     * @param buffer
     * @param red
     */
    public void put(byte [] buffer, int red) {
        nbuffer.put(buffer, red);                     
        dbuffer.put(buffer, red);
    }

    /**
     * 
     * @return
     */
    public LinkedList<String> decode(Preferences pref) {

        LinkedList<String> objs = new LinkedList<String>();
        
        byte[] buf;
        
        while(null != (buf = nbuffer.get())) {
            com.ds.avare.nmea.Message m = ndecode.decode(buf);
            
            if(m instanceof RTMMessage) {
                
                /*
                 * Make a GPS locaiton message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                RTMMessage tm = (RTMMessage)m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", (double)tm.mLon);
                    object.put("latitude", (double)tm.mLat);
                    object.put("speed", (double)(tm.mSpeed));
                    object.put("bearing", (double)tm.mDirection);
                    object.put("altitude", (double)((double)tm.mAltitude));
                    object.put("callsign", (String)"");
                    object.put("address", (int)tm.mIcaoAddress);
                    object.put("time", (long)tm.getTime());
                } catch (JSONException e1) {
                    continue;
                }                
                
                objs.add(object.toString());

            }

            else if(nmeaOwnship.addMessage(m)) {
                    
                /*
                 * Make a GPS locaiton message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                Ownship om = nmeaOwnship;
                try {
                    object.put("type", "ownship");
                    object.put("longitude", (double)om.mLon);
                    object.put("latitude", (double)om.mLat);
                    object.put("speed", (double)(om.mHorizontalVelocity));
                    object.put("bearing", (double)om.mDirection);
                    object.put("altitude", (double)((double)om.mAltitude));
                    object.put("time", (long)om.getTime());
                } catch (JSONException e1) {
                    continue;
                }

                objs.add(object.toString());
            }
        }

        while(null != (buf = dbuffer.get())) {

            /*
             * Get packets, decode
             */
            com.ds.avare.adsb.gdl90.Message m = decode.decode(buf);
            /*
             * Post on UI thread.
             */
            
            if(m instanceof TrafficReportMessage) {
                
                /*
                 * Make a GPS locaiton message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                TrafficReportMessage tm = (TrafficReportMessage)m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", (double)tm.mLon);
                    object.put("latitude", (double)tm.mLat);
                    object.put("speed", (double)(tm.mHorizVelocity));
                    object.put("bearing", (double)tm.mHeading);
                    object.put("altitude", (double)((double)tm.mAltitude));
                    object.put("callsign", (String)tm.mCallSign);
                    object.put("address", (int)tm.mIcaoAddress);
                    object.put("time", (long)tm.getTime());
                } catch (JSONException e1) {
                    continue;
                }                
                
                objs.add(object.toString());

            }

            else if(m instanceof BasicReportMessage) {
                
                /*
                 * Make a GPS locaiton message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                BasicReportMessage tm = (BasicReportMessage)m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", (double)tm.mLon);
                    object.put("latitude", (double)tm.mLat);
                    object.put("speed", (double)(tm.mSpeed));
                    object.put("bearing", (double)tm.mHeading);
                    object.put("altitude", (double)((double)tm.mAltitude));
                    object.put("callsign", (String)tm.mCallSign);
                    object.put("address", (int)tm.mIcaoAddress);
                    object.put("time", (long)tm.getTime());
                } catch (JSONException e1) {
                    continue;
                }
                
                objs.add(object.toString());
            }

            else if(m instanceof LongReportMessage) {
                
                /*
                 * Make a GPS locaiton message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                LongReportMessage tm = (LongReportMessage)m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", (double)tm.mLon);
                    object.put("latitude", (double)tm.mLat);
                    object.put("speed", (double)(tm.mSpeed));
                    object.put("bearing", (double)tm.mHeading);
                    object.put("altitude", (double)((double)tm.mAltitude));
                    object.put("callsign", (String)tm.mCallSign);
                    object.put("address", (int)tm.mIcaoAddress);
                    object.put("time", (long)tm.getTime());
                } catch (JSONException e1) {
                    continue;
                }
                
                objs.add(object.toString());
            }

            else if(m instanceof OwnshipGeometricAltitudeMessage) {
                JSONObject object = new JSONObject();
                try {
                    object.put("type", "geoaltitude");

                    int altitude = (int)((OwnshipGeometricAltitudeMessage)m).mAltitudeWGS84;
                    if(altitude == Integer.MIN_VALUE) {
                        // invalid
                        continue;
                    }
                    object.put("altitude", (double) altitude);
                    object.put("time", (long) m.getTime());
                } catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }

            else if(m instanceof UplinkMessage) {
                /*
                 * Send an uplink nexrad message
                 */
                FisBuffer fis = ((UplinkMessage) m).getFis();
                if(null == fis) {
                    continue;
                }
                LinkedList<Product> pds = fis.getProducts();
                for(Product p : pds) {
                    if(p instanceof Id8Product) {
                        Id8Product pn = (Id8Product)p;
                        JSONObject object = addFisGraphics("notam", pn.getFis());
                        if(null != object) {
                            objs.add(object.toString());
                        }
                    }
                    if(p instanceof Id11Product) {
                        Id11Product pn = (Id11Product)p;
                        JSONObject object = addFisGraphics("airmet", pn.getFis());
                        if(null != object) {
                            objs.add(object.toString());
                        }
                    }
                    if(p instanceof Id12Product) {
                        Id12Product pn = (Id12Product)p;
                        JSONObject object = addFisGraphics("sigmet", pn.getFis());
                        if(null != object) {
                            objs.add(object.toString());
                        }
                    }
                    if(p instanceof Id13Product) {
                        Id13Product pn = (Id13Product)p;
                        JSONObject object = addFisGraphics("sua", pn.getFis());
                        if(null != object) {
                            objs.add(object.toString());
                        }
                    }
                    else if(p instanceof Id6364Product) {
                        Id6364Product pn = (Id6364Product)p;
                        JSONObject object = new JSONObject();
                        
                        JSONArray arrayEmpty = new JSONArray();
                        JSONArray arrayData = new JSONArray();
                        
                        int[] data = pn.getData();
                        if(null != data) {
                            for(int i = 0; i < data.length; i++) {
                                arrayData.put(data[i]);
                            }
                        }
                        LinkedList<Integer> empty = pn.getEmpty();
                        if(null != empty) {
                            for(int e : empty) {
                                arrayEmpty.put(e);
                            }
                        }
                    
                        try {
                            object.put("type", "nexrad");
                            object.put("time", (long)pn.getTime().getTimeInMillis());
                            object.put("conus", pn.isConus());
                            object.put("blocknumber", (long)pn.getBlockNumber());
                            object.put("x", Constants.COLS_PER_BIN);
                            object.put("y", Constants.ROWS_PER_BIN);
                            object.put("empty", arrayEmpty);
                            object.put("data", arrayData);
                        } catch (JSONException e1) {
                            continue;
                        }
                        
                        objs.add(object.toString());
                    }
                    /*
                     * Text product
                     */

                    else if(p instanceof Id413Product) {
                        Id413Product pn = (Id413Product)p;
                        JSONObject object = new JSONObject();
                        
                        String data = pn.getData();
                        String type = pn.getHeader();
                        long time = (long)pn.getTime().getTimeInMillis();
                        
                        /*
                         * Clear garbage spaces etc. Convert to Avare format
                         */

                        try {
                            if(type.equals("METAR") || type.equals("SPECI")) {
                                object.put("flight_category", MetarFlightCategory.getFlightCategory(pn.getLocation(), pn.getData()));
                            }
                            if(type.equals("WINDS")) {
                                
                                String tokens[] = data.split("\n");
                                if(tokens.length < 2) {
                                    /*
                                     * Must have line like
                                     * MSY 230000Z  FT 3000 6000    F9000   C12000  G18000  C24000  C30000  D34000  39000   Y 
                                     * and second line like
                                     * 1410 2508+10 2521+07 2620+01 3037-12 3041-26 304843 295251 29765
                                     */
                                    continue;
                                }
                                
                                tokens[0] = tokens[0].replaceAll("\\s+", " ");
                                tokens[1] = tokens[1].replaceAll("\\s+", " ");
                                String winds[] = tokens[1].split(" ");
                                String alts[] = tokens[0].split(" ");
                                                                        
                                /*
                                 * Start from 3rd entry - alts
                                 */
                                data = "";
                                boolean found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("3000") && !alts[i].contains("30000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("6000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("9000") && !alts[i].contains("39000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("12000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("18000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("24000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("30000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("34000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                                found = false;
                                for(int i = 2; i < alts.length; i++) {
                                    if(alts[i].contains("39000")) {
                                        data += winds[i - 2] + ",";
                                        found = true;
                                    }
                                }
                                if(!found) {
                                    data += ",";
                                }
                            }
                        }
                        catch (Exception e) {
                            continue;
                        }
                        
                        try {
                            object.put("type", pn.getHeader());
                            object.put("time", time);
                            object.put("location", pn.getLocation());
                            object.put("data", data);
                        } catch (JSONException e1) {
                            continue;
                        }
                        
                        objs.add(object.toString());
                    }
                }
            }
            else if(m instanceof OwnshipMessage) {
                
                /*
                 * Make a GPS locaiton message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                OwnshipMessage om = (OwnshipMessage)m;
                try {
                    object.put("type", "ownship");
                    object.put("longitude", (double)om.mLon);
                    object.put("latitude", (double)om.mLat);
                    object.put("speed", (double)(om.mHorizontalVelocity));
                    object.put("bearing", (double)om.mDirection);
                    object.put("time", (long)om.getTime());
                    object.put("altitude", (double) om.mAltitude);
                } catch (JSONException e1) {
                    continue;
                }
                
                objs.add(object.toString());
            }
        }

        return objs;
    }

    /**
     * Add Fis-B graphics products except Nexrad
     * @param type
     * @param fisg
     * @return
     */
    private JSONObject addFisGraphics(String type, FisGraphics fisg) {
        if(null == fisg) {
            return null;
        }
        JSONObject object = new JSONObject();
        try {
            object.put("type", type);
            object.put("text", null == fisg.getText() ? "" : fisg.getText());
            object.put("location", fisg.getLocation());
            object.put("label", fisg.getLabel());
            object.put("startTime", fisg.getStartTime());
            object.put("endTime", fisg.getEndTime());
            object.put("shape", fisg.getShapeString());
            object.put("number", fisg.getReportNumber());

            if(null != fisg.getShapeString()) {
                LinkedList<FisGraphics.Coordinate> coords = fisg.getCoordinates();
                String data = "";
                for (FisGraphics.Coordinate c : coords) {
                    String lon = Double.toString(c.lon);
                    String lat = Double.toString(c.lat);
                    data += lon + ":" + lat + ";";
                }
                if(data.length() > 1) {
                    // remove last ;
                    object.put("data", data.substring(0, data.length() - 1));
                }
                else {
                    object.put("data", "");
                }
            }
            else {
                object.put("data", "");
            }

        }
        catch (JSONException el) {
            return null;
        }
        return object;
    }
}

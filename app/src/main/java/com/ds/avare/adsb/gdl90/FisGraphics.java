/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * Code rewrite with help from:
 * Copyright (c) 2015-2016 Christopher Young ("Copyright Holder").
 * All rights reserved.
 */

package com.ds.avare.adsb.gdl90;


import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by zkhan on 7/10/17.
 */

public class FisGraphics {

    public static final int SHAPE_NONE = -1;
    public static final int SHAPE_POLYGON_MSL = 3;
    public static final int SHAPE_PRISM_MSL = 7;
    public static final int SHAPE_PRISM_AGL = 8;
    public static final int SHAPE_POINT3D_AGL = 9;
    String mLocation;
    String mText;
    String mLabel;
    String mStartTime;
    String mEndTime;
    int mReportNumber;
    LinkedList<Coordinate> mCoordinates;
    int mGeometryOverlayOptions;

    private String parseDate(byte b0, byte b1, byte b2, byte b3, int format) {
        switch (format) {
            case 0: // No date/time used.
                return "";
            case 1: // Month, Day, Hours, Minutes.

                return String.format("%02d-%02dT%02d:%02d:00Z", (int) b0, (int) b1, (int) b2, (int) b3);
            case 2: // Day, Hours, Minutes.
                return String.format("%02dT%02d:%02d:00Z", (int) b0, (int) b1, (int) b2);
            case 3: // Hours, Minutes.
                return String.format("%02d:%02d:00Z", (int) b0, (int) b1);
        }

        return "";
    }

    private Coordinate parseLatLon(int lat, int lon, boolean alt) {
        double factor = 0.000687;
        Coordinate coord = new Coordinate();

        if (alt) {
            factor = 0.001373;
        }
        coord.lat = factor * (double) lat;
        coord.lon = factor * (double) lon;
        if (coord.lat > 90) {
            coord.lat = coord.lat - 180;
        }
        if (coord.lon > 180) {
            coord.lon = coord.lon - 360;
        }


        return coord;
    }

    public boolean decode(byte[] data) {

        int format;
        int count;
        int length;

        mText = "";
        mStartTime = "";
        mEndTime = "";
        mCoordinates = new LinkedList<Coordinate>();
        mGeometryOverlayOptions = SHAPE_NONE;
        format = (((int) data[0]) & 0xF0) >> 4;
        count = (((int) data[1]) & 0xF0) >> 4;
        // Only support 1 record
        if (count != 1) {
            return false;
        }

        mLocation = Dlac.decode(data[2], data[3], data[4]);
        mLocation = Dlac.format(mLocation);

        /*
            0 - No data
            1 - Unformatted ASCII Text
            2 - Unformatted DLAC Text
            3 - Unformatted DLAC Text w/ dictionary
            4 - Formatted Text using ASN.1/PER
            5-7 - Future Use
            8 - Graphical Overlay
            9-15 - Future Use
        */
        switch (format) {
            case 0:
                return false;
            case 2:
                length = (((int) data[6] & 0xFF) << 8) + ((int) data[7] & 0xFF);
                if (data.length - length < 6) {
                    return false;
                }

                mReportNumber = (((int) data[8] & 0xFF) << 6) + (((int) data[9] & 0xFC) >> 2);

                int len = length - 5;

                mText = "";
                for (int i = 0; i < (len - 3); i += 3) {
                    mText += Dlac.decode(data[i + 11], data[i + 12], data[i + 13]);
                }
                mText = Dlac.format(mText);
                break;

            case 8:

                byte recordData[] = Arrays.copyOfRange(data, 6, data.length);

                mReportNumber = (((int) recordData[1] & 0x3F) << 8) + ((int) recordData[2] & 0xFF);

                // (6-1). (6.22 - Graphical Overlay Record Format).
                int flag = recordData[4] & 0x01;

                if (0 == flag) { // Numeric index.
                    mLabel = Integer.toString((((int) recordData[5] & 0xFF) << 8) + ((int) recordData[6] & 0xFF));
                    byte recordDataA[] = Arrays.copyOfRange(recordData, 7, recordData.length);
                    recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                } else {
                    mLabel = Dlac.decode(recordData[5], recordData[6], recordData[7]) +
                            Dlac.decode(recordData[8], recordData[9], recordData[10]) +
                            Dlac.decode(recordData[11], recordData[12], recordData[13]);
                    mLabel = Dlac.format(mLabel);
                    byte recordDataA[] = Arrays.copyOfRange(recordData, 14, recordData.length);
                    recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                }

                flag = ((int) recordData[0] & 0x40) >> 6;

                if (0 == flag) { //TODO: Check.
                    byte recordDataA[] = Arrays.copyOfRange(recordData, 2, recordData.length);
                    recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                } else {
                    byte recordDataA[] = Arrays.copyOfRange(recordData, 5, recordData.length);
                    recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                }

                int applicabilityOptions = ((int) recordData[0] & 0xC0) >> 6;
                int dtFormat = ((int) recordData[0] & 0x30) >> 4;
                mGeometryOverlayOptions = (int) recordData[0] & 0x0F;
                int overlayVerticesCount = ((int) recordData[1] & 0x3F) + 1; // Document instructs to add 1. (6.20).

                // Parse all of the dates.
                switch (applicabilityOptions) {
                    case 0: // No times given. UFN.
                        byte recordDataA[] = Arrays.copyOfRange(recordData, 2, recordData.length);
                        recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                        break;
                    case 1: // Start time only. WEF.
                        mStartTime = parseDate(recordData[2], recordData[3], recordData[4], recordData[5], dtFormat);
                        mEndTime = "";
                        recordDataA = Arrays.copyOfRange(recordData, 6, recordData.length);
                        recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                        break;
                    case 2: // End time only. TIL.
                        mEndTime = parseDate(recordData[2], recordData[3], recordData[4], recordData[5], dtFormat);
                        mStartTime = "";
                        recordDataA = Arrays.copyOfRange(recordData, 6, recordData.length);
                        recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                        break;
                    case 3: // Both start and end times. WEF.
                        mStartTime = parseDate(recordData[2], recordData[3], recordData[4], recordData[5], dtFormat);
                        mEndTime = parseDate(recordData[6], recordData[7], recordData[8], recordData[9], dtFormat);
                        recordDataA = Arrays.copyOfRange(recordData, 10, recordData.length);
                        recordData = Arrays.copyOfRange(recordDataA, 0, recordDataA.length);
                        break;

                }

                // Now we have the vertices.
                switch (mGeometryOverlayOptions) {
                    case SHAPE_POLYGON_MSL: // Extended Range 3D Polygon (MSL).
                        for (int i = 0; i < overlayVerticesCount; i++) {
                            int lon = (((int) recordData[6 * i] & 0xFF) << 11) +
                                    (((int) recordData[6 * i + 1] & 0xFF) << 3) +
                                    (((int) recordData[6 * i + 2] & 0xE0) >> 5);

                            int lat = (((int) recordData[6 * i + 2] & 0x1F) << 14) +
                                    (((int) recordData[6 * i + 3] & 0xFF) << 6) +
                                    (((int) recordData[6 * i + 4] & 0xFC) >> 2);

                            int alt = (((int) recordData[6 * i + 4] & 0x03) << 8) +
                                    ((int) recordData[6 * i + 5] & 0xFF);


                            Coordinate c = parseLatLon(lat, lon, false);
                            c.altitude = alt * 100;
                            mCoordinates.add(c);
                        }
                        break;

                    case SHAPE_POINT3D_AGL: // Extended Range 3D Point (AGL). p.47.
                        if (recordData.length >= 6) {
                            int lon = (((int) recordData[0] & 0xFF) << 11) +
                                    (((int) recordData[1] & 0xFF) << 3) +
                                    (((int) recordData[2] & 0xE0) >> 5);

                            int lat = (((int) recordData[2] & 0x1F) << 14) +
                                    (((int) recordData[3] & 0xFF) << 6) +
                                    (((int) recordData[4] & 0xFC) >> 2);

                            int alt = (((int) recordData[4] & 0x03) << 8) +
                                    ((int) recordData[5] & 0xFF);

                            Coordinate c = parseLatLon(lat, lon, false);
                            c.altitude = alt * 100;
                            mCoordinates.add(c);
                        } else {
                            return false;
                        }
                        break;
                    case SHAPE_PRISM_AGL:
                    case SHAPE_PRISM_MSL:// Extended Range Circular Prism (7 = MSL, 8 = AGL)
                        if (recordData.length >= 14) {

                            int bottomLon = (((int) recordData[0] & 0xFF) << 10) + (((int) recordData[1] & 0xFF) << 2) + (((int) recordData[2] & 0xC0) >> 6);
                            int bottomLat = (((int) recordData[2] & 0x3F) << 10) + (((int) recordData[3] & 0xFF) << 4) + (((int) recordData[4] & 0xF0) >> 4);

                            int topLon = (((int) recordData[4] & 0x0F) << 14) + (((int) recordData[5] & 0xFF) << 6) + (((int) recordData[6] & 0xFC) >> 2);
                            int topLat = (((int) recordData[6] & 0x03) << 16) + (((int) recordData[7] & 0xFF) << 8) + (((int) recordData[8] & 0xFF));

                            int bottomAlt = ((int) recordData[9] & 0xFE) >> 1;
                            int topAlt = (((int) recordData[9] & 0x01) << 6) + ((int) recordData[10] & 0xFC) >> 2;

                            double rLon = ((double) (((int) recordData[10] & 0x03) << 7) + (((int) recordData[11] & 0xFE) >> 1)) * 0.2;
                            double rLat = ((double) (((int) recordData[11] & 0x01) << 8) + ((int) recordData[12] & 0xFF)) * 0.2;

                            int alpha = (int) recordData[13] & 0xFF;

                            Coordinate b = parseLatLon(bottomLat, bottomLon, true);
                            b.altitude = bottomAlt * 5;


                            Coordinate t = parseLatLon(topLat, topLon, true);
                            t.altitude = topAlt * 500;

                            mCoordinates.add(t);
                            mCoordinates.add(b);

                            // This is not a coordinate
                            Coordinate r = new Coordinate();
                            r.lon = rLon;
                            r.lat = rLat;
                            r.altitude = alpha;

                            mCoordinates.add(r);
                        } else {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
                break;
            default:
                return false;
        }

        return true;
    }

    public String getLocation() {
        return mLocation;
    }

    public String getText() {
        return mText;
    }

    public String getLabel() {
        return mLabel;
    }

    public String getStartTime() {
        return mStartTime;
    }

    public String getEndTime() {
        return mEndTime;
    }

    public LinkedList<Coordinate> getCoordinates() {
        return mCoordinates;
    }

    public int getReportNumber() {
        return mReportNumber;
    }

    public String getShapeString() {
        switch (mGeometryOverlayOptions) {

            case SHAPE_POINT3D_AGL:
                return "point";
            case SHAPE_POLYGON_MSL:
                return "polygon";
            case SHAPE_PRISM_MSL:
                return "prism.msl";
            case SHAPE_PRISM_AGL:
                return "prism";
            default:
                return "";
        }
    }

    public class Coordinate {
        public double lon;
        public double lat;
        public double altitude;
    }
}

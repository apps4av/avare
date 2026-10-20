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


import java.util.LinkedList;
import java.util.Locale;

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
    String mAltitudeBottom = "0";
    String mAltitudeTop = "0";

    /** Nautical miles. Ring size must not follow the display-unit earth radius. */
    private static final double EARTH_RADIUS_NM = 3440.069;

    private static String parseDate(byte b0, byte b1, byte b2, byte b3, int format) {
        int u0 = b0 & 0xFF;
        int u1 = b1 & 0xFF;
        int u2 = b2 & 0xFF;
        int u3 = b3 & 0xFF;
        switch (format) {
            case 0: // No date/time used.
                return "";
            case 1: // Month, Day, Hours, Minutes.
                return String.format(Locale.US, "%02d-%02dT%02d:%02d:00Z", u0, u1, u2, u3);
            case 2: // Day, Hours, Minutes.
                return String.format(Locale.US, "%02dT%02d:%02d:00Z", u0, u1, u2);
            case 3: // Hours, Minutes.
                return String.format(Locale.US, "%02d:%02d:00Z", u0, u1);
        }

        return "";
    }

    private Coordinate parseLatLon(int lat, int lon, boolean alt) {
        // 19-bit polygon / 18-bit circular-prism scaling (360 / 2^n), same as AvareX.
        double factor = 0.00068665;
        Coordinate coord = new Coordinate();

        if (alt) {
            factor = 0.00137329;
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

        if (data == null || data.length < 6) {
            return false;
        }

        mText = "";
        mStartTime = "";
        mEndTime = "";
        mAltitudeBottom = "0";
        mAltitudeTop = "0";
        mCoordinates = new LinkedList<Coordinate>();
        mGeometryOverlayOptions = SHAPE_NONE;
        format = (data[0] & 0xF0) >> 4;
        count = (data[1] & 0xF0) >> 4;
        if (count < 1) {
            return false;
        }

        mLocation = Dlac.decode(data[2], data[3], data[4]);
        mLocation = Dlac.format(mLocation);

        int[] cursor = new int[] { 6 };
        for (int record = 0; record < count; record++) {
            boolean ok;
            if (format == 2) {
                ok = decodeTextRecord(data, cursor);
            } else if (format == 8) {
                ok = decodeGraphicRecord(data, cursor);
            } else {
                return false;
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Unformatted DLAC text record. {@code cursor} is an offset into {@code data}.
     */
    private boolean decodeTextRecord(byte[] data, int[] cursor) {
        int base = cursor[0];
        if (base + 2 > data.length) {
            return false;
        }
        int length = ((data[base] & 0xFF) << 8) + (data[base + 1] & 0xFF);
        if (length < 5 || base + length > data.length) {
            return false;
        }

        mReportNumber = ((data[base + 2] & 0xFF) << 6) + ((data[base + 3] & 0xFC) >> 2);
        int len = length - 5;
        mText = "";
        for (int i = 0; i < (len - 3); i += 3) {
            int o = base + 5 + i;
            if (o + 2 >= data.length) {
                break;
            }
            mText += Dlac.decode(data[o], data[o + 1], data[o + 2]);
        }
        mText = Dlac.format(mText);
        cursor[0] = base + length;
        return true;
    }

    /**
     * Graphical overlay record. Polygon vertices are stored as sent. Circular
     * prisms and extended points become a closed ring, the same outlines AvareX draws.
     */
    private boolean decodeGraphicRecord(byte[] data, int[] cursor) {
        int p = cursor[0];
        if (p + 5 > data.length) {
            return false;
        }

        mReportNumber = ((data[p + 1] & 0x3F) << 8) + (data[p + 2] & 0xFF);
        int flag = data[p + 4] & 0x01;
        if (flag == 0) {
            if (p + 7 > data.length) {
                return false;
            }
            mLabel = Integer.toString(((data[p + 5] & 0xFF) << 8) + (data[p + 6] & 0xFF));
            p += 7;
        } else {
            if (p + 14 > data.length) {
                return false;
            }
            mLabel = Dlac.decode(data[p + 5], data[p + 6], data[p + 7]) +
                    Dlac.decode(data[p + 8], data[p + 9], data[p + 10]) +
                    Dlac.decode(data[p + 11], data[p + 12], data[p + 13]);
            mLabel = Dlac.format(mLabel);
            p += 14;
        }

        if (p >= data.length) {
            return false;
        }
        flag = (data[p] & 0x40) >> 6;
        if (flag == 0) {
            if (p + 2 > data.length) {
                return false;
            }
            p += 2;
        } else {
            if (p + 5 > data.length) {
                return false;
            }
            p += 5;
        }

        if (p + 2 > data.length) {
            return false;
        }
        int applicabilityOptions = (data[p] & 0xC0) >> 6;
        int dtFormat = (data[p] & 0x30) >> 4;
        mGeometryOverlayOptions = data[p] & 0x0F;
        int overlayVerticesCount = (data[p + 1] & 0x3F) + 1;

        switch (applicabilityOptions) {
            case 0: // No times given. UFN.
                p += 2;
                break;
            case 1: // Start time only. WEF.
                if (p + 6 > data.length) {
                    return false;
                }
                mStartTime = parseDate(data[p + 2], data[p + 3], data[p + 4], data[p + 5], dtFormat);
                mEndTime = "";
                p += 6;
                break;
            case 2: // End time only. TIL.
                if (p + 6 > data.length) {
                    return false;
                }
                mEndTime = parseDate(data[p + 2], data[p + 3], data[p + 4], data[p + 5], dtFormat);
                mStartTime = "";
                p += 6;
                break;
            case 3: // Both start and end times.
                if (p + 10 > data.length) {
                    return false;
                }
                mStartTime = parseDate(data[p + 2], data[p + 3], data[p + 4], data[p + 5], dtFormat);
                mEndTime = parseDate(data[p + 6], data[p + 7], data[p + 8], data[p + 9], dtFormat);
                p += 10;
                break;
            default:
                return false;
        }

        switch (mGeometryOverlayOptions) {
            case SHAPE_POLYGON_MSL:
                if (p + overlayVerticesCount * 6 > data.length) {
                    return false;
                }
                for (int i = 0; i < overlayVerticesCount; i++) {
                    int o = p + 6 * i;
                    int lon = ((data[o] & 0xFF) << 11) +
                            ((data[o + 1] & 0xFF) << 3) +
                            ((data[o + 2] & 0xE0) >> 5);
                    int lat = ((data[o + 2] & 0x1F) << 14) +
                            ((data[o + 3] & 0xFF) << 6) +
                            ((data[o + 4] & 0xFC) >> 2);
                    int alt = ((data[o + 4] & 0x03) << 8) + (data[o + 5] & 0xFF);
                    Coordinate c = parseLatLon(lat, lon, false);
                    c.altitude = alt * 100;
                    mAltitudeTop = Integer.toString(alt * 100);
                    mCoordinates.add(c);
                }
                p += overlayVerticesCount * 6;
                break;

            case SHAPE_POINT3D_AGL:
                if (p + 6 > data.length) {
                    return false;
                }
                {
                    int lon = ((data[p] & 0xFF) << 11) +
                            ((data[p + 1] & 0xFF) << 3) +
                            ((data[p + 2] & 0xE0) >> 5);
                    int lat = ((data[p + 2] & 0x1F) << 14) +
                            ((data[p + 3] & 0xFF) << 6) +
                            ((data[p + 4] & 0xFC) >> 2);
                    int alt = ((data[p + 4] & 0x03) << 8) + (data[p + 5] & 0xFF);
                    Coordinate center = parseLatLon(lat, lon, false);
                    mAltitudeTop = Integer.toString(alt * 100);
                    addRing(center.lon, center.lat, 0.25, alt * 100);
                }
                p += 6;
                break;

            case SHAPE_PRISM_AGL:
            case SHAPE_PRISM_MSL:
                if (p + 14 > data.length) {
                    return false;
                }
                {
                    // Latitude is 18 bits: 6 + 8 + 4. A 10-bit shift overlaps the next byte.
                    int bottomLon = ((data[p] & 0xFF) << 10) +
                            ((data[p + 1] & 0xFF) << 2) +
                            ((data[p + 2] & 0xC0) >> 6);
                    int bottomLat = ((data[p + 2] & 0x3F) << 12) +
                            ((data[p + 3] & 0xFF) << 4) +
                            ((data[p + 4] & 0xF0) >> 4);
                    int bottomAlt = (data[p + 9] & 0xFE) >> 1;
                    int topAlt = ((data[p + 9] & 0x01) << 6) + ((data[p + 10] & 0xFC) >> 2);
                    mAltitudeBottom = Integer.toString(bottomAlt * 5);
                    mAltitudeTop = Integer.toString(topAlt * 500);
                    int rLonRaw = ((data[p + 10] & 0x03) << 7) + ((data[p + 11] & 0xFE) >> 1);
                    int rLatRaw = ((data[p + 11] & 0x01) << 8) + (data[p + 12] & 0xFF);
                    double radiusNm = (rLonRaw + rLatRaw) / 2.0 * 0.2;
                    Coordinate center = parseLatLon(bottomLat, bottomLon, true);
                    addRing(center.lon, center.lat, radiusNm, topAlt * 500.0);
                }
                p += 14;
                break;

            default:
                return false;
        }

        cursor[0] = p;
        return true;
    }

    /**
     * Closed ring of 36 points, plus the first point repeated so the outline joins.
     * Radius is in nautical miles. Anything under 0.25 NM is drawn at 0.25 NM.
     */
    private void addRing(double lon, double lat, double radiusNm, double altitude) {
        if (radiusNm < 0.25) {
            radiusNm = 0.25;
        }
        Coordinate first = null;
        for (int i = 0; i < 36; i++) {
            Coordinate c = offsetNm(lon, lat, radiusNm, i * 10.0);
            c.altitude = altitude;
            if (i == 0) {
                first = c;
            }
            mCoordinates.add(c);
        }
        if (first != null) {
            Coordinate close = new Coordinate();
            close.lon = first.lon;
            close.lat = first.lat;
            close.altitude = first.altitude;
            mCoordinates.add(close);
        }
    }

    private Coordinate offsetNm(double lon, double lat, double distanceNm, double bearingDeg) {
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double brg = Math.toRadians(bearingDeg);
        double ang = distanceNm / EARTH_RADIUS_NM;
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(ang)
                + Math.cos(lat1) * Math.sin(ang) * Math.cos(brg));
        double lon2 = lon1 + Math.atan2(Math.sin(brg) * Math.sin(ang) * Math.cos(lat1),
                Math.cos(ang) - Math.sin(lat1) * Math.sin(lat2));
        Coordinate c = new Coordinate();
        c.lon = Math.toDegrees(lon2);
        c.lat = Math.toDegrees(lat2);
        if (c.lon > 180) {
            c.lon -= 360;
        }
        if (c.lon < -180) {
            c.lon += 360;
        }
        return c;
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

    public String getAltitudeBottom() {
        return mAltitudeBottom;
    }

    public String getAltitudeTop() {
        return mAltitudeTop;
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

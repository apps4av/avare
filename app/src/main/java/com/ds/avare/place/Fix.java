package com.ds.avare.place;

/**
 * Created by roleary on 1/3/2017.
 */

public class Fix {
    private String id;
    private double lat, lon;
    private String type;

    public Fix(String id, double lat, double lon, String type) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getType() {
        return type;
    }


}

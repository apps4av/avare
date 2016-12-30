package com.ds.avare.views;

/**
 * Created by pasniak on 12/4/2016.
 */
public class LongPressedDestination {
    private String name, type;

    public LongPressedDestination(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String toJSON() {
        String json = "{";
        json += "\"name\":\"" + this.name + "\",";
        json += "\"type\":\"" + this.type + "\"";
        json += "}";
        return json;
    }
}

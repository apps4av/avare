package com.ds.avare.place;

import com.ds.avare.StorageService;

/**
 * Created by zkhan on 2/9/17.
 */


public class DestinationFactory {

    public static Destination build(String name, String type) {

        if(type.equals(Destination.GPS)) {
            return new GpsDestination(name);
        }
        else if(type.equals(Destination.MAPS)) {
            return new MapsDestination(name);
        }
        else if(type.equals(Destination.UDW)) {
            return new UDWDestination(name);
        }
        else {
            return new DatabaseDestination(name, type);
        }
    }
}

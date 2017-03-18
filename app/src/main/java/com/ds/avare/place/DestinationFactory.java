package com.ds.avare.place;

import com.ds.avare.StorageService;

/**
 * Created by zkhan on 2/9/17.
 */


public class DestinationFactory {

    public static Destination build(StorageService service, String name, String type) {

        if(type.equals(Destination.GPS)) {
            return new GpsDestination(service, name);
        }
        else if(type.equals(Destination.MAPS)) {
            return new MapsDestination(service, name);
        }
        else if(type.equals(Destination.UDW)) {
            return new UDWDestination(service, name);
        }
        else {
            return new DatabaseDestination(service, name, type);
        }
    }
}

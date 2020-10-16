package com.ds.avare.nmea;

import java.util.Locale;

public class Packet {

    protected String mPacket;
    
    /**
     * 
     */
    protected void assemble() {
        /*
         * Checksum
         */
        mPacket += "*";
        
        int xor = MessageFactory.checkSum(mPacket.getBytes());
        String ma = Integer.toHexString(xor).toUpperCase(Locale.getDefault());
        if(ma.length() < 2) {   // The checksum needs to be 2 ascii digits
            mPacket += "0";
        }
        mPacket += ma;
        mPacket += "\r\n";
    }

    /**
     * 
     */
    public String getPacket() {
        return mPacket;
    }
    
}

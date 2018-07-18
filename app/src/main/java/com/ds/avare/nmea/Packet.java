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
        mPacket += ma;
        mPacket += "\r\n";
    }

    /**
     * 
     * @param msg
     */
    public String getPacket() {
        return mPacket;
    }
    
}

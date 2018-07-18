/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.nmea;

/**
 * 
 * @author zkhan
 *
 */
public class MessageFactory {

    /**
     * Find NMEA checksum that excludes $ and things including, after *
     * @param bufin
     * @param len
     * @return
     */
    public static int checkSum(byte bufin[]) {
        int xor = 0;
        int i = 1;
        int len = bufin.length;
        /*
         * Find checksum from after $ to before *
         */
        while(i < len) {
            if(bufin[i] == 42) {
                break;
            }
            xor = xor ^ ((int)bufin[i] & 0xFF);
            i++;
        }

        return xor;
    }
    
    public static Message buildMessage(byte bufin[]) {
        
        int len = bufin.length;
        
        if(len < 6) {
            /*
             * A simple check for length
             */
            return null;
        }
        
        /*
         * Check checksum
         */
        byte cs[];
        cs = new byte[2];
        /*
         * Starts with $GP, ends with checksum *DD
         */
        if(bufin[0] == 36 && bufin[1] == 71) {
            int xor = checkSum(bufin);
           
            /*
             * Checksum is in xor data[len - 1] and data[len - 2] has checksum in Hex
             */
            System.arraycopy(bufin, len - 4, cs, 0, 2);
            String css = new String(cs);
            String ma = Integer.toHexString(xor);
            if(!ma.equalsIgnoreCase(css)) {
                return null;
            }
        }
        else {
        	return null;
        }

        
        /*
         * data has actual data and type is its type
         * Parse now
         */
        Message m = null;
        String data;
        String type;
        try {
            /*
             * Find type in data
             */
            data = new String(bufin);
            type = data.substring(3, data.indexOf(","));
        }
        catch (Exception e) {
            return null;
        }
        
        /**
         * Find which message we have
         */
        if(type.equals(MessageType.RecommendedMinimumSentence)) {
            m = new RMCMessage();
        }
        else if(type.equals(MessageType.EssentialFix)) {
            m = new GGAMessage();
        }
        else if(type.equals(MessageType.Traffic)) {
            m = new RTMMessage();
        }
                
        /*
         * Parse it.
         */
        if(null != m) {
            m.parse(data);
        }
        return(m);
    }    
    
}

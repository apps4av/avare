/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.gdl90;

/**
 * 
 * @author zkhan
 *
 */
public class ProductFactory {

    
    public static Product buildProduct(byte bufin[]) {

        BitInputStream s = new BitInputStream(bufin);

        if(false) {
            s.getBits(16);
            /*
             * Skip this. This over the air does not contain ADPU header.
             */
        }

        boolean flagAppMethod = s.getBits(1) != 0;
        boolean flagGeoLocator = s.getBits(1) != 0;
        boolean flagProviderSpec = s.getBits(1) != 0;

        int productID = s.getBits(11);
      
        if(flagAppMethod) {
            s.getBits(8);
        }
      
        if(flagGeoLocator) {
            s.getBits(20);
        }
      
        boolean segFlag = s.getBits(1) != 0;
      
        int timeOpts = s.getBits(2);
      
        // 00 - No day, No sec
        // 01 - sec
        // 10 - day
        // 11 - day, sec
        if((timeOpts & 0x02) != 0) {
            int month = s.getBits(4);
            int day   = s.getBits(5);
        }
        int hours = s.getBits(5);
        int mins  = s.getBits(6);
        if((timeOpts & 0x01) != 0) {
            int secs = s.getBits(6);
        }
      
        if(segFlag) {
            if(false) {     // do it the DO-267A way
                int productFileLength = s.getBits(12);
                int apduNumber = s.getBits(12);  
            }
            else {        // do it the mitre way
                int productFileID = s.getBits(10);
                int productFileLength = s.getBits(9);
                int apduNumber = s.getBits(9);
            }
            return null;
        }
      
        int totalRead = s.totalRead();
        int total = bufin.length;
      
        int length = total - totalRead;
        int offset = totalRead;
      
        Product p = null;
        
        switch(productID) {
            case 8:
                p = new Id8Product();
                break;
            case 9:
                p = new Id9Product();
                break;
            case 10:    
                p = new Id10Product();
                break;
            case 11:
                p = new Id11Product();
                break;
            case 12:
                p = new Id12Product();
                break;
            case 13:
                p = new Id13Product();
                break;
            case 63:
                p = new Id6364Product();
                ((Id6364Product)p).setConus(false);
                break;
            case 64:
                p = new Id6364Product();
                ((Id6364Product)p).setConus(true);
                break;
            case 413:
                p = new Id413Product();
                break;
            default:
                p = null;
                break;
        }

        byte data[] = new byte[length];
        System.arraycopy(bufin, offset, data, 0, length);

        /*
         * Parse it.
         */
        if(null != p) {
            p.parse(data);
        }
        
        return(p);   
    }   
}

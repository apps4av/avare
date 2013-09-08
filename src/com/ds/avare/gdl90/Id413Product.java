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
public class Id413Product extends Product {

    String mText;
 
    public Id413Product() {
        super(ProductType.PRODUCT_TYPE_TEXT);
    }

    @Override
    protected void parse(byte[] msg) {

        mText = "";
        int len = msg.length;
        int holder = 0;
        int i = 0;
        
        /*
         * Decode text: begins with @METAR, @TAF, @SPECI, @SUA, @PIREP, @WINDS
         */
        for(i = 0; i < (len - 3); i += 3) { 
            holder = 
                    (((int)msg[i + 0] & 0xFF) << 24) + 
                    (((int)msg[i + 1] & 0xFF) << 16) +
                    (((int)msg[i + 2] & 0xFF) << 8);
            
            /*
             * 4 chars in 3 bytes
             */
            int firstChar = Constants.DLAC_CODE[((holder & 0xFC000000) >> 26) & 0x3F]; 
            int secondChar = Constants.DLAC_CODE[((holder & 0x03F00000) >> 20) & 0x3F]; 
            int thirdChar = Constants.DLAC_CODE[((holder & 0x000FC000) >> 14) & 0x3F];
            int fourthChar = Constants.DLAC_CODE[((holder & 0x00003F00) >> 8) & 0x3F];
            
            mText += String.format("%c%c%c%c", firstChar, secondChar, thirdChar, fourthChar);
        }
        
        if(!mText.equals("")) {
            mText = mText.split("\u001E")[0];
            Logger.Logit(mText);
        }
        
    }

}

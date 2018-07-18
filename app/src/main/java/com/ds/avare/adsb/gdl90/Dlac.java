/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.adsb.gdl90;


/**
 * Created by zkhan on 7/10/17.
 */

public class Dlac {

    public static String decode(byte b1, byte b2, byte b3) {
        int holder =
                (((int) b1 & 0xFF) << 24) +
                        (((int) b2 & 0xFF) << 16) +
                        (((int) b3 & 0xFF) << 8);

        /*
         * 4 chars in 3 bytes
         */
        int firstChar = Constants.DLAC_CODE[((holder & 0xFC000000) >> 26) & 0x3F];
        int secondChar = Constants.DLAC_CODE[((holder & 0x03F00000) >> 20) & 0x3F];
        int thirdChar = Constants.DLAC_CODE[((holder & 0x000FC000) >> 14) & 0x3F];
        int fourthChar = Constants.DLAC_CODE[((holder & 0x00003F00) >> 8) & 0x3F];

        return String.format("%c%c%c%c", firstChar, secondChar, thirdChar, fourthChar);
    }

    public static String format(String in) {
        if (null == in) {
            return in;
        }
        if (!in.equals("")) {
            in = in.split("\u001E")[0];
            in = in.replaceAll("\n\t[A-Z]{1}", "\n"); /* remove invalid chars after newline */
        }
        return in;
    }

}

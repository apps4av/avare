/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adsb.gdl90;

import com.ds.avare.utils.Logger;

/**
 * @author zkhan
 */
public class MessageFactory {


    public static Message buildMessage(byte bufin[]) {

        int len = bufin.length;

        /*
         * Strip flag bytes 0x7E
         */
        if (len < 5) {
            return null;
        }
        byte strp[] = new byte[len - 2];
        System.arraycopy(bufin, 1, strp, 0, len - 2);

        /* Check CRC */
        byte[] inbuf = process(strp);

        if (null == inbuf) {
            /*
             * CRC fail
             */
            return null;
        }

        /*
         * Strip type and CRC to get actual data
         */
        int type = inbuf[0] & 0xFF;
        byte data[] = new byte[inbuf.length - 3];
        System.arraycopy(inbuf, 1, data, 0, inbuf.length - 3);

        /*
         * data has actual data and type is its type
         * Parse now
         */
        Message m = null;
        switch (type) {

            case MessageType.HEARTBEAT:
                m = new HeartbeatMessage();
                break;

            case MessageType.UPLINK:
                m = new UplinkMessage();
                break;

            case MessageType.OWNSHIP:
                m = new OwnshipMessage();
                break;

            case MessageType.OWNSHIP_GEOMETRIC_ALTITUDE:
                m = new OwnshipGeometricAltitudeMessage();
                break;

            case MessageType.TRAFFIC_REPORT:
                m = new TrafficReportMessage();
                break;

            case MessageType.BASIC_REPORT:
                m = new BasicReportMessage();
                break;

            case MessageType.LONG_REPORT:
                m = new LongReportMessage();
                break;

            case MessageType.DEVICE_REPORT:
                m = new DeviceReportMessage();
                break;

            default:
                m = null;
                break;
        }

        /*
         * Parse it.
         */
        if (null != m) {
            m.parse(data);
        }
        return (m);

    }

    /**
     * CRC16 process with 0x7D escape remove
     *
     * @param msg
     * @return
     */
    private static byte[] process(byte msg[]) {
        int i = 0;
        int length = 0;
        int len = msg.length;

        byte msgCrc[] = new byte[len];
        byte msgChar;
        while (i < len) {
            /*
             * 0x7D skip, and ^ with 0x20 to correct.
             */
            if (msg[i] == 0x7D) {
                i++;
                if (i >= len) {
                    break;
                }
                msgChar = (byte) (msg[i] ^ 0x20);
                msgCrc[length] = msgChar;
            } else {
                msgCrc[length] = msg[i];
            }
            length++;
            i++;
        }

        if (length < 2) {
            return null;
        }
        /*
         *  exclude CRC in CRC compute
         */
        int msb = ((int) msgCrc[length - 1]) & 0xFF;
        int lsb = ((int) msgCrc[length - 2]) & 0xFF;
        int inCrc = (msb << 8) + lsb;
        if (!Crc.checkCrc(msgCrc, length - 2, inCrc)) {
            Logger.Logit("CRC failed");
            return null;
        }

        /*
         * Return corrected message.
         */
        byte ret[] = new byte[length];
        System.arraycopy(msgCrc, 0, ret, 0, length);
        return ret;
    }


}

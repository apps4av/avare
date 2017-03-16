package com.ds.avare.utils;

import java.io.FileInputStream;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by zkhan on 3/14/17.
 */

public class PngCommentReader {


    public static float[] readPlate(String fileName) {

        FileChannel channel = null;

        try { // parsing a file causes unknown issues, so surround entire with exception catch
            channel = new FileInputStream(fileName).getChannel();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buffer.order(ByteOrder.BIG_ENDIAN); // BE for PNGs

            byte[] sign = new byte[8];
            buffer.get(sign);
            if(sign[0] == (byte)0x89 && sign[1] == (byte)0x50 && sign[2] == (byte)0x4E && sign[3] == (byte)0x47 && sign[4] == (byte)0x0D && sign[5] == (byte)0x0A && sign[6] == (byte)0x1A && sign[7] == (byte)0x0A) {
                // valid sign
                int len = 0;
                byte[] type = new byte[4];
                do {
                    len = buffer.getInt();
                    buffer.get(type);
                    if('t' == type[0] && 'E' == type[1] && 'X' == type[2] && 't' == type[3]) {
                        byte[] data = new byte[len];
                        buffer.get(data);
                        for(int count = 0; count < data.length; count++) {
                            if(0 == data[count]) { // remove nulls
                                data[count] = ' ';
                            }
                        }
                        String txt = new String(data);
                        if(txt.startsWith("Comment")) {
                            txt = txt.replace("Comment", "");
                            String toks[] = txt.split("[|]");
                            if(4 == toks.length) {
                                float matrix[] = new float[4];
                                matrix[0] = (float)Double.parseDouble(toks[0]);
                                matrix[1] = (float)Double.parseDouble(toks[1]);
                                matrix[2] = (float)Double.parseDouble(toks[2]);
                                matrix[3] = (float)Double.parseDouble(toks[3]);
                                return matrix;
                            }
                        }
                        buffer.position(buffer.position() + 4); // 4 for CRC
                    }
                    else {
                        buffer.position(buffer.position() + len + 4); // 4 for CRC
                    }
                }
                while(len > 0);

            }

        }
        catch (Exception e) {

        }
        try {
            channel.close();
        }
        catch (Exception e) {

        }

        return null;
    }
}

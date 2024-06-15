package com.ds.avare.utils;


import androidx.exifinterface.media.ExifInterface;

import java.io.FileInputStream;

/**
 * Created by zkhan on 3/14/17.
 */

public class PngCommentReader {


    public static float[] readPlate(String fileName) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(new FileInputStream(fileName));
        }
        catch (Exception e) {
            return null;
        }
        String comment = exif.getAttribute("UserComment");
        if(comment == null) {
            return null;
        }

        String[] toks = comment.split("\\|");
        if(toks.length == 4) {
            float[] matrix = new float[4];
            matrix[0] = (float)Double.parseDouble(toks[0]);
            matrix[1] = (float)Double.parseDouble(toks[1]);
            matrix[2] = (float)Double.parseDouble(toks[2]);
            matrix[3] = (float)Double.parseDouble(toks[3]);
            return matrix;
        }
        if(toks.length == 6) {
            float[] matrix = new float[12];
            matrix[6] = (float)Double.parseDouble(toks[0]);
            matrix[7] = (float)Double.parseDouble(toks[1]);
            matrix[8] = (float)Double.parseDouble(toks[2]);
            matrix[9] = (float)Double.parseDouble(toks[3]);
            matrix[10] = (float)Double.parseDouble(toks[4]);
            matrix[11] = (float)Double.parseDouble(toks[5]);
            return matrix;
        }
        return null;
    }
}

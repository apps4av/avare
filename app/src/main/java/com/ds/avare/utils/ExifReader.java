package com.ds.avare.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.png.PngDirectory;

import java.io.File;

/**
 * Created by zkhan on 3/14/17.
 */

public class ExifReader {


    public static float[] readPlate(String aname) {
        try {

            Metadata metadata = ImageMetadataReader.readMetadata(new File(aname));

            // Our plates data is under the PNG file, in Textual-Data filed, named Comment:
            // Data is of format nn|mm|xx|yy

            for (Directory directory : metadata.getDirectories()) {
                if(directory instanceof PngDirectory) {
                    for (Tag tag : directory.getTags()) {
                        if(tag.getTagType() == PngDirectory.TAG_TEXTUAL_DATA) {
                            String val = tag.getDescription();
                            if(val.startsWith("Comment:")) {
                                val = val.replace("Comment:", "");
                                String toks[] = val.split("[|]");
                                if(4 == toks.length) {
                                    float matrix[] = new float[4];
                                    matrix[0] = (float)Double.parseDouble(toks[0]);
                                    matrix[1] = (float)Double.parseDouble(toks[1]);
                                    matrix[2] = (float)Double.parseDouble(toks[2]);
                                    matrix[3] = (float)Double.parseDouble(toks[3]);
                                    return matrix;
                                }

                            }
                        }
                    }
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

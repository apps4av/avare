/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFolder {

    private static final String SHARED_FOLDER = "shared_prefs";

    /**
     * Get list of all files
     * @param dir
     * @param fileList
     */
    public static void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml") || name.endsWith(".kml");
            }
        });
        for (File file : files) {
            fileList.add(file);
            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }
    }

    public static boolean zipFiles(String folder, OutputStream os) {
        /**
         * List of files we want to zip, folder is parent of app preferences folder
         * Shared prefs has all Avare data except tracks
         * TODO : Tracks
         */

        String look1 = folder + File.separator + SHARED_FOLDER;
        List<File> fileList = new ArrayList<File>();
        getAllFiles(new File(look1), fileList);


        /**
         * Zip to output stream (could be File or Drive)
         */
        try {
            ZipOutputStream zos = new ZipOutputStream(os);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories

                    // put files starting from app's top level folder
                    FileInputStream fis = new FileInputStream(file);
                    String filetoSave = SHARED_FOLDER + File.separator + file.getName();
                    ZipEntry zipEntry = new ZipEntry(filetoSave);
                    zos.putNextEntry(zipEntry);

                    byte[] bytes = new byte[4096];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }

                    zos.closeEntry();
                    fis.close();
                }
            }
            zos.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    /*
     * Unzip to folder from Drive or zip file
     */
    public static boolean unzipFiles(String folder, InputStream is) {
        /**
         * List of files we want to zip, folder is parent of app preferences folder
         * Shared prefs has all Avare data except tracks
         * TODO : Tracks
         */

        /**
         * Zip to output stream (could be File or Drive)
         */
        try {
            ZipInputStream zis = new ZipInputStream(is);

            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                String fileName = ze.getName();
                File newFile = new File(folder + File.separator + fileName);

                //create all non existent folders
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                byte[] bytes = new byte[4096];
                while ((len = zis.read(bytes)) > 0) {
                    fos.write(bytes, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (Exception e) {
            return false;
        }
        return true;
    }
 }
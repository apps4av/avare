/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.weather;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.annotations.internal.ValueProcessorProvider;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.AnnotationEntryParser;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

/**
 * 
 * @author zkhan
 *
 */
public class InternetWeatherCache {

    private static final String AIREP_FILE = "/aircraftreports.cache.csv.stripped";
    
    private List<Airep> mAirep;

    /**
     * Task that would draw tiles on bitmap.
     */
    private WeatherTask                mWeatherTask; 
    private Thread                     mWeatherThread;
    private String                     mRoot;

    
    /**
     * 
     * @param root
     */
    public void parse(String root) {
        
        /*
         * Do weather parsing in background. It takes a long time.
         */
        mRoot = root;
        if(mWeatherThread != null) {
            if(mWeatherThread.isAlive()) {
                return;
            }
        }
        mWeatherTask =  new WeatherTask();
        mWeatherThread = new Thread(mWeatherTask);
        mWeatherThread.start();
    }
    
    /**
     * 
     * @return
     */
    public List<Airep> getAirep() {
        return mAirep;
    }
    
    
    private class WeatherTask implements Runnable {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        public void run() {
            try {
                
                /*
                 * Read the CSV
                 */
                Reader csvFile = new InputStreamReader(new FileInputStream(mRoot + AIREP_FILE));
            
                ValueProcessorProvider vpp = new ValueProcessorProvider();
                CSVReader<Airep> airepReader = new CSVReaderBuilder<Airep>(csvFile).strategy(CSVStrategy.UK_DEFAULT).entryParser(
                                new AnnotationEntryParser<Airep>(Airep.class, vpp)).build();
                mAirep = airepReader.readAll();
            }
            catch(Exception e) {
            }
        }
    }
}

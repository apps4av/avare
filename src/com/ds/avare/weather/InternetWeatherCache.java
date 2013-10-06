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
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.ds.avare.position.Projection;
import com.ds.avare.shapes.MetShape;
import com.ds.avare.storage.Preferences;
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
    private static final String METAR_FILE = "/metars.cache.csv.stripped";
    private static final String TAF_FILE = "/tafs.cache.csv.stripped";
    private static final String MET_FILE = "/airsigmets.cache.csv.stripped";
    
    private static final int AIREP_DISTANCE = 200;
    
    private List<Airep> mAirep;
    private List<Metar> mMetar;
    private List<Taf> mTaf;
    private List<AirSigMet> mAirSig;

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
    public void parse(Context ctx) {
        
        /*
         * Do weather parsing in background. It takes a long time.
         */
        mRoot = (new Preferences(ctx)).mapsFolder();
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
    public List<Airep> getAirep(double lon, double lat) {
        List<Airep> l = new ArrayList<Airep>();

        /*
         * Limit airep based of distance from this point
         */
        double lon2;
        double lat2;
        if(null == mAirep) {
            return null;
        }
        for(int m = 0; m < mAirep.size(); m++) {
            Airep mm = mAirep.get(m);
            try {
                lon2 = Double.parseDouble(mm.longitude);
                lat2 = Double.parseDouble(mm.latitude);
            }
            catch (Exception e) {
                continue;
            }
        
            Projection p = new Projection(lon, lat, lon2, lat2);
            if(p.getDistance() < AIREP_DISTANCE) {
                l.add(mm);
            }
        }
        return l;
    }

    /**
     * 
     * @return
     */
    public List<AirSigMet> getAirSigMet() {
        return mAirSig;
    }

    /**
     * 
     * @return
     */
    public Metar getMetar(String station) {
        if(null == mMetar || null == station) {
            return null;
        }
        for(int m = 0; m < mMetar.size(); m++) {
            Metar mm = mMetar.get(m);
            if(mm.stationId.equals(station) || mm.stationId.equals("K" + station)) {
                return mm;
            }
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public Taf getTaf(String station) {
        if(null == mTaf || null == station) {
            return null;
        }
        for(int m = 0; m < mTaf.size(); m++) {
            Taf mm = mTaf.get(m);
            if(mm.stationId.equals(station) || mm.stationId.equals("K" + station)) {
                return mm;
            }
        }
        return null;
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

                /*
                 * AIR/SIG MET
                 */
                Reader csvFile = new InputStreamReader(new FileInputStream(mRoot + MET_FILE));
                
                ValueProcessorProvider vpp = new ValueProcessorProvider();
                CSVReader<AirSigMet> asmReader = new CSVReaderBuilder<AirSigMet>(csvFile).strategy(CSVStrategy.UK_DEFAULT).entryParser(
                                new AnnotationEntryParser<AirSigMet>(AirSigMet.class, vpp)).build();
                mAirSig = asmReader.readAll();
                
                /*
                 * Convert AIRMET/SIGMETS to shapes compatible coordinates
                 */
                for(int i = 0; i < mAirSig.size(); i++) {
                    AirSigMet asm = mAirSig.get(i);
                    asm.shape = new MetShape(
                            asm.timeFrom + "-" + asm.timeTo + "\n" +
                            asm.hazard + "\n" +
                            asm.reportType + "\n" +
                            asm.severity + "\n" +
                            asm.rawText);
                    String tokens[] = asm.points.split("[;]");
                    for(int j = 0; j < tokens.length; j++) {
                        String point[] = tokens[j].split("[:]");
                        double lon = Double.parseDouble(point[0]);
                        double lat = Double.parseDouble(point[1]);
                        if(0 == lat || 0 == lon) {
                            continue;
                        }
                        asm.shape.add(lon, lat);
                    }
                }

                /*
                 * AIREP
                 */
                csvFile = new InputStreamReader(new FileInputStream(mRoot + AIREP_FILE));
            
                vpp = new ValueProcessorProvider();
                CSVReader<Airep> airepReader = new CSVReaderBuilder<Airep>(csvFile).strategy(CSVStrategy.UK_DEFAULT).entryParser(
                                new AnnotationEntryParser<Airep>(Airep.class, vpp)).build();
                mAirep = airepReader.readAll();
                
                
                /*
                 * METAR.
                 */
                csvFile = new InputStreamReader(new FileInputStream(mRoot + METAR_FILE));
                
                vpp = new ValueProcessorProvider();
                CSVReader<Metar> metarReader = new CSVReaderBuilder<Metar>(csvFile).strategy(CSVStrategy.UK_DEFAULT).entryParser(
                                new AnnotationEntryParser<Metar>(Metar.class, vpp)).build();
                mMetar = metarReader.readAll();

                /*
                 * TAF.
                 */
                csvFile = new InputStreamReader(new FileInputStream(mRoot + TAF_FILE));
                
                vpp = new ValueProcessorProvider();
                CSVReader<Taf> tafReader = new CSVReaderBuilder<Taf>(csvFile).strategy(CSVStrategy.UK_DEFAULT).entryParser(
                                new AnnotationEntryParser<Taf>(Taf.class, vpp)).build();
                mTaf = tafReader.readAll();

            }
            catch(Exception e) {
            }
        }
    }
}

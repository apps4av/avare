/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.webinfc;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.ds.avare.StorageService;
import com.ds.avare.flight.AircraftPrefflight;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 * Modified by kbabbert for multiple aircraft preferences from WebAppWnbInterface.java
 */
public class WebAppAcpInterface {
    private StorageService mService;
    private Preferences mPref;
    private WebView mWebView;

    private static final int MSG_UPDATE_ACP = 1;
    private static final int MSG_ACP_CALCULATE = 2;
    private static final int MSG_CLEAR_ACP_SAVE = 7;
    private static final int MSG_ADD_ACP_SAVE = 8;

    /**
     * Instantiate the interface and set the context
     */
    public WebAppAcpInterface(Context c, WebView ww, GenericCallback cb) {
        mPref = new Preferences(c);
        mWebView = ww;
    }

    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) {
        mService = s;
        mService.setAcps(AircraftPrefflight.getAcpsFromStorageFormat(mPref.getAcps()));
    }

    /**
     *
     */
    public void calculate() {
        mHandler.sendEmptyMessage(MSG_ACP_CALCULATE);
    }

    /**
     *
     */
    public void clearAcpSave() {
        mHandler.sendEmptyMessage(MSG_CLEAR_ACP_SAVE);
    }

    /**
     * Update the passed point on the ACP page
     * @param
     */
    public void updateAcp() {
        mHandler.sendEmptyMessage(MSG_UPDATE_ACP);
    }

    /**
     * New saved acp when the save list changes.
     */
    public void newSaveAcp() {

        clearAcpSave();
        LinkedList<AircraftPrefflight> acps = mService.getAcps();
        if(acps == null) {
            return;
        }
        if(acps.size() == 0) {
            init(acps);
        }

        for (AircraftPrefflight acp : acps) {
            Message m = mHandler.obtainMessage(MSG_ADD_ACP_SAVE, (Object)("'" + Helper.formatJsArgs(acp.getName()) + "'"));
            mHandler.sendMessage(m);
        }
    }

    /**
     * Add two samples to the saved ACP list
     * @param acps
     */
    private void init(LinkedList<AircraftPrefflight> acps) {
        acps.add(new AircraftPrefflight(AircraftPrefflight.ACP_SAMPLE1));
        acps.add(new AircraftPrefflight(AircraftPrefflight.ACP_SAMPLE2));

    }

    /**
     * 
     * @param data
     */
    @JavascriptInterface
    public void saveAcp(String data) {

        // get current
        JSONObject obj;
        try {
            obj = new JSONObject(data);
        } catch (JSONException e) {
            return;
        }


        AircraftPrefflight acp = new AircraftPrefflight(obj);
        mService.setAcp(acp);

        LinkedList<AircraftPrefflight> acps = mService.getAcps();
        if(acps == null) {
            return;
        }
        if(acps.size() == 0) {
            init(acps);
        }

        acps.add(mService.getAcp());

        /*
         * Save to storage on save button
         */
        mPref.putAcps(AircraftPrefflight.putAcpsToStorageFormat(acps));
        
        /*
         * Make a new working list since last one stored already 
         */
        mService.setAcp(new AircraftPrefflight(mService.getAcp().getJSON()));

        newSaveAcp();
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void loadAcp(String name) {
        LinkedList<AircraftPrefflight> acps = mService.getAcps();
        if(acps == null) {
            return;
        }
        if(acps.size() == 0) {
            init(acps);
        }


        for (AircraftPrefflight acp : acps) {
        	if(acp.getName().equals(name)) {
        		mService.setAcp(new AircraftPrefflight(acp.getJSON()));
        	}
        }

        updateAcp();
    }

     /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void saveDelete(String name) {
    	int toremove = -1;
        int i = 0;
        LinkedList<AircraftPrefflight> acps = mService.getAcps();
        if(acps == null) {
            return;
        }
        if(acps.size() == 0) {
            init(acps);
        }


        // Find and remove
        for (AircraftPrefflight acp : acps) {
        	if(acp.getName().equals(name)) {
        		toremove = i;
        	}
        	i++;
        }
        if(toremove > -1) {
        	acps.remove(toremove);
        }
        
        /*
         * Save to storage on save button
         */
        mPref.putAcps(AircraftPrefflight.putAcpsToStorageFormat(acps));

        newSaveAcp();

    }

    @JavascriptInterface
    public void UpdateACPreferences (
            String ACPName,
            String pilotname,
            String homebase,
            String tailnumber,
            String aircrafttype,
            String aircrafticaocode,
            String aircraftcolor1,
            String aircraftcolor2,
            String aircraftequipment,
            String aircraftsurveillance,
            String aircraftotherinfo,
            String aircrafttas,
            String aircraftbgsr,
            String aircraftfuelburn,
            String emerchecklist
    ) {


        /*
         * Save to preferences
         */
        mPref.setACPName(ACPName);
        mPref.setPilotName(pilotname);
        mPref.setHomeBase(homebase);
        mPref.setTailNumber(tailnumber);
        mPref.setAircraftType(aircrafttype);
        mPref.setAircraftICAOCode(aircrafticaocode);
        mPref.setAircraftColor1(aircraftcolor1);
        mPref.setAircraftColor2(aircraftcolor2);
        mPref.setAircraftEquipment(aircraftequipment);
        mPref.setAircraftSurveillance(aircraftsurveillance);
        mPref.setAircraftOtherInfo(aircraftotherinfo);
        mPref.setAircraftTAS(aircrafttas);
        mPref.setAircraftBGSR(aircraftbgsr);
        mPref.setAircraftFuelBurn(aircraftfuelburn);
        mPref.setEmergencyChecklist(emerchecklist);

    }

    /*
     * Get from preferences
     */

    @JavascriptInterface
    public String getPilotContact() {
        return mPref.getPilotContact();
    }

    @JavascriptInterface
    public String getAircraftHomeBase() {
        return mPref.getAircraftHomeBase();
    }

    @JavascriptInterface
    public String getAircraftTailNumber() {
        return mPref.getAircraftTailNumber();
    }

    @JavascriptInterface
    public String getAircraftType() {
        return mPref.getAircraftType();
    }

    @JavascriptInterface
    public String getstringAircraftICAOCode() {
        return mPref.getstringAircraftICAOCode();
    }

    @JavascriptInterface
    public String getAircraftColorPrimary() {
        return mPref.getAircraftColorPrimary();
    }

    @JavascriptInterface
    public String getAircraftColorSecondary() {
        return mPref.getAircraftColorSecondary();
    }

    @JavascriptInterface
    public String getAircraftEquipment() {
        return mPref.getAircraftEquipment();
    }

    @JavascriptInterface
    public String getAircraftSurveillanceEquipment() {
        return mPref.getAircraftSurveillanceEquipment();
    }

    @JavascriptInterface
    public String getAircraftOtherInfo() {

        return mPref.getAircraftOtherInfo();
    }

    @JavascriptInterface
    public int getAircraftTAS() {
        return mPref.getAircraftTAS();
    }

    @JavascriptInterface
    public String getstringBestGlideSinkRate() {
        return mPref.getstringBestGlideSinkRate();
    }

    @JavascriptInterface
    public String getstringFuelBurn() {
        return mPref.getstringFuelBurn();
    }

    @JavascriptInterface
    public String getEmergencyChecklist() {
        return mPref.getEmergencyChecklist();
    }

    @JavascriptInterface
    public String getACPName() {
        return mPref.getACPName();
    }

    @JavascriptInterface
    public void updateACPName(String prefname) {
        mPref.setACPName(prefname);
    }

    @JavascriptInterface
    public void updatePilotContact(String prefname) {
        mPref.setPilotName(prefname);    }

    @JavascriptInterface
    public void updateHomeBase(String prefname) {
        mPref.setHomeBase(prefname);    }

    @JavascriptInterface
    public void updateTailNumber(String prefname) {
        mPref.setTailNumber(prefname);    }

    @JavascriptInterface
    public void updateAircraftType(String prefname) {
        mPref.setAircraftType(prefname);    }

    @JavascriptInterface
    public void updateAircraftColor1(String prefname) {
        mPref.setAircraftColor1(prefname);    }

    @JavascriptInterface
    public void updateAircraftColor2(String prefname) {
        mPref.setAircraftColor2(prefname);    }

    @JavascriptInterface
    public void updateAircraftEquipment(String prefname) {
        mPref.setAircraftEquipment(prefname);    }

    @JavascriptInterface
    public void updateAircraftSurveillance(String prefname) {
        mPref.setAircraftSurveillance(prefname);    }

    @JavascriptInterface
    public void updateAircraftOtherInfo(String prefname) {
        mPref.setAircraftOtherInfo(prefname);    }

    @JavascriptInterface
    public void updateAircraftTAS(String prefname) {
        mPref.setAircraftTAS(prefname);    }

    @JavascriptInterface
    public void updateAircraftBGSR(String prefname) {
        mPref.setAircraftBGSR(prefname);    }

    @JavascriptInterface
    public void updateAircraftFuelBurn(String prefname) {
        mPref.setAircraftFuelBurn(prefname);    }

    @JavascriptInterface
    public void updateEmergencyChecklist(String prefname) {
        mPref.setEmergencyChecklist(prefname);    }

    @JavascriptInterface
    public String getDistanceUnit() {
        return mPref.getDistanceUnit();
    }


    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     * Must use handler for functions called from JS, but for uniformity, call all JS from this handler
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(MSG_UPDATE_ACP == msg.what) {
                /*
                 * Now update HTML with latest acp stuff, do this every time we start the List screen as
                 * things might have changed.
                 */
                if(null != mService.getAcp()) {
                    String data = mService.getAcp().getJSON().toString();

                    if (null != data) {
                        mWebView.loadUrl("javascript:acp_set('" + data + "')");
                    }
                }
            }
            else if(MSG_ADD_ACP_SAVE == msg.what) {
            	String func = "javascript:acp_save_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
            else if(MSG_ACP_CALCULATE == msg.what) {
                String func = "javascript:acp_calculate()";
                mWebView.loadUrl(func);
            }
            else if(MSG_CLEAR_ACP_SAVE == msg.what) {
                String func = "javascript:acp_save_clear()";
                mWebView.loadUrl(func);
            }

        }
    };

}
package com.ds.avare.connections;

import android.content.Context;

import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;



public class Dump1090Connection extends Connection {

    private static Dump1090Connection mConnection;
    private URL mURL;

    private Dump1090Connection() {
        super("Dump1090 Input");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {
                while(isRunning()) {
                    JSONArray trafficList = fetchDump1090JSON();

                    if (null != trafficList) {
                        for (int i = 0; i < trafficList.length(); i++) {
                            JSONObject sent = new JSONObject();
                            try {
                                JSONObject recv = trafficList.getJSONObject(i);
                                sent.put("type", "traffic");
                                sent.put("longitude", recv.getDouble("lon"));
                                sent.put("latitude", recv.getDouble("lat"));
                                sent.put("speed", recv.getDouble("speed"));
                                sent.put("bearing", recv.getDouble("track"));
                                sent.put("altitude", recv.getDouble("altitude"));
                                sent.put("callsign", recv.getString("flight").trim());
                                sent.put("address", Integer.decode("0x" + recv.getString("hex")));
                                sent.put("time", System.currentTimeMillis());
                            } catch (JSONException e1) {
                                Logger.Logit(e1.toString());
                                continue;
                            }
                            sendDataToHelper(sent.toString());
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch(Exception e) {
                    }
                }
                return null;
            }
        });
    }

    @Override
    public boolean connect(String to, boolean secure) {
        try {
            Logger.Logit("Fetch Dump1090 feed from http://" + to + "/data.json");
            mURL = new URL("http://" + to + "/data.json");
            connectConnection();
        } catch (Exception e) {
            Logger.Logit("Illegal URL format.");
            mURL = null;
            return false;
        }
        return true;
    }

    @Override
    public void write(byte[] aData) {

    }

    @Override
    public void disconnect() {
        mURL = null;
        disconnectConnection();
    }

    public static Dump1090Connection getInstance(Context ctx) {
        if(null == mConnection) {
            mConnection = new Dump1090Connection();
        }
        return mConnection;
    }

    @Override
    public List<String> getDevices() {
        return new ArrayList<>();
    }

    @Override
    public String getConnDevice() {
        return "";
    }

    public JSONArray fetchDump1090JSON() {
        if (null == mURL) return null;

        StringBuffer response = new StringBuffer();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) mURL.openConnection();
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");

            int status = conn.getResponseCode();
            if (status != 200) {
                // Return empty if fetch failed.
                Logger.Logit("HTTP connection failed, status code: " + status);
                return null;
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (response.length() > 0) {
                try {
                    JSONArray array = new JSONArray(response.toString());
                    Logger.Logit("Get traffic JSON array of size " + array.length());
                    return array;
                } catch (Exception e) {
                    Logger.Logit(e.toString());
                    return null;
                }
            }
            return null;
        }
    }
}

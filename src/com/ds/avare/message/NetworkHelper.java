/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare.message;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author zkhan
 *
 */
public class NetworkHelper {

    /*
     * This has to be provided by apps4av
     */
    public static String getServer() {
        return "https://apps4av.net/new/";
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws Exception propagated from POST.
     */
    public static String post(String endpoint, Map<String, String> params)
            throws Exception {   
         
        URL url;
        url = new URL(endpoint);
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded;charset=UTF-8");
        // post the request
        OutputStream out = conn.getOutputStream();
        out.write(bytes);
        out.close();
        // handle the response
        int status = conn.getResponseCode();
        if(status != 200) {
            throw new Exception("POST exception HTTP return code " + status);
        }
        
        // Return value
        byte ret[] = new byte[32768];
        InputStream is = conn.getInputStream();
        int len = 0;
        int index = 0;
        do {
        	len = is.read(ret, index, ret.length - index);
        	if(len <= 0) {
        		break;
        	}
        	index += len;
        } while(true);
        
        conn.disconnect();
        if(index > 0) {
            return new String(ret);
        }
        return "";
    }

}

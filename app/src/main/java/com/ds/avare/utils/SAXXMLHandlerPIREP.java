/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author zkhan
 *
 */
public class SAXXMLHandlerPIREP extends DefaultHandler {
    private List<String> mText;
    private String mTempText;
    private String mTempVal;
    private String mTempType;
 
    public SAXXMLHandlerPIREP() {
        mText = new ArrayList<String>();
    }
 
    public List<String> getText() {
        return mText;
    }
 
    // Event Handlers
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) {
        mTempVal = "";
        if(qName.equalsIgnoreCase("AircraftReport")) {
            mTempText = new String();
            mTempType = new String();
        }
    }
 
    public void characters(char[] ch, int start, int length) {
        mTempVal += new String(ch, start, length);
    }
 
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("raw_text")) {
            mTempText = mTempVal;
        }
        if (qName.equalsIgnoreCase("report_type")) {
            mTempType = mTempVal;
        }
        if(qName.equalsIgnoreCase("AircraftReport")) {
            if(!mTempType.equals("AIREP")) {
                mText.add(mTempText);
            }
        }
    }
}

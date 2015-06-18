/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.storage;

import java.util.LinkedHashMap;

/**
 * @author zkhan
 *
 */
public class StringPreference {

    private String mDbType;
    private String mDestType;
    private String mId;
    private String mName;
    
    /**
     * 
     */
    public StringPreference(String destinationType, String dbType, String name, String id) {
        mDbType = dbType;
        mDestType = destinationType;
        mId = id;
        mName = name;
    }

    /**
     * 
     * @return
     */
    private String joinName() {
        return mDestType + ";" + mDbType + ";" + mName;
    }
    
    /**
     * 
     * @return
     */
    public void putInHash(LinkedHashMap<String, String> params) {
        /*
         * Make a large key and small ID
         */
        params.put(joinName(), mId);
    }

    /**
     * 
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * 
     * @return
     */
    public String getType() {
        return mDestType;
    }

    /**
     * 
     * @return
     */
    public String getHashedName() {
        return mId + "::" + joinName();
    }
    
    /**
     * 
     * @param id
     * @param joinedName
     * LocationID::DestType;Type;FacilityName
     * @return
     */
    static public String getHashedName(String id, String joinedName) {
        return id + "::" + joinedName;
    }

    /**
     * Google Address name format for storage
     * @param name
     * @return
     */
    static public String formatAddressName(String name) {
    	// Change \n to space.  Google might pass the address like the following:
    	// 123 Main St\n\nGotham City, New Jersey
        return name.replaceAll("http:.*", "").replaceAll(",", " ").replaceAll("\n", " ");
    }

    /**
     * 
     * @param hashedName
     * @return
     */
    static public String parseHashedNameId(String hashedName) {
        String tokens[] = hashedName.split("::");
        if(tokens.length > 0) {
            return (tokens[0]);
        }
        return null;
    }

    /**
     * 
     * @param hashedName
     * @return
     */
    static public String parseHashedNameIdBefore(String hashedName) {
        String tokens[] = hashedName.split("@");
        if(tokens.length > 1) {
            return (tokens[0]);
        }
        return "";
    }

    /**
     * 
     * @param hashedName
     * @return
     */
    static public String parseHashedNameIdAfter(String hashedName) {
        String tokens[] = hashedName.split("@");
        if(tokens.length > 1) {
            return (tokens[1]);
        }
        return hashedName;
    }

    /**
     * 
     * @param hashedName
     * @return
     */
    static public String parseHashedNameDestType(String hashedName) {
        String tokens[] = hashedName.split("::");
        if(tokens.length > 1) {
            String token[] = tokens[1].split(";");
            if(token.length > 0) {
                return (token[0]);
            }
        }
        return null;
    }

    /**
     * 
     * @param hashedDesc
     * @return
     */
    static public String parseHashedNameDesc(String hashedName) {
        String tokens[] = hashedName.split("::");
        if(tokens.length > 1) {
            String token[] = tokens[1].split(";");
            if(token.length > 0) {
                return (token[tokens.length - 1]);
            }
        }
        return null;
    }

    /**
     * 
     * @param hashedName
     * @return
     */
    static public String parseHashedNameDbType(String hashedName) {
        String tokens[] = hashedName.split("::");
        if(tokens.length > 1) {
            String token[] = tokens[1].split(";");
            if(token.length > 1) {
                return (token[1]);
            }
        }
        return null;
    }

    /**
     * 
     * @param hashedName
     * @return
     */
    static public String parseHashedNameFacilityName(String hashedName) {
        String tokens[] = hashedName.split("::");
        if(tokens.length > 1) {
            String token[] = tokens[1].split(";");
            if(token.length > 2) {
                return (token[2]);
            }
        }
        return null;
    }

}

/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare.utils;

import com.ds.avare.storage.Preferences;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * @author zkhan
 * Find user's google email from account manager
 *
 */
public class PossibleEmail {

    /**
     * 
     * @param ctx
     * @return
     */
    public static String[] getAll(Context ctx) {
        try{
            Account[] accounts = AccountManager.get(ctx).getAccountsByType("com.google");
            String emails[] = new String[accounts.length];
            for(int i = 0; i < emails.length; i++) {
            	emails[i] = accounts[i].name;
            }
            return emails;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 
     * @param ctx
     * @return
     */
    public static String get(Context ctx) {
    	Preferences pref = new Preferences(ctx);
    	String email = pref.getRegisteredEmail();
    	if(email == null) {
    		// Backward compatibility
    		String[] act = getAll(ctx);
    		if(act != null && act.length > 0) {
    			email = act[0];
    		}
    	}
    	return email;
    }    
}

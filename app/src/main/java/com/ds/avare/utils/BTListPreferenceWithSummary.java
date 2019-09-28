/*
Copyright (c) 2019, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Set;

/**
 *
 * @author rwalker
 * A custom list preference configuration control that populates the items
 * with paired bluetooth connections.
 */
public class BTListPreferenceWithSummary extends ListPreference {
    public static final String NONE="OFF";
    private String mOriginalSummary = "";
    private CharSequence[] mSelections;

    public BTListPreferenceWithSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
        mOriginalSummary = super.getSummary().toString();
        mSelections = getSelections();
        super.setEntries(mSelections);
        super.setEntryValues(mSelections);
        super.setDefaultValue(NONE);
    }

    public BTListPreferenceWithSummary(Context context) {
        super(context);
        mOriginalSummary = super.getSummary().toString();
        mSelections = getSelections();
        super.setEntries(mSelections);
        super.setEntryValues(mSelections);
        super.setDefaultValue(NONE);
    }

    @Override
    public void setValue(String value) {
        for(CharSequence sel : mSelections) {
            if(sel.equals(value)) {
                super.setValue(value);
                setSummary(mOriginalSummary + " (" + value + ")");
                return;
            }
        }
        super.setValue(NONE);
        setSummary(mOriginalSummary + " (" + value + ")");
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(mOriginalSummary + " (" + getEntry() + ")");
    }

    // Build and return an array of all available Bluetooth paired names
    private CharSequence[] getSelections() {

        // Allocate a new arraylist to hold the names
        ArrayList<CharSequence> entries = new ArrayList<>();
        entries.add(NONE);

        // fetch the bt interface adapter
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null != btAdapter) {

            // Fetch collection of bluetooth devices
            Set<BluetoothDevice> setDevices = btAdapter.getBondedDevices();

            // For each device we found, get the name. It's possible for the getName()
            // to fail and return a null
            for (BluetoothDevice btd : setDevices) {
                String btName = btd.getName();
                if(null != btName) {
                    entries.add(btName);
                }
            }

            // tell bluetooth to stop scanning. it's a power issue
            btAdapter.cancelDiscovery();
        }

        // Return with an array of what we just built
        return entries.toArray(new CharSequence[0]);
    }
}

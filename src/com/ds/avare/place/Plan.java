/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;



import com.ds.avare.gps.GpsParams;

/**
 * 
 * @author zkhan
 *
 */
public class Plan {

    private Destination[] mDestination = new Destination[MAX_DESTINATIONS];
    
    private static final int MAX_DESTINATIONS = 10;
    
    
    /**
     * 
     * @param dataSource
     */
    public Plan() {
    }

    /**
     * 
     */
    public Destination getDestination(int index) {
        /*
         * Check for null.
         */
        if(index >= MAX_DESTINATIONS) {
            return null;
        }
        return(mDestination[index]);
    }

    /**
     * 
     * @return
     */
    public int getDestinationNumber() {

        /*
         * Get all airports in a string array in this Plan.
         */
        int id;
        for(id = 0; id < MAX_DESTINATIONS; id++) {
            if(getDestination(id) == null) {
                break;
            }
        }
        return(id);
    }

    /**
     * 
     * @return
     */
    public void remove(int rmId) {
        int num = getDestinationNumber() - 1;
        if(rmId > num || rmId < 0) {
            return;
        }
        mDestination[rmId] = null;
        for(int id = rmId; id < num; id++) {
            mDestination[id] = mDestination[id + 1];
            mDestination[id + 1] = null;
        }        
    }

    /**
     * 
     * @return
     */
    public boolean appendDestination(Destination dest) {

        int n = getDestinationNumber();
        if(n >= MAX_DESTINATIONS) {
            return false;
        }
        
        if(n > 0) {
            /*
             * Check if last set was set again, it makes no sense to have same dest twice in sequence
             */
            if(mDestination[n - 1].getStorageName().equals(dest.getStorageName())) {
                return false;
            }
        }
        mDestination[n] = dest;
        
        return(true);
    }

    /**
     * 
     * @param lon
     * @param lat
     */
    public void updateLocation(GpsParams params) {
        
        for(int id = 0; id < getDestinationNumber(); id++) {
            mDestination[id].updateTo(params);
        }
    }
}

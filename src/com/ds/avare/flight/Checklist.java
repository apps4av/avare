/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.flight;

import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * All lists get stored and get retrieved in JSON format
 * @author zkhan
 *
 */
public class Checklist {
    
    private String mSteps;
    private String mName;
    private int mWorkingIndex;
    
    private static String DELIM = "::";
    
    /**
     * 
     * @param name
     */
    public Checklist(String name) {
        mName = name;
        mSteps = "";
        mWorkingIndex = 0;
    }

    
    /**
     * Complete from string
     * @param name
     */
    public Checklist(String name, String steps) {
        mName = name;
        mSteps = steps;
        mWorkingIndex = 0;
    }

    /**
     * From JSON
     * @param name
     */
    public Checklist(JSONObject json) {
        try {
            mName = json.getString("name");
            mSteps = json.getString("steps");
        } catch (JSONException e) {
            mName = "";
            mSteps = "";
        }
    }

    /**
     * 
     * @param name
     */
    public void changeName(String name) {
        mName = name;
    }

    /**
     * Step at the end
     * @param step
     */
    public void addStep(String step) {
        mSteps += step.replaceAll(DELIM, "--") + DELIM;
    }
    
    /**
     * Get steps in encoded form
     * @return
     */
    public String getSteps() {
        return mSteps;
    }
    
    /**
     * Remove a step
     * @param pos
     */
    public void removeStep(int pos) {
        String steps[] = getStepsArray();
        if(pos < 0 || pos >= steps.length) {
            return;
        }
        
        /*
         * Remove a step
         */
        mSteps = "";
        for (int i = 0; i < steps.length; i++) {
            if(i == pos) {
                continue;
            }
            mSteps += steps[i] + DELIM;
        }
    }

    /**
     * Insert a step in the middle
     * @param step
     * @param pos
     */
    public void insertStep(String step, int pos) {
        String steps[] = getStepsArray();
        
        /*
         * Insert a step
         */
        mSteps = "";
        int i;
        for (i = 0; i < steps.length; i++) {
            if(i == pos) {
                mSteps += step.replaceAll(DELIM, "--") + DELIM;
            }
            mSteps += steps[i] + DELIM;
        }
        // In the end
        if(i == pos) {
            mSteps += step.replaceAll(DELIM, "--") + DELIM;
        }
    }

    /**
     * Insert a step in the middle
     * @param step
     * @param pos
     */
    public void moveStep(int from, int to) {
        String steps[] = getStepsArray();
        if(from < 0 || from >= steps.length || to < 0 || to >= steps.length) {
            return;
        }
        String step = steps[from];
        removeStep(from);
        insertStep(step, to);
    }

    /**
     * Get all the steps
     * @return
     */
    public String[] getStepsArray() {
    	if(mSteps.equals("")) {
    		return new String[0];
    	}
        String tokens[] = mSteps.split(DELIM);
        return tokens;
    }
    
    
    /**
     * Get in JSON format
     * @return
     */
    public JSONObject getJSON() {
        JSONObject jsonAdd = new JSONObject();
        try {
            jsonAdd.put("name", mName);
            jsonAdd.put("steps", getSteps());
        } catch (JSONException e) {
            return null;
        }
        
        return jsonAdd;
    }
    
    /**
     * 
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * Put a list of checklists in JSON array
     * @param cls
     * @return
     */
    public static String putCheckListsToStorageFormat(LinkedList<Checklist> cls) {
        
        JSONArray jsonArr = new JSONArray();
        for(Checklist c : cls) {
            
            JSONObject o = c.getJSON();
            jsonArr.put(o);
        }
        
        return jsonArr.toString();
    }
    
    /**
     * Gets an array of check lists from storage JSON
     * @return
     */
    public static LinkedList<Checklist> getCheckListsFromStorageFromat(String json) {
        JSONArray jsonArr;
        LinkedList<Checklist> ret = new LinkedList<Checklist>();
        try {
            jsonArr = new JSONArray(json);
        } catch (JSONException e) {
            return ret;
        }
        
        for(int i = 0; i < jsonArr.length(); i++) {
            try {
                JSONObject o = jsonArr.getJSONObject(i);
                ret.add(new Checklist(o));
            } catch (JSONException e) {
                continue;
            }
        }
        
        return ret;
    }
   
    
    /**
     * Move index back
     */
    public void moveBack() {
    	mWorkingIndex--;
    	if(mWorkingIndex < 0) {
    		mWorkingIndex = 0;
    	}    	
    }

    /**
     * Move index forward
     */
    public void moveForward() {
    	mWorkingIndex++;
    	if(mWorkingIndex >= getStepsArray().length) {
    		mWorkingIndex = getStepsArray().length - 1;
    	}
    }

    /**
     * Move index to
     */
    public void moveTo(int item) {
    	mWorkingIndex = item;
    	if(mWorkingIndex >= getStepsArray().length) {
    		mWorkingIndex = getStepsArray().length - 1;
    	}
    	if(mWorkingIndex < 0) {
    		mWorkingIndex = 0;
    	}
    }

    /**
     * Item goes up with index 
     */
    public void moveItemUp() {
    	if(mWorkingIndex <= 0) {
    		return;
    	}
    	moveStep(mWorkingIndex, mWorkingIndex - 1);
    	mWorkingIndex--; // Move with step
    }
    
    /**
     * Item goes down with index 
     */
    public void moveItemDown() {
    	// Do not get past last
    	mWorkingIndex++;
    	if(mWorkingIndex >= getStepsArray().length) {
    		mWorkingIndex = getStepsArray().length - 1;
    		return;
    	}
    	moveStep(mWorkingIndex - 1, mWorkingIndex);    	
    }
    
    /**
     * Delete item at index
     */
    public void deleteItem() {
    	removeStep(mWorkingIndex);
    	mWorkingIndex--;
    	// move with index
    	if(mWorkingIndex < 0) {
    		mWorkingIndex = 0;
    	}
    }
    
    /**
     * 
     * @return
     */
    public boolean isSelected(int step) {
    	return (mWorkingIndex == step);
    }
}

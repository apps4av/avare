/*
Copyright (c) 2012, Apps4Av Inc. (ds.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.storage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Sticky edit text, value stored in Preferences
 * @author zkhan
 *
 */
public class SavedEditText extends EditText {

	Preferences mPref = null;
	
	/**
	 * Get value from saved
	 */
	private void setup(Context ctx) {
		
		/*
		 * Get value from stored prefs
		 */
		mPref = new Preferences(ctx);
		String val  = mPref.getEditTextValue(getId());
		if(val != null) {
			setText(val);
		}
	}
	
	public SavedEditText(Context context) {
		super(context);
		setup(context);
		// TODO Auto-generated constructor stub
	}

	public SavedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		setup(context);
	}

	public SavedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		setup(context);
	}


	/**
	 * Override the text changed callback to save the text
	 */
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count){
		super.onTextChanged(s, start, before, count);
		if(null != mPref) {
			String val = getText().toString();
			mPref.setEditTextValue(getId(), val);
		}
	} 
}

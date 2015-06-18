/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.animation;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * A custom toggle button as Android default sucks
 * @author zkhan
 *
 */
public class TwoButton extends Button implements OnClickListener {

	private boolean mOn;
	private String mOnName;
	private String mOffName;
	TwoClickListener mListener;

	/**
	 * 
	 */
	private void setup(Context context, AttributeSet attrs) {

		/*
		 * Get text on and off values from XML layout
		 */
		String packageName = "http://schemas.android.com/apk/res/android";
		
		mOnName = null;
		mOffName = null;
		if(null != attrs) {
			mOnName = context.getString(attrs.getAttributeResourceValue(packageName, "textOn", 0));
			mOffName = context.getString(attrs.getAttributeResourceValue(packageName, "textOff", 0));
		}
		
		if(null == mOnName) {
			mOnName = "";
		}
		if(null == mOffName) {
			mOffName = "";
		}
		
		setOnClickListener(this);
		
		/*
		 * Default state on
		 */
		mOn = false;
		setText(mOffName);
	}
	
	/**
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public TwoButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setup(context, attrs);
	}

	/**
	 * 
	 * @param context
	 * @param attrs
	 */
	public TwoButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context, attrs);
	}

	/**
	 * 
	 * @param context
	 */
	public TwoButton(Context context) {
		super(context);
		setup(context, null);
	}
	
	/**
	 * 
	 * @param on
	 */
	public void setChecked(boolean checked) {
		mOn = checked;
		if(mOn) {
			setText(mOnName);
		}
		else {
			setText(mOffName);
		}		
	}
	
	
	/**
	 * 
	 * @return
	 */
	public boolean isChecked() {
		return mOn;
	}

	/**
	 * 
	 */
	@Override
	public void onClick(View v) {
		setChecked(!mOn);
		/*
		 * Just call the listener of button
		 */
		if(mListener != null) {
			mListener.onClick(v);
		}
	}

	/**
	 * 
	 * @author zkhan
	 *
	 */
	public interface TwoClickListener {
		public void onClick(View v);
	}
	
	/**
	 * 
	 * @param listener
	 */
	public void setTwoClickListener(TwoClickListener listener) {
		mListener = listener;
	}
}

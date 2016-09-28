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
 * A custom triple toggle button as Android default sucks
 * @author jamez70
 *
 */
public class ThreeButton extends Button implements OnClickListener {

//    private boolean mOn;
    private Integer mState;
    private String mState1Name;
    private String mState2Name;
    private String mState3Name;
    ThreeClickListener mListener;

    /**
     *
     */
    private void setup(Context context, AttributeSet attrs) {

		/*
		 * Get text on and off values from XML layout
		 */
        String packageName = "http://schemas.android.com/apk/res/android";

        mState1Name = "";
        mState2Name = "";
        mState3Name = "";

        setOnClickListener(this);

		/*
		 * Default state on
		 */
        mState = 0;
        setText(mState1Name);
    }

    /**
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public ThreeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    /**
     *
     * @param context
     * @param attrs
     */
    public ThreeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    /**
     *
     * @param context
     */
    public ThreeButton(Context context) {
        super(context);
        setup(context, null);
    }

    /**
     *
     * @param state
     */
    public void setState(Integer state) {
        mState = state;
        switch(mState)
        {
            case 0:
                setText(mState1Name);
                break;
            case 1:
                setText(mState2Name);
                break;
            case 2:
                setText(mState3Name);
                break;
        }
    }
    public void setStateNames(String s1, String s2, String s3)
    {
        mState1Name=s1;
        mState2Name=s2;
        mState3Name=s3;
        setState(mState);
    }

    /**
     *
     * @return
     */
    public Integer getState() {
        return mState;
    }

    public void setStateString(String State)
    {
        if (State == mState1Name)
            setState(0);
        else if (State == mState2Name)
            setState(1);
        else if (State == mState3Name)
            setState(2);
        else
            setState(0);
    }

    public String getStateString() {
        String state;
        state=mState1Name;
        switch(mState) {
            case 0:
                state=mState1Name;
                break;
            case 1:
                state=mState2Name;
                break;
            case 2:
                state=mState3Name;
                break;
        }
        return state;
    }
    /**
     *
     */
    @Override
    public void onClick(View v) {
        mState++;
        if (mState > 2)
            mState = 0;
        setState(mState);
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
    public interface ThreeClickListener {
        public void onClick(View v);
    }

    /**
     *
     * @param listener
     */
    public void setThreeClickListener(ThreeClickListener listener) {
        mListener = listener;
    }
}

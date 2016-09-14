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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

/**
 * Created by zkhan on 2/28/16.
 *
 * This is used to replace spinners.
 */
public class OptionButton extends Button implements View.OnClickListener {

    private Context mContext;
    private ArrayList<String> mOptions;
    private int mSelected;
    private GenericCallback mCallback;
    private CharSequence mLabel;

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mSelected = 0;
        setOnClickListener(this);
        mOptions = new ArrayList<String>();
        if(null == attrs) {
            return;
        }
        int[] attrsArray = new int[] {
                android.R.attr.entries, // 0
                android.R.attr.labelFor
        };
        TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
        if(null == ta) {
            return;
        }
        CharSequence[] entries = ta.getTextArray(0);
        @SuppressWarnings("ResourceType")
        CharSequence label = ta.getText(1);
        ta.recycle();
        if(null == entries) {
            return;
        }
        for (CharSequence s : entries) {
            mOptions.add(String.valueOf(s));
        }
        this.setText(entries[0]);

        mLabel = label;
    }

    public OptionButton(Context context) {
        super(context);
        init(context, null);
    }

    public OptionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OptionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * This must be called to set up the dialog callback
     * @param callback
     */
    public void setCallback(GenericCallback callback) {
        mCallback = callback;
    }

    /**
     *
     * @param index
     */
    public void setCurrentSelectionIndex(int index) {
        if(mSelected >= mOptions.size()) {
            return;
        }
        mSelected = index;
        OptionButton.this.setText(mOptions.get(index));
    }

    public void setSelectedValue(String value) {
        for(String s : mOptions) {
            if(s.equals(value)) {
                mSelected = mOptions.indexOf(s);
                OptionButton.this.setText(s);
            }
        }
    }

    public String getCurrentValue() {
        if(mSelected >= mOptions.size()) {
            return "";
        }
        return mOptions.get(mSelected);
    }

    public int getCurrentIndex() {
        return mSelected;
    }

    /**
     *
     * @param options
     */
    public void setOptions(ArrayList<String> options) {
        mOptions = options;
        mSelected = 0;
        if(mOptions != null && mOptions.size() > 0) {
            OptionButton.this.setText(mOptions.get(0));
        }
    }

    @Override
    public void onClick(View v) {

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which >= mOptions.size() || which < 0) {
                    return;
                }
                mSelected = which;
                OptionButton.this.setText(mOptions.get(which));
                if(mCallback != null) {
                    mCallback.callback(this, which);
                }
            }
        };

        DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(mContext);
        if(mLabel != null) {
            builder.setTitle(mLabel);
        }
        AlertDialog dialog = builder.setSingleChoiceItems(mOptions.toArray(new String[mOptions.size()]), mSelected, onClickListener).create();
        dialog.show();
    }
}

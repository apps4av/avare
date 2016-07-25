/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adapters;


import android.content.Context;
import android.graphics.Color;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ds.avare.R;

import java.util.regex.Pattern;

/**
 * @author zkhan
 *
 */
public class TypeValueAdapter extends ArrayAdapter<String> {

    private Context  mContext;
    private String[] mType;
    private String[] mValue;
    private boolean mNight;

    private int[]    mCategory;

	public static final int CATEGORY_ANY = 0;
	public static final int CATEGORY_LABEL = 1;
	public static final int CATEGORY_FREQUENCY = 2;
	public static final int CATEGORY_RUNWAYS = 3;
	public static final int CATEGORY_FUEL = 4;

    /**
     */
    public TypeValueAdapter(Context context, String[] type, String value[], int category[], boolean night) {
        super(context, R.layout.typevalue, type);
        mContext = context;
        mType = type;
        mValue = value;
		mCategory = category;
        mNight = night;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = convertView;

		if (null == rowView) {
			rowView = inflater.inflate(R.layout.typevalue, parent, false);
		}
		TextView textView = (TextView) rowView
				.findViewById(R.id.typevalue_type);

        // Set colors in list to separate out frequencies, fuel, etc.
		switch(mCategory[position]) {
			case CATEGORY_LABEL:
                if(mNight) {
                    textView.setTextColor(Color.RED);
                }
                else {
                    textView.setTextColor(0xFFAA0000);
                }
				break;
			case CATEGORY_FREQUENCY:
                if(mNight) {
                    textView.setTextColor(0xFF545AA7);
                }
                else {
                    textView.setTextColor(0xFF0000AA);
                }
				break;
			case CATEGORY_RUNWAYS:
                if(mNight) {
                    textView.setTextColor(Color.MAGENTA);
                }
                else {
                    textView.setTextColor(0xFFAA00AA);
                }
				break;
			case CATEGORY_FUEL:
                if(mNight) {
                    textView.setTextColor(Color.GREEN);
                }
                else {
                    textView.setTextColor(0xFF00AA00);
                }
				break;
			default:
			case CATEGORY_ANY:
                if(mNight) {
                    textView.setTextColor(0xFFAAAAAA);
                }
                else {
                    textView.setTextColor(Color.BLACK);
                }
				break;
		}
		textView.setText(mType[position]);
		textView = (TextView) rowView.findViewById(R.id.typevalue_value);
		textView.setText(mValue[position]);
		/*
		 * This will make links out of certain text patterns eg. Phone numbers,
		 * email, web address
		 */
		/*
		 * The next three lines are a more custom version of the commented out
		 * default linkify above. It was catching things like lat/lon, times etc
		 */
		
		String phoneRegex = "\\b(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]‌​)\\s*)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-‌​9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})\\b";
		Pattern phoneMatcher = Pattern.compile(phoneRegex);
		Linkify.addLinks(textView, phoneMatcher, "tel:");

		return rowView;
	}

}

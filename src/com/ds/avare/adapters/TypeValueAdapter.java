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


import java.util.regex.Pattern;

import com.ds.avare.R;
import com.ds.avare.R.id;
import com.ds.avare.R.layout;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * @author zkhan
 *
 */
public class TypeValueAdapter extends ArrayAdapter<String> {

    private Context  mContext;
    private String[] mType;
    private String[] mValue;
    
    
    /**
     * @param context
     * @param textViewResourceId
     */
    public TypeValueAdapter(Context context, String[] type, String value[]) {
        super(context, R.layout.typevalue, type);
        mContext = context;
        mType = type;
        mValue = value;
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

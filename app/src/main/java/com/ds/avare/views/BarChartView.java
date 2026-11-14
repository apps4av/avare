/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.Locale;

/** Horizontal-scrolling bar chart used by the logbook dashboard. */
public class BarChartView extends View {

    private String[] mLabels = new String[0];
    private float[] mValues = new float[0];
    private int mColor = 0xFF1565C0;
    private final Paint mBar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGrid = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context) {
        super(context);
        mText.setColor(0xFF616161);
        mText.setTextSize(26f);
        mText.setTextAlign(Paint.Align.CENTER);
        mGrid.setColor(0x22000000);
        mGrid.setStrokeWidth(1f);
    }

    public void setData(String[] labels, float[] values, int color) {
        mLabels = labels == null ? new String[0] : labels;
        mValues = values == null ? new float[0] : values;
        mColor = color;
        mBar.setColor(color);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = Math.max(mLabels.length * 140, MeasureSpec.getSize(widthMeasureSpec));
        setMeasuredDimension(width, dp(200));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mValues.length == 0) {
            return;
        }
        float max = 0;
        for (float v : mValues) {
            if (v > max) {
                max = v;
            }
        }
        if (max <= 0) {
            return;
        }
        max *= 1.2f;
        float top = 16f;
        float bottom = getHeight() - 70f;
        float slot = getWidth() / (float) mValues.length;
        for (int i = 0; i <= 3; i++) {
            float y = bottom - (bottom - top) * i / 3f;
            canvas.drawLine(0, y, getWidth(), y, mGrid);
        }
        for (int i = 0; i < mValues.length; i++) {
            float h = (bottom - top) * (mValues[i] / max);
            float left = i * slot + slot * 0.25f;
            float right = (i + 1) * slot - slot * 0.25f;
            canvas.drawRect(left, bottom - h, right, bottom, mBar);
            String value = mValues[i] == Math.rint(mValues[i])
                    ? String.valueOf(Math.round(mValues[i]))
                    : String.format(Locale.US, "%.1f", mValues[i]);
            canvas.drawText(value, (left + right) / 2f, bottom - h - 8f, mText);
            String label = mLabels[i] == null ? "" : mLabels[i];
            if (label.length() > 10) {
                label = label.substring(0, 9) + "…";
            }
            canvas.drawText(label, (left + right) / 2f, bottom + 36f, mText);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

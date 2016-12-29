/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.ds.avare.R;
import com.ds.avare.gps.ExtendedGpsParams;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.instruments.VNAV;
import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 * Draws memory use 
 */
public class PfdView extends View {

    /*
     * PFD View.
     *
     * Its easier to draw if cartesian coordinates are used in percentage. Use x() and y() functions to draw.
     *
     * All fixed numbers are found through layout percentages.
     *
     */
    private Paint            mPaint;
    private Context          mContext;
    private float            mDpi;
    private float            mPitch;
    private float            mRoll;
    private float            mInclinometer;
    private Path             mPath;
    private float            mWidth;
    private float            mHeight;
    private float            mSpeed;
    private float            mSpeedChange;
    private float            mAltitude;
    private float            mAltitudeChange;
    private float            mVsi;
    private RectF            mRectf;
    private float            mYaw;
    private float            mTurnTrend;
    private float            mTo;
    private float            mCdi;
    private float            mVdi;


    private static final float SPEED_TEN = 1.5f;
    private static final float ALTITUDE_THOUSAND = 1.5f;
    private static final float VSI_FIVE = 1.5f;
    private static final float PITCH_DEGREE = 4f;
    private static final float VDI_DEGREE = 30f;

    /**
     * PAn the whole drawing up with this fraction of screen to utilize maximum screen
     */
    private static final float Y_PAN = 0.125f;

    /**
     *
     */
    private void setup(Context context) {
        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setTextSize(getResources().getDimension(R.dimen.pfdTextSize));
        mDpi = Helper.getDpiToPix(context);
        mPitch = 0;
        mSpeed = 0;
        mSpeedChange = 0;
        mAltitude = 0;
        mAltitudeChange = 0;
        mVsi = 0;
        mYaw = 0;
        mTurnTrend = 0;
        mInclinometer = 0;
        mCdi = 0;
        mVdi = 3;
        mPath = new Path();
    }


    /**
     *
     * @param context
     */
    public PfdView(Context context) {
        super(context);
        setup(context);
    }

    /**
     *
     * @param context
     * @param aset
     */
    public PfdView(Context context, AttributeSet aset) {
        super(context, aset);
        setup(context);
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public PfdView(Context context, AttributeSet aset, int arg) {
        super(context, aset, arg);
        setup(context);
    }

    /**
     * Xform 0,0 in center, and (100, 100) on top right, and (-100, -100) on bottom left
     * @param yval
     * @return
     */
    private float y(float yval) {
        float yperc = mHeight / 200f;
        return mHeight / 2f - yval * yperc - mHeight * Y_PAN;
    }

    /**
     * Xform 0,0 in center, and (100, 100) on top right, and (-100, -100) on bottom left
     * @param xval
     * @return
     */
    private float x(float xval) {
        float xperc = mWidth / 200f;
        return mWidth / 2f + xval * xperc;
    }


    @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh) {
        mWidth = (float)w;
        mHeight = (float)h;
        if(h < w) {
            return;
        }


        // prealloc rectangles for clip
        mRectf = new RectF(0, 0, 0, 0);

    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {
        /*
         * Now draw the target cross hair
         */

        mPaint.setStrokeWidth(2 * mDpi);

        /**
         * Cross on error
         */
        if(mWidth > mHeight) {
            mPaint.setColor(Color.RED);
            canvas.drawLine(x(-100), y(-100), x(100), y(100), mPaint);
            canvas.drawLine(x(-100), y(100), x(100), y(-100), mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawText(mContext.getString(R.string.OnlyPortrait), x(-90), y(0), mPaint);
            return;
        }

        /*
         * draw pitch / roll
         */
        canvas.save();


        Paint.Style style = mPaint.getStyle();

        canvas.rotate(mRoll, x(0), y(0));
        canvas.translate(0, y(0) - y(mPitch * PITCH_DEGREE));

        mPaint.setColor(Color.BLUE);
        canvas.drawRect(x(-400), y(PITCH_DEGREE * 90), x(400), y(0), mPaint);

        mPaint.setColor(0xFF826644);
        canvas.drawRect(x(-400), y(0), x(400), y(-PITCH_DEGREE * 90), mPaint);

        //center attitude degrees
        mPaint.setColor(Color.WHITE);
        canvas.drawLine(x(-150), y(0), x(150), y(0), mPaint);

        // degree lines
        float degrees = ((float)Math.round((mPitch + 1.25) / 2.5f) * 2.5f);
        float offset = (mPaint.descent() + mPaint.ascent()) / 2;

        for(float d = -12.5f; d <= 12.5f; d += 2.5) {
            float inc = degrees + d;
            if (0 == inc % 10f) {
                canvas.drawLine(x(-12), y(inc * PITCH_DEGREE), x(12), y(inc * PITCH_DEGREE), mPaint);
                if(0 == inc) {
                    continue;
                }
                canvas.drawText(" " + Math.round(Math.abs(inc)), x(12), y(inc * PITCH_DEGREE) - offset, mPaint);
            }
            if (0 == inc % 5f) {
                canvas.drawLine(x(-4), y(inc * PITCH_DEGREE), x(4), y(inc * PITCH_DEGREE), mPaint);
            }
            else {
                canvas.drawLine(x(-2), y(inc * PITCH_DEGREE), x(2), y(inc * PITCH_DEGREE), mPaint);
            }
        }


        canvas.restore();
        canvas.save();

        canvas.rotate(mRoll, x(0), y(0));

        //draw roll arc
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        float r = y(0) - y(70);
        mRectf.set(x(0) - r, y(0) - r, x(0) + r, y(0) + r);
        canvas.drawArc(mRectf, 210, 120, false, mPaint);
        mPaint.setStyle(style);

        // degree ticks
        //60
        canvas.rotate(-60, x(0), y(0));
        canvas.drawLine(x(0), y(75), x(0), y(70), mPaint);

        //45
        canvas.rotate(15, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        //30
        canvas.rotate(15, x(0), y(0));
        canvas.drawLine(x(0), y(75), x(0), y(70), mPaint);

        //20
        canvas.rotate(10, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        //10
        canvas.rotate(10, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        // center arrow
        canvas.rotate(10, x(0), y(0));
        mPath.reset();
        mPath.moveTo(x(-7), y(75));
        mPath.lineTo(x(0), y(70));
        mPath.lineTo(x(7), y(75));
        canvas.drawPath(mPath, mPaint);

        //10
        canvas.rotate(10, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        //20
        canvas.rotate(10, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        //30
        canvas.rotate(10, x(0), y(0));
        canvas.drawLine(x(0), y(75), x(0), y(70), mPaint);

        //45
        canvas.rotate(15, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        //60
        canvas.rotate(15, x(0), y(0));
        canvas.drawLine(x(0), y(73), x(0), y(70), mPaint);

        canvas.rotate(-60, x(0), y(0));

        canvas.restore();

        //bank arrow
        mPath.reset();
        mPath.moveTo(x(7), y(65));
        mPath.lineTo(x(0), y(70));
        mPath.lineTo(x(-7), y(65));
        canvas.drawPath(mPath, mPaint);
        // inclinometer, displace +-20 of screen from +- 10 degrees
        canvas.drawRect(x(-7 + mInclinometer * 2), y(64), x(7 + mInclinometer * 2), y(62), mPaint);


        // draw airplane wings
        mPaint.setColor(Color.YELLOW);
        canvas.drawRect(x(-45), y(1), x(-20), y(-1), mPaint);
        canvas.drawRect(x(20), y(1), x(45), y(-1), mPaint);

        // draw airplane triangle
        mPath.reset();
        mPath.moveTo(x(0) , y(0));
        mPath.lineTo(x(-15), y(-10));
        mPath.lineTo(x(0), y(-5));
        mPath.lineTo(x(15), y(-10));
        canvas.drawPath(mPath, mPaint);

        /**
         * Speed tape
         */
        mPaint.setColor(Color.WHITE);
        canvas.save();
        canvas.clipRect(x(-80), y(35), x(-50), y(-35));
        canvas.translate(0, y(0) - y(mSpeed * SPEED_TEN));

        // lines, just draw + - 30
        float tens = Math.round(mSpeed / 10f) * 10f;
        for(float c = tens - 30; c <= tens + 30; c += 10) {
            if(c < 0) {
                continue; // no negative speed
            }
            canvas.drawLine(x(-50), y(c * SPEED_TEN), x(-55), y(c * SPEED_TEN), mPaint);
            canvas.drawText("" + Math.round(Math.abs(c)), x(-75), y(c * SPEED_TEN), mPaint);
        }
        for(float c = tens - 30; c <= tens + 30; c += 5) {
            if(c < 0) {
                continue; // no negative speed
            }
            canvas.drawLine(x(-50), y(c * SPEED_TEN), x(-53), y(c * SPEED_TEN), mPaint);
        }

        canvas.restore();

        // trend
        mPaint.setColor(Color.MAGENTA);
        if(mSpeedChange > 0) {
            canvas.drawRect(x(-53), y(mSpeedChange * SPEED_TEN), x(-50), y(0), mPaint);
        }
        else {
            canvas.drawRect(x(-53), y(0), x(-50), y(mSpeedChange * SPEED_TEN), mPaint);
        }

        // value
        mPaint.setColor(Color.BLACK);

        mPath.reset();
        mPath.moveTo(x(-80), y(3));
        mPath.lineTo(x(-55), y(3));
        mPath.lineTo(x(-50), y(0));
        mPath.lineTo(x(-55), y(-3));
        mPath.lineTo(x(-80), y(-3));
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(Color.WHITE);
        canvas.drawText("" + Math.round(Math.abs(mSpeed)), x(-75), y(-2), mPaint);

        // boundary
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(-80), y(35), x(-50), y(-35), mPaint);
        mPaint.setStyle(style);

        /**
         * Altitude tape
         */
        mPaint.setColor(Color.WHITE);
        canvas.save();
        canvas.clipRect(x(35), y(35), x(85), y(-35));
        canvas.translate(0, y(0) - y(mAltitude * ALTITUDE_THOUSAND / 10f)); // alt is dealt in 10's of feet

        // lines, just draw + and - 300 ft.
        float hundreds = Math.round(mAltitude / 100f) * 100f;
        for(float c = (hundreds - 300) / 10f; c <= (hundreds + 300) / 10f; c += 10) {
            canvas.drawLine(x(50), y(c * ALTITUDE_THOUSAND), x(55), y(c * ALTITUDE_THOUSAND), mPaint);
            canvas.drawText(Math.round(c) + "0", x(55), y(c * ALTITUDE_THOUSAND), mPaint);
        }
        for(float c = (hundreds - 300) / 10f; c <= (hundreds + 300) / 10f; c += 2) {
            canvas.drawLine(x(50), y(c * ALTITUDE_THOUSAND), x(53), y(c * ALTITUDE_THOUSAND), mPaint);
        }
        canvas.restore();

        // trend
        mPaint.setColor(Color.MAGENTA);
        if(mAltitudeChange > 0) {
            canvas.drawRect(x(50), y(mAltitudeChange * ALTITUDE_THOUSAND / 10f), x(52), y(0), mPaint);
        }
        else {
            canvas.drawRect(x(50), y(0), x(52), y(mAltitudeChange * ALTITUDE_THOUSAND / 10f), mPaint);
        }

        // value
        mPaint.setColor(Color.BLACK);

        mPath.reset();
        mPath.moveTo(x(50), y(0));
        mPath.lineTo(x(55), y(3));
        mPath.lineTo(x(85), y(3));
        mPath.lineTo(x(85), y(-3));
        mPath.lineTo(x(55), y(-3));
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(Color.WHITE);
        canvas.drawText(Math.round(mAltitude) + "", x(55), y(-2), mPaint);

        // boundary
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(50), y(35), x(85), y(-35), mPaint);
        mPaint.setStyle(style);


        /**
         * VSI tape
         */


        mPaint.setColor(Color.WHITE);

        //lines
        canvas.drawLine(x(90), y(5   * VSI_FIVE), x(85), y(5   * VSI_FIVE), mPaint);
        canvas.drawLine(x(95), y(10  * VSI_FIVE), x(85), y(10  * VSI_FIVE), mPaint);
        canvas.drawLine(x(90), y(15  * VSI_FIVE), x(85), y(15  * VSI_FIVE), mPaint);
        canvas.drawLine(x(90), y(-5  * VSI_FIVE), x(85), y(-5  * VSI_FIVE), mPaint);
        canvas.drawLine(x(95), y(-10 * VSI_FIVE), x(85), y(-10 * VSI_FIVE), mPaint);
        canvas.drawLine(x(90), y(-15 * VSI_FIVE), x(85), y(-15 * VSI_FIVE), mPaint);



        // boundary
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPath.reset();
        mPath.moveTo(x(85), y(20 * VSI_FIVE));
        mPath.lineTo(x(98), y(20 * VSI_FIVE));
        mPath.lineTo(x(98), y(5 * VSI_FIVE));
        mPath.lineTo(x(85), y(0 * VSI_FIVE));
        mPath.lineTo(x(98), y(-5 * VSI_FIVE));
        mPath.lineTo(x(98), y(-20 * VSI_FIVE));
        mPath.lineTo(x(85), y(-20 * VSI_FIVE));
        canvas.drawPath(mPath, mPaint);
        mPaint.setStyle(style);


        // text on VSI
        canvas.drawText("1", x(90), y(11 * VSI_FIVE), mPaint);
        canvas.drawText("2", x(90), y(21 * VSI_FIVE), mPaint);
        canvas.drawText("1", x(90), y(-9 * VSI_FIVE), mPaint);
        canvas.drawText("2", x(90), y(-19 * VSI_FIVE), mPaint);


        // value
        mPaint.setColor(Color.BLACK);

        float offs = mVsi / 100f * VSI_FIVE;
        mPath.reset();
        mPath.moveTo(x(85), y(0 * VSI_FIVE + offs));
        mPath.lineTo(x(92), y(2.5f * VSI_FIVE + offs));
        mPath.lineTo(x(98), y(2.5f * VSI_FIVE + offs));
        mPath.lineTo(x(98), y(-2.5f * VSI_FIVE + offs));
        mPath.lineTo(x(92), y(-2.5f * VSI_FIVE + offs));
        mPath.lineTo(x(85), y(0 * VSI_FIVE + offs));
        canvas.drawPath(mPath, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawPath(mPath, mPaint);
        mPaint.setStyle(style);

        // VSI hundreds
        int hvsi = (int)((Math.abs(mVsi) %1000f) / 100f);
        canvas.drawText(hvsi + "", x(90), y(0 * VSI_FIVE + offs) - (mPaint.descent() + mPaint.ascent()) / 2,
                mPaint);


        /**
         * Compass
         */

        // arrow
        mPaint.setColor(Color.WHITE);
        mPath.reset();
        mPath.moveTo(x(-5), y(-60));
        mPath.lineTo(x(0), y(-65));
        mPath.lineTo(x(5), y(-60));
        canvas.drawPath(mPath, mPaint);

        canvas.save();

        // half standrad rate, 9 degrees in 6 seconds
        canvas.rotate(-18, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-65), mPaint);

        // standrad rate, 18 degrees in 6 seconds
        canvas.rotate(9, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-65), mPaint);

        // standrad rate, 18 degrees in 6 seconds
        canvas.rotate(18, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-65), mPaint);

        // half standrad rate, 9 degrees in 6 seconds
        canvas.rotate(9, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-65), mPaint);

        canvas.restore();

        //draw 12, 30 degree marks.
        canvas.save();

        canvas.rotate(-mYaw, x(0), y(-95));

        offset = (mPaint.descent() + mPaint.ascent()) / 2;

        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("N", x(0) + offset, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("3", x(0) + offset, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("6", x(0) + offset, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("E", x(0) + offset, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("12", x(0) + offset * 2, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("15", x(0) + offset * 2, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("S", x(0) + offset, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("21", x(0) + offset * 2, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("24", x(0) + offset * 2, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("W", x(0) + offset, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("30", x(0) + offset * 2, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-70), mPaint);
        canvas.drawText("33", x(0) + offset * 2, y(-75), mPaint);
        canvas.rotate(30, x(0), y(-95));
        
        canvas.restore();

        // airplane
        mPaint.setColor(Color.WHITE);
        canvas.drawLine(x(0), y(-105), x(0), y(-85), mPaint);

        //draw heading
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(x(-13), y(-50), x(13), y(-58), mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(-13), y(-50), x(13), y(-58), mPaint);
        mPaint.setStyle(style);
        canvas.drawText(Math.round((mYaw + 360) % 360) + "\u00B0", x(-10), y(-56), mPaint);


        // draw rate of turn arc.
        mPaint.setStrokeWidth(4 * mDpi);
        mPaint.setColor(Color.MAGENTA);
        mPaint.setStyle(Paint.Style.STROKE);
        r = y(-95) - y(-65);
        mRectf.set(x(0) - r, y(-95) - r, x(0) + r, y(-95) + r);
        canvas.drawArc(mRectf, -90, mTurnTrend, false, mPaint);
        mPaint.setStyle(style);

        // CDI

        canvas.save();
        canvas.rotate((mTo - mYaw + 360) % 360, x(0), y(-95));
        //draw dots for displacement.
        mPaint.setColor(Color.WHITE);

        for(float i = 0; i < 25; i += 5) {
            canvas.drawCircle(x(-5 - i), y(-95), y(0) - y(1), mPaint);
            canvas.drawCircle(x( 5 + i), y(-95), y(0) - y(1), mPaint);
        }
        mPaint.setColor(Color.MAGENTA);
        canvas.drawLine(x(0), y(-115), x(0), y(-105), mPaint); // three to break up CDI
        canvas.drawLine(x(0), y(-85), x(0), y(-80), mPaint);
        mPath.reset();
        mPath.moveTo(x(0), y(-75));
        mPath.lineTo(x(-5), y(-80));
        mPath.lineTo(x(5), y(-80));
        canvas.drawPath(mPath, mPaint);
        mPaint.setStrokeWidth(2 * mDpi);
        canvas.drawLine(x(mCdi * 5), y(-105), x(mCdi * 5), y(-85), mPaint);
        canvas.restore();


        //draw course
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(x(45), y(-70), x(71), y(-78), mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(45), y(-70), x(71), y(-78), mPaint);
        mPaint.setStyle(style);
        canvas.drawText(Math.round((mTo + 360) % 360) + "\u00B0", x(48), y(-76), mPaint);

        // Warning.
        mPaint.setColor(Color.YELLOW);
        canvas.drawText(mContext.getString(R.string.SeeHelp), x(-95), y(-45), mPaint);

        /*
         * draw VDI
         */
        // boundary
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(45), y(25), x(50), y(-25), mPaint);
        canvas.drawLine(x(45), y(0), x(50), y(0), mPaint);
        mPaint.setStyle(style);

        //draw bars in 10s
        canvas.drawCircle(x(47.5f), y((float)VNAV.BAR_DEGREES * 4 * VDI_DEGREE), y(0) - y(1), mPaint);
        canvas.drawCircle(x(47.5f), y((float)VNAV.BAR_DEGREES * 2 * VDI_DEGREE), y(0) - y(1), mPaint);
        canvas.drawCircle(x(47.5f), y(-(float)VNAV.BAR_DEGREES * 2 * VDI_DEGREE), y(0) - y(1), mPaint);
        canvas.drawCircle(x(47.5f), y(-(float)VNAV.BAR_DEGREES * 4 * VDI_DEGREE), y(0) - y(1), mPaint);

        //draw VDI circle
        if(mVdi >= VNAV.HI) {
            mPaint.setColor(Color.MAGENTA);
        }
        else if(mVdi <= VNAV.LOW) {
            mPaint.setColor(Color.MAGENTA);
        }
        else {
            mPaint.setColor(Color.CYAN);
        }
        float val = 3f - mVdi;
        canvas.drawCircle(x(47.5f), y(val * VDI_DEGREE), y(0) - y(1), mPaint);
        mPaint.setColor(Color.WHITE);

        canvas.drawText("G", x(45), y(26), mPaint);

    }

    public void setPitch(float pitch) {
        mPitch = pitch;
    }

    public void setRoll(float roll) {
        mRoll = roll;
    }

    public void setYaw(float yaw) {
        //mYaw = yaw; //unstable, use GPS track instead
    }

    public void setAcceleration(double acceleration) {
        double a = acceleration;
        if(a > 9.8) {
            a = 9.8;
        }
        if(a < -9.8) {
            a = -9.8;
        }
        // mgsin(0) pendulum displacement, find 0
        a = a / 9.8f;
        double angle = Math.toDegrees(Math.asin(a));
        // limit angle +-10 degrees, judging from actual instrument circle
        if(angle > 10) {
            angle = 10;
        }
        if(angle < -10) {
            angle = -10;
        }

        mInclinometer = (float)angle;
    }

    public void setParams(GpsParams params, ExtendedGpsParams eparams, double bearing, double cdi, double vdi) {
        /**
         * Assign and limit numbers
         */
        mSpeed = (float)params.getSpeed();
        if(mSpeed > 500) {
            mSpeed = 500;
        }
        mSpeedChange = (float)eparams.getDiffSpeedTrend();
        if(mSpeedChange > 25) {
            mSpeedChange = 25;
        }
        if(mSpeedChange < -25) {
            mSpeedChange = -25;
        }

        mVsi = (float)eparams.getAltitudeRateOfChange() * 60; //for per minute calculations
        if(mVsi > 2000) {
            mVsi = 2000;
        }
        if(mVsi < -2000) {
            mVsi = -2000;
        }

        mAltitude = (float)params.getAltitude();
        if(mAltitude > 50000) {
            mAltitude = 50000;
        }
        if(mAltitude < -200) {
            mAltitude = -200;
        }

        mAltitudeChange = (float)eparams.getDiffAltitudeTrend();
        if(mAltitudeChange > 200) {
            mAltitudeChange = 200;
        }
        if(mAltitudeChange < -200) {
            mAltitudeChange = -200;
        }

        mTurnTrend = (float)eparams.getDiffBearingTrend();
        if(mTurnTrend > 30) {
            mTurnTrend = 30;
        }
        if(mTurnTrend < -30) {
            mTurnTrend = -30;
        }


        // ideally derive from gyro
        mYaw = (float)(params.getBearing() + params.getDeclinition() + 360) % 360f;

        mTo = (float)(bearing + params.getDeclinition() + 360) % 360f;

        mCdi = (float)cdi; // CDI is in miles, each tick is 1 miles enroute
        if(mCdi > 5) {
            mCdi = 5;
        }
        if(mCdi < -5) {
            mCdi = -5;
        }

        // degrees
        mVdi = (float)vdi;
        if(mVdi > 3.8f) {
            mVdi = 3.8f;
        }
        if(mVdi < 2.2f) {
            mVdi = 2.2f;
        }
    }

}

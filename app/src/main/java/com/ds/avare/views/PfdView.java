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
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.ds.avare.IHelperService;
import com.ds.avare.R;
import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.ExtendedGpsParams;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.instruments.VNAV;
import com.ds.avare.position.PixelCoordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.utils.BitmapHolder;
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
    private boolean          mIsYawFromMagneticSensor;
    private float            mGroundTrack;
    private float            mTurnTrend;
    private float            mTo;
    private float            mCdi;
    private String           mDst;
    private String           mDistance;
    private float            mVdi;
    private float            mPressureAltitude;
    private double           mLat, mLon;
    private SparseArray<Traffic>     mTraffic;
    private BitmapHolder     mAirplaneBitmap;
    private static final ColorFilter colorFilter = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);


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
        mGroundTrack = 0;
        mTurnTrend = 0;
        mInclinometer = 0;
        mCdi = 0;
        mVdi = 3;
        mPath = new Path();
        mAirplaneBitmap = new BitmapHolder(context, R.drawable.plane);
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

        final float scaledTextSize = mPaint.getTextSize()*0.80f,
                normalTextSize = mPaint.getTextSize(),
                scaledUpTextSize = mPaint.getTextSize()*1.15f;
        
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
        
        // limit angle +-10 degrees, judging from actual instrument circle
        float angle = (mInclinometer > 10) ? 10 : (mInclinometer < -10) ? -10 : mInclinometer;
        canvas.drawRect(x(-7 + angle * 2), y(64), x(7 + angle * 2), y(62), mPaint);


        // draw airplane wings
        mPaint.setColor(Color.YELLOW);                          // wing
        canvas.drawRect(x(-45), y(1), x(-20), y(0), mPaint); 
        canvas.drawRect(x(20), y(1), x(45), y(0), mPaint);
        mPaint.setColor(darker(Color.YELLOW, .7f));             // shade
        canvas.drawRect(x(-45), y(0), x(-20), y(-1), mPaint); 
        canvas.drawRect(x(20), y(0), x(45), y(-1), mPaint);

        // draw airplane triangle
        mPaint.setColor(Color.YELLOW);                          // top
        mPath.reset();
        mPath.moveTo(x(0) , y(0));  
        mPath.lineTo(x(-15), y(-10));
        mPath.lineTo(x(0), y(-3));  
        mPath.lineTo(x(15), y(-10));
        canvas.drawPath(mPath, mPaint);
        mPath.reset();
        mPaint.setColor(darker(Color.YELLOW, .7f));             // bottom
        mPath.moveTo(x(0) , y(-3));
        mPath.lineTo(x(-15), y(-10));
        mPath.lineTo(x(0), y(-6));
        mPath.lineTo(x(15), y(-10));
        canvas.drawPath(mPath, mPaint);

        /**
         * Speed tape
         */
        mPaint.setColor(Color.LTGRAY);
        canvas.save();
        int right = mSpeed < 100 ? -60 : -55; // for v>1000 make space for 4 digits 
        canvas.clipRect(x(-80), y(35), x(right), y(-35));
        canvas.translate(0, y(0) - y(mSpeed * SPEED_TEN));

        // lines, just draw + - 30
        float tens = Math.round(mSpeed / 10f) * 10f;
        for(float c = tens - 30; c <= tens + 30; c += 10) {
            if(c < 0) {
                continue; // no negative speed
            }
            canvas.drawLine(x(right), y(c * SPEED_TEN), x(right-5), y(c * SPEED_TEN), mPaint);
            int speed = Math.round(Math.abs(c));
            canvas.drawText(spaces(4 - numDigits(speed)) + speed, x(right-25), y(c * SPEED_TEN), mPaint);
        }
        for(float c = tens - 30; c <= tens + 30; c += 5) {
            if(c < 0) {
                continue; // no negative speed
            }
            canvas.drawLine(x(right), y(c * SPEED_TEN), x(right-3), y(c * SPEED_TEN), mPaint);
        }

        canvas.restore();

        // trend
        mPaint.setColor(Color.MAGENTA);
        if(mSpeedChange > 0) {
            canvas.drawRect(x(-53), y(mSpeedChange * SPEED_TEN), x(right), y(0), mPaint);
        }
        else {
            canvas.drawRect(x(-53), y(0), x(right), y(mSpeedChange * SPEED_TEN), mPaint);
        }

        // value
        mPaint.setColor(Color.BLACK);

        mPath.reset();
        mPath.moveTo(x(-80), y(3));
        mPath.lineTo(x(right-5), y(3));
        mPath.lineTo(x(right), y(0));
        mPath.lineTo(x(right-5), y(-3));
        mPath.lineTo(x(-80), y(-3));
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(scaledUpTextSize);
        final int speed = Math.round(Math.abs(mSpeed));
        canvas.drawText(spaces(4 - numDigits(speed)) + speed, x(right-28), y(-2), mPaint);
        mPaint.setTextSize(normalTextSize);

        // boundary
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(-80), y(35), x(right), y(-35), mPaint);
        mPaint.setStyle(style);

        /**
         * Altitude tape
         */
        mPaint.setColor(Color.LTGRAY);
        canvas.save();
        canvas.clipRect(x(35), y(35), x(85), y(-35));
        canvas.translate(0, y(0) - y(mAltitude * ALTITUDE_THOUSAND / 10f)); // alt is dealt in 10's of feet

        // lines, just draw + and - 300 ft.
        final float hundreds = Math.round(mAltitude / 100f) * 100f;
        for(float c = (hundreds - 300) / 10f; c <= (hundreds + 300) / 10f; c += 10) {
            float yOffset = y(c * ALTITUDE_THOUSAND);
            canvas.drawLine(x(50), yOffset, x(55), yOffset, mPaint);
            // altitude numbers; thousands and half thousands in larger font
            yOffset = yOffset + normalTextSize/4; 
            String altToPrint = Math.round(c) + "0";
            if (altToPrint.length() > 3) {
                if (altToPrint.endsWith("500") || altToPrint.endsWith("000")) {
                    canvas.drawText(altToPrint, x(55), yOffset, mPaint);
                } else {
                    String thousandsDigits = altToPrint.substring(0, altToPrint.length() - 3);
                    String hundredsDigits  = altToPrint.substring(altToPrint.length() - 3);
                    canvas.drawText(thousandsDigits, x(55), yOffset, mPaint);
                    mPaint.setTextSize(scaledTextSize);
                    canvas.drawText(hundredsDigits, x(55 + 5 * thousandsDigits.length()), yOffset, mPaint);
                    mPaint.setTextSize(normalTextSize);
                }
            } else {
                mPaint.setTextSize(scaledTextSize);
                canvas.drawText(altToPrint, x(55), yOffset, mPaint);
                mPaint.setTextSize(normalTextSize);
            }
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
        mPaint.setTextSize(scaledUpTextSize);
        canvas.drawText(Math.round(mAltitude) + "", x(55), y(-2), mPaint);
        mPaint.setTextSize(normalTextSize);

        // boundary
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(50), y(35), x(85), y(-35), mPaint);
        mPaint.setStyle(style);

        // pressure altitude
        if (mPressureAltitude > 0) {
            mPaint.setColor(Color.BLACK);
            canvas.drawRect(x(50), y(-35), x(85), y(-45), mPaint);
            mPaint.setColor(Color.BLUE);
            int pa = (int)altitudeToPressure(mPressureAltitude);
            canvas.drawText(Integer.toString(pa), x(54), y(-42), mPaint);
        }

        /**
         * VSI tape
         */


        mPaint.setColor(Color.LTGRAY);

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
        mPaint.setColor(Color.LTGRAY);
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
        mPath.moveTo(x(-2), y(-60));
        mPath.lineTo(x(0), y(-65));
        mPath.lineTo(x(2), y(-60));
        canvas.drawPath(mPath, mPaint);

        canvas.save();

        mPaint.setColor(Color.LTGRAY);
        
        // half standrad rate, 9 degrees in 6 seconds
        canvas.rotate(-18, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-64), mPaint);

        // standrad rate, 18 degrees in 6 seconds
        canvas.rotate(9, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-64), mPaint);

        // standrad rate, 18 degrees in 6 seconds
        canvas.rotate(18, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-64), mPaint);

        // half standrad rate, 9 degrees in 6 seconds
        canvas.rotate(9, x(0), y(-95));
        canvas.drawLine(x(0), y(-60), x(0), y(-64), mPaint);

        canvas.restore();
        
        // 45, 90, 135 deg marks on compass outer edge
        canvas.save();
        mPaint.setColor(Color.WHITE);
        
        canvas.rotate(-135, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);
        
        canvas.rotate(45, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);

        canvas.rotate(45, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);

        canvas.rotate(45, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);

        canvas.rotate(45, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);

        canvas.rotate(45, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);

        canvas.rotate(45, x(0), y(-95));
        canvas.drawLine(x(0), y(-61), x(0), y(-64), mPaint);

        canvas.restore();

        //draw 12, 30 degree marks.
        canvas.save();
        mPaint.setColor(Color.LTGRAY);

        canvas.rotate(-mYaw, x(0), y(-95));

        offset = (mPaint.descent() + mPaint.ascent()) / 2;

        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("N", x(0) + offset, y(-73), mPaint);
        mPaint.setTextSize(scaledTextSize);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("3", x(0) + offset, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("6", x(0) + offset, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        mPaint.setTextSize(normalTextSize);
        canvas.drawText("E", x(0) + offset, y(-73), mPaint);
        mPaint.setTextSize(scaledTextSize);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("12", x(0) + offset * 2, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("15", x(0) + offset * 2, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        mPaint.setTextSize(normalTextSize);
        canvas.drawText("S", x(0) + offset, y(-73), mPaint);
        mPaint.setTextSize(scaledTextSize);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("21", x(0) + offset * 2, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("24", x(0) + offset * 2, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        mPaint.setTextSize(normalTextSize);
        canvas.drawText("W", x(0) + offset, y(-73), mPaint);
        mPaint.setTextSize(scaledTextSize);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("30", x(0) + offset * 2, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.drawText("33", x(0) + offset * 2, y(-72), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        canvas.rotate(10, x(0), y(-95));
        canvas.drawLine(x(0), y(-65), x(0), y(-68), mPaint);
        mPaint.setTextSize(normalTextSize);

        canvas.restore();

        // current ground track indicator, indicated by a diamond
        canvas.save();
        mPaint.setColor(Color.MAGENTA);
        canvas.rotate(-mYaw + mGroundTrack, x(0), y(-95));
        mPaint.setStrokeWidth(2 * mDpi);
        mPaint.setStyle(Paint.Style.FILL);
        mPath.reset();
        mPath.moveTo(x(0), y(-65));
        mPath.lineTo(x(2), y(-67));
        mPath.lineTo(x(0), y(-69));
        mPath.lineTo(x(-2), y(-67));
        mPath.lineTo(x(0), y(-65));
        canvas.drawPath(mPath, mPaint);
        canvas.drawLine(x(0), y(-66), x(0), y(-69), mPaint);
        canvas.restore();

        //draw heading
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(x(-13), y(-50), x(16), y(-58), mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(x(-13), y(-50), x(16), y(-58), mPaint);
        mPaint.setStyle(style);
        mPaint.setTextSize(scaledUpTextSize);
        canvas.drawText(degreesString(mYaw), x(-10), y(-56), mPaint);
        mPaint.setTextSize(normalTextSize);


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
        mPaint.setColor(Color.LTGRAY);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2 * mDpi);
        for(float i = 0; i < 25; i += 5) {
            canvas.drawCircle(x(-5 - i), y(-95), y(0) - y(1), mPaint);
            canvas.drawCircle(x( 5 + i), y(-95), y(0) - y(1), mPaint);
        }
        mPaint.setStyle(style);
        
        mPaint.setColor(Color.MAGENTA);                        // three lines to break up CDI
        canvas.drawLine(x(0), y(-117), x(0), y(-105), mPaint);               // bottom
        canvas.drawLine(x(0), y(-85), x(0), y(-78), mPaint);                 // top
        mPath.reset();                                         
        mPath.moveTo(x(0), y(-73));                            // arrow
        mPath.lineTo(x(-3), y(-78));
        mPath.lineTo(x(3), y(-78));
        canvas.drawPath(mPath, mPaint);
        mPaint.setStrokeWidth(2 * mDpi);
        canvas.drawLine(x(mCdi * 5), y(-105), x(mCdi * 5), y(-85), mPaint); // middle
        canvas.restore();

        //destination name, if set 
        if (mDst != null && !mDst.isEmpty()) {
            
            //draw course
            mPaint.setColor(Color.BLACK);
            int rightCDI = 65; 
            canvas.drawRect(x(rightCDI-26), y(-70), x(rightCDI), y(-78), mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.WHITE);
            canvas.drawRect(x(rightCDI-26), y(-70), x(rightCDI), y(-78), mPaint);
            mPaint.setStyle(style);
            mPaint.setColor(Color.MAGENTA);
            canvas.drawText(degreesString(mTo), x(rightCDI-23), y(-76), mPaint);

            // draw destination id
            int dstTop = 68, dstLeft = -101;
            final String destWithArrow = "\u21D2" /*->*/ + mDst; 
            mPaint.setColor(Color.BLACK);
            float measuredIdTextWidth = mPaint.measureText(destWithArrow);
            canvas.drawRect(x(dstLeft), y(dstTop + 8), x(dstLeft + 4) + measuredIdTextWidth, y(dstTop), mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.WHITE);
            canvas.drawRect(x(dstLeft), y(dstTop + 8), x(dstLeft + 4) + measuredIdTextWidth, y(dstTop), mPaint);
            mPaint.setStyle(style);
            mPaint.setColor(Color.MAGENTA);
            canvas.drawText(destWithArrow, x(dstLeft + 1), y(dstTop + 2), mPaint);

            // draw distance to destination
            int distTop = 68, distLeft = -101 + 4;
            float measuredDistTextWidth = mPaint.measureText(mDistance);
            mPaint.setColor(Color.BLACK);
            canvas.drawRect(x(distLeft) + measuredIdTextWidth, y(distTop + 8), x(distLeft + 4) + measuredIdTextWidth + measuredDistTextWidth, y(distTop), mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.WHITE);
            canvas.drawRect(x(distLeft) + measuredIdTextWidth, y(distTop + 8), x(distLeft + 4) + measuredIdTextWidth + measuredDistTextWidth, y(distTop), mPaint);
            mPaint.setStyle(style);
            mPaint.setColor(Color.MAGENTA);
            canvas.drawText(mDistance, x(distLeft + 2) + measuredIdTextWidth, y(distTop + 2), mPaint);
        }

        // airplane in compass
        mPaint.setColor(Color.WHITE);
        mPaint.setColorFilter(colorFilter);
        float cx = x(0)   - mAirplaneBitmap.getWidth() / 2f;
        float cy = y(-95) - mAirplaneBitmap.getHeight() / 2f;
        canvas.drawBitmap(mAirplaneBitmap.getBitmap(), cx, cy, mPaint);
        mPaint.setColorFilter(null);

        // Warning. Only when not moving
        if (mSpeed==0) {
            mPaint.setColor(Color.YELLOW);
            canvas.drawText(mContext.getString(R.string.SeeHelp), x(-95), y(-45), mPaint);
        }

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
        mPaint.setColor(Color.LTGRAY);
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
        
        mPaint.setColor(Color.LTGRAY);
        canvas.drawText("G", x(45), y(26), mPaint);

        // draw traffic
        if(mTraffic != null) {
            int filterAltitude = 10000; // mPrefs.showAdsbTrafficWithin();
            for (int i = 0; i < mTraffic.size(); ++i) {
                Traffic tr = mTraffic.valueAt(i);
                if(mAltitude < IHelperService.MIN_ALTITUDE) {
                    // filter
                    if(Math.abs(tr.mAltitude - mAltitude) > filterAltitude) {
                        continue;
                    }
                }
                canvas.save();
                canvas.rotate(-mYaw, x(0), y(-95));
                double trafficBearing = Projection.getStaticBearing(mLon, mLat, (double) tr.mLon, (double) tr.mLat);
                double trafficDistance = Math.min(6, Projection.getStaticDistance(mLon, mLat, (double) tr.mLon, (double) tr.mLat));
                canvas.rotate((float) trafficBearing, x(0), y(-95));
                mPaint.setColor(tr.getColorFromAltitude(mAltitude, tr.mAltitude));
                float radius = y(0) - y(1);
                float yOff = y(-95) + ( y(-67) - y(-95) ) * (float)trafficDistance / 6;
                canvas.drawCircle(x(0), yOff, radius, mPaint);
                float speedLength = radius + tr.mHorizVelocity/10 ;
                double xr = x(0) + PixelCoordinate.rotateX(speedLength, tr.mHeading - trafficBearing);
                double yr = yOff + PixelCoordinate.rotateY(speedLength, tr.mHeading - trafficBearing);
                canvas.drawLine(x(0), yOff, (float)xr, (float)yr, mPaint);
                canvas.restore();
            }
        }

    }

    public void setPitch(float pitch) {
        mPitch = pitch;
    }

    public void setRoll(float roll) {
        mRoll = roll;
    }

    public void setYaw(float yaw) { mYaw = yaw; mIsYawFromMagneticSensor = true; }

    public void setSlip(float slip) { mInclinometer = slip; }

    public void setPressureAltitude(float pa) { mPressureAltitude = pa; }

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
        mInclinometer = (float)angle;
    }

    public void setParams(GpsParams params, ExtendedGpsParams eparams, double bearing, double cdi, double vdi, String dst, String distance) {
        /**
         * Assign and limit numbers
         */
        mSpeed = (float)params.getSpeed();
 
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

        mGroundTrack = (float)(params.getBearing() + params.getDeclinition() + 360) % 360f;

        // ideally derive from gyro, use ground track otherwise
        if (!mIsYawFromMagneticSensor) {
            mYaw = mGroundTrack;
        }

        mTo = (float)(bearing + params.getDeclinition() + 360) % 360f;

        mCdi = (float)cdi; // CDI is in miles, each tick is 1 miles enroute
        if(mCdi > 5) {
            mCdi = 5;
        }
        if(mCdi < -5) {
            mCdi = -5;
        }
        
        mDst = dst;
        mDistance = distance;

        // degrees
        mVdi = (float)vdi;
        if(mVdi > 3.8f) {
            mVdi = 3.8f;
        }
        if(mVdi < 2.2f) {
            mVdi = 2.2f;
        }
        mLat = params.getLatitude();
        mLon = params.getLongitude();
    }
    
    public void setTraffic(SparseArray<Traffic> traffic) {
        mTraffic = traffic;
    } 
    
    private static double altitudeToPressure(double altitude) {
        return 29.92 * Math.pow(1.0 - (altitude / 145366.45), 1.0 / 0.190284); 
    }

    // use string formatting functions because String.format takes much more CPU

    private static String degreesString(float d) {
        int intVal = (int)(d + 360) % 360;
        return (intVal == 0 ?  "360" :
                intVal < 10 ?  "00" + Integer.toString(intVal) :
                        intVal < 100 ? "0" + Integer.toString(intVal) :
                                Integer.toString(intVal)) + "\u00B0";
    }

    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(" ");
        return sb.toString();
    }
    
    private static int numDigits(int n) {
        if (n < 100000){
            // 5 or less
            if (n < 100){
                // 1 or 2
                if (n < 10)
                    return 1;
                else
                    return 2;
            }else{
                // 3 or 4 or 5
                if (n < 1000)
                    return 3;
                else{
                    // 4 or 5
                    if (n < 10000)
                        return 4;
                    else
                        return 5;
                }
            }
        } else {
            // 6 or more
            if (n < 10000000) {
                // 6 or 7
                if (n < 1000000)
                    return 6;
                else
                    return 7;
            } else {
                // 8 to 10
                if (n < 100000000)
                    return 8;
                else {
                    // 9 or 10
                    if (n < 1000000000)
                        return 9;
                    else
                        return 10;
                }
            }
        }        
    }

    /**
     * Returns darker version of specified <code>color</code>.
     */
    public static int darker (int color, float factor) {
        int a = Color.alpha( color );
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }
}

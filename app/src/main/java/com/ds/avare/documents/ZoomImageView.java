/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.documents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/**
 * Picture view with pinch zoom, drag, and double tap zoom. While the picture
 * is not zoomed a horizontal fling is reported as a swipe so the owner can go
 * to the previous / next picture or page, like the avarex picture viewer.
 */
public class ZoomImageView extends ImageView {

    /** Zoom limit, relative to the picture fitted to the view. */
    private static final float MAX_ZOOM = 6f;
    private static final float DOUBLE_TAP_ZOOM = 2.5f;
    private static final float SWIPE_VELOCITY = 900f;

    /** Receives swipes and taps. */
    public interface Listener {
        /** @param direction +1 for the next picture, -1 for the previous one */
        void onSwipe(int direction);

        void onTap();
    }

    private final Matrix mMatrix = new Matrix();
    private final float[] mValues = new float[9];
    private final RectF mRect = new RectF();
    private float mBaseScale = 1f;
    private Listener mListener;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    public ZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        mScaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        zoomBy(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                        return true;
                    }
                });
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (mScaleDetector.isInProgress() || !isZoomed()) {
                    return false;
                }
                mMatrix.postTranslate(-dx, -dy);
                applyMatrix();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (!isZoomed() && mListener != null
                        && Math.abs(vx) > SWIPE_VELOCITY && Math.abs(vx) > Math.abs(vy) * 1.5f) {
                    mListener.onSwipe(vx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isZoomed()) {
                    fit();
                } else {
                    zoomBy(DOUBLE_TAP_ZOOM, e.getX(), e.getY());
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mListener != null) {
                    mListener.onTap();
                }
                return true;
            }
        });
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    /** @return true when the picture is zoomed in beyond the fitted size */
    public boolean isZoomed() {
        return currentScale() > mBaseScale * 1.02f;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        fit();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fit();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    private float currentScale() {
        mMatrix.getValues(mValues);
        return mValues[Matrix.MSCALE_X];
    }

    /** Fit the whole picture in the view, centered. */
    private void fit() {
        Drawable d = getDrawable();
        int vw = getWidth();
        int vh = getHeight();
        if (d == null || vw == 0 || vh == 0 || d.getIntrinsicWidth() <= 0 || d.getIntrinsicHeight() <= 0) {
            return;
        }
        float dw = d.getIntrinsicWidth();
        float dh = d.getIntrinsicHeight();
        mBaseScale = Math.min(vw / dw, vh / dh);
        mMatrix.reset();
        mMatrix.postScale(mBaseScale, mBaseScale);
        mMatrix.postTranslate((vw - dw * mBaseScale) / 2f, (vh - dh * mBaseScale) / 2f);
        setImageMatrix(mMatrix);
    }

    private void zoomBy(float factor, float px, float py) {
        float target = currentScale() * factor;
        float min = mBaseScale;
        float max = mBaseScale * MAX_ZOOM;
        if (target < min) {
            target = min;
        } else if (target > max) {
            target = max;
        }
        float f = target / currentScale();
        mMatrix.postScale(f, f, px, py);
        applyMatrix();
    }

    /** Keep the picture from being dragged out of view, then show it. */
    private void applyMatrix() {
        Drawable d = getDrawable();
        if (d == null) {
            return;
        }
        mRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        mMatrix.mapRect(mRect);
        float dx = 0;
        float dy = 0;
        if (mRect.width() <= getWidth()) {
            dx = (getWidth() - mRect.width()) / 2f - mRect.left;
        } else if (mRect.left > 0) {
            dx = -mRect.left;
        } else if (mRect.right < getWidth()) {
            dx = getWidth() - mRect.right;
        }
        if (mRect.height() <= getHeight()) {
            dy = (getHeight() - mRect.height()) / 2f - mRect.top;
        } else if (mRect.top > 0) {
            dy = -mRect.top;
        } else if (mRect.bottom < getHeight()) {
            dy = getHeight() - mRect.bottom;
        }
        mMatrix.postTranslate(dx, dy);
        setImageMatrix(mMatrix);
    }
}

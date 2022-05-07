package com.ds.avare.views;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.utils.GenericCallback;

public class PanZoomView extends View {

    private static final int INVALID_POINTER_ID = 1;
    private int mActivePointerId = INVALID_POINTER_ID;

    protected Scale mScale;
    protected Pan mPan;

    private float mScaleFactor;
    private ScaleGestureDetector mScaleDetector;
    private float mPosX;
    private float mPosY;

    private float mLastTouchX;
    private float mLastTouchY;
    private float mFocusY;
    private float mFocusX;

    private void setup(Context context) {
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mScale = new Scale();
        mPan = new Pan();
        mScaleFactor = mScale.getScaleFactor();
    }

    public PanZoomView(Context context) {
        super(context);
        setup(context);
    }

    public PanZoomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public PanZoomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    public void resetPan() {
        mLastTouchX = 0;
        mLastTouchY = 0;
        mPosX = 0;
        mPosY = 0;
        mPan = new Pan();
    }

    public void resetZoom(double maxScale) {
        mScaleFactor = 1;
        mScale = new Scale(maxScale);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;

                // Save the ID of this pointer
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                mLastTouchX = x;
                mLastTouchY = y;

                // do not pan when scaling
                if (mScaleDetector.isInProgress()) {
                    return true;
                }

                mPosX += dx / mScaleFactor;
                mPosY += dy / mScaleFactor;

                mPan.setMove(mPosX, mPosY);
                invalidate();

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            if (detector.isInProgress()) {
                mFocusX = detector.getFocusX();
                mFocusY = detector.getFocusY();
            }

            mScale.setScaleFactor(mScaleFactor);
            mScaleFactor = mScale.getScaleFactor();
            invalidate();

            return true;
        }
    }

}

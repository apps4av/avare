package com.ds.avare.views;

import android.content.Context;
import android.graphics.Point;
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

    private Point mFocusPoint;
    private ScaleGestureDetector mScaleDetector;

    private float mLastTouchX;
    private float mLastTouchY;

    private void setup(Context context) {
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mScale = new Scale();
        mPan = new Pan();
        mFocusPoint = new Point();
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

    private GenericCallback mMotionCallback;
    public void setMotionCallback(GenericCallback cb) {
        mMotionCallback = cb;
    }

    public void resetPan() {
        mLastTouchX = 0;
        mLastTouchY = 0;
        mPan = new Pan();
    }

    public void resetZoom(double maxScale) {
        mScale = new Scale(maxScale);
    }

    public Point getFocusPoint() {
        return mFocusPoint;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        boolean reload = false;
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
                
                float xm = mPan.getMoveX() + dx / mScale.getScaleFactor() / mScale.getMacroFactor();  // slow down pan with zoom out
                float ym = mPan.getMoveY() + dy / mScale.getScaleFactor() / mScale.getMacroFactor();  // so finger does not move through

                reload = mPan.setMove(xm, ym);
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
        // cb for motion event
        if(mMotionCallback != null) {
            mMotionCallback.callback(ev, reload);
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScale.setScaleFactor(mScale.getScaleFactor() * detector.getScaleFactor());
            if (detector.isInProgress()) {
                mFocusPoint.set((int)detector.getFocusX(), (int)detector.getFocusY());
            }
            else {
                if(mMotionCallback != null) {
                    mMotionCallback.callback(null, true);
                }
            }

            invalidate();
            return true;
        }
    }

}

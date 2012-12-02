/**
 * 
 */
package com.ds.avare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author zkhan
 *
 */
public class WeatherView extends View {

    private Context mContext;
    private Paint mPaint;
    
    /**
     * @param context
     */
    public WeatherView(Context context) {
        super(context);
        setup(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public WeatherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public WeatherView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    private void setup(Context context) {
        mContext = context;
        mPaint = new Paint();
    }
    
    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {
        drawMap(canvas);
    }

    /**
     * 
     * @param canvas
     */
    private void drawMap(Canvas canvas) {
    }
    
}

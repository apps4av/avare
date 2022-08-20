package com.ds.avare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ds.avare.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;
import com.google.mlkit.vision.digitalink.RecognitionCandidate;
import com.google.mlkit.vision.digitalink.RecognitionResult;

import java.util.List;

public class WritingTextView extends TextView implements View.OnTouchListener {

    private Ink.Stroke.Builder          mStrokeBuilder;
    private Ink.Builder                 mInkBuilder;
    DigitalInkRecognizer mRecognizer;
    private boolean                    mWriting;
    DigitalInkRecognitionModel mModel = null;
    final Handler mHandler = new Handler();
    private Paint mPaint;
    private ShadowedText mShadowedText;
    private Context mContext;
    private float            mDpi;

    void setup(Context context) {
        setOnTouchListener(this);
        mWriting = false;
        mContext = context;
        mStrokeBuilder = Ink.Stroke.builder();
        mInkBuilder = Ink.builder();
        mPaint = new Paint();
        mShadowedText = new ShadowedText(mContext);

        // Pick a recognition model.
        mModel = DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.EN_US).build();
        RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();

        remoteModelManager.isModelDownloaded(mModel).addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if(success) {
                    mWriting = true;
                }
                else {
                    mWriting = false;
                    remoteModelManager.download(mModel, new DownloadConditions.Builder().build());
                }
            }
        });

        DigitalInkRecognizerOptions.Builder opts = DigitalInkRecognizerOptions.builder(mModel);
        opts.setMaxResultCount(1); // max results
        mRecognizer = DigitalInkRecognition.getClient(opts.build());
    }

    public WritingTextView(Context context) {
        super(context);
        setup(context);
    }

    public WritingTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public WritingTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        mPaint.setTypeface(Helper.getTypeFace(mContext));
        mPaint.setTextSize(Helper.adjustTextSize(mContext, R.dimen.buttonTextSize));
        float offset = getHeight() / 10;
        mPaint.setColor(Color.RED);
        // commonly used aviation words
        mShadowedText.draw(c, mPaint, "W", Color.RED,    ShadowedText.LEFT, getWidth(), offset * 1);
        mShadowedText.draw(c, mPaint, "V", Color.YELLOW, ShadowedText.LEFT, getWidth(), offset * 2);
        mShadowedText.draw(c, mPaint, "S", Color.BLUE,   ShadowedText.LEFT, getWidth(), offset * 3);
        mShadowedText.draw(c, mPaint, "R", Color.GREEN,  ShadowedText.LEFT, getWidth(), offset * 4);
        mShadowedText.draw(c, mPaint, "C", Color.RED,    ShadowedText.LEFT, getWidth(), offset * 5);
        mShadowedText.draw(c, mPaint, "R", Color.YELLOW, ShadowedText.LEFT, getWidth(), offset * 6);
        mShadowedText.draw(c, mPaint, "A", Color.BLUE,   ShadowedText.LEFT, getWidth(), offset * 7);
        mShadowedText.draw(c, mPaint, "F", Color.GREEN,  ShadowedText.LEFT, getWidth(), offset * 8);
        mShadowedText.draw(c, mPaint, "T", Color.CYAN,   ShadowedText.LEFT, getWidth(), offset * 9);
    }

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        // writing
        float x= e.getX();
        float y= e.getY();
        float offset = getHeight() / 10;

        if(x > getWidth() * 0.90 && (e.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            // insert commonly used aviation words
            if(y < offset * 1.5) {
                setText(getText() + "Wind ");
            }
            else if (y < offset * 2.5) {
                setText(getText() + "Visibility ");
            }
            else if (y < offset * 3.5) {
                setText(getText() + "Sky ");
            }
            else if (y < offset * 4.5) {
                setText(getText() + "Runway ");
            }
            else if (y < offset * 5.5) {
                setText(getText() + "Cleared ");
            }
            else if (y < offset * 6.5) {
                setText(getText() + "Route ");
            }
            else if (y < offset * 7.5) {
                setText(getText() + "Altitude ");
            }
            else if (y < offset * 8.5) {
                setText(getText() + "Frequency ");
            }
            else if (y < offset * 9.5) {
                setText(getText() + "Transponder ");
            }
            return true;
        }
        long t = e.getEventTime();
        if(mWriting) {
            switch (e.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:
                    mHandler.removeCallbacksAndMessages(null); // down so word continues
                    mStrokeBuilder = Ink.Stroke.builder();
                    mStrokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(null != mStrokeBuilder) {
                        mStrokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(null != mStrokeBuilder) {
                        mStrokeBuilder.addPoint(Ink.Point.create(x, y, t));
                        mInkBuilder.addStroke(mStrokeBuilder.build());
                        mStrokeBuilder = null;
                        // only if callback is not null do the detection
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // This is what to send to the recognizer.
                                Ink ink = mInkBuilder.build();

                                mRecognizer.recognize(ink)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<RecognitionResult>() {
                                                    @Override
                                                    public void onSuccess(RecognitionResult recognitionResult) {
                                                        List<RecognitionCandidate> candidates = recognitionResult.getCandidates();
                                                        setText(getText() + candidates.get(0).getText() + " ");
                                                    }
                                                }
                                        );
                                // new recognition
                                mInkBuilder = Ink.builder();
                            }
                        }, 500); // lifting up for 500 ms is a new word
                    }
                    break;
                default:
                    break;

            }
            // writing so do not pan
            return true;
        }
        return false;
    }
}

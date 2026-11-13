/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.avare.documents.ImageLoader;
import com.ds.avare.documents.ZoomImageView;
import com.ds.avare.utils.Helper;

import java.io.File;

/**
 * Full screen, zoomable picture viewer for the Documents screen. Shows internet
 * weather pictures (WPC, SigWx, ...) or local picture files. Swipe left / right
 * or use the arrows to go to the previous / next picture without closing the
 * viewer, like the avarex WebPictureViewer. While a picture is zoomed in, drags
 * pan it; zoom back out to swipe again.
 */
public class PictureViewerActivity extends AppCompatActivity {

    /** String[] of titles, same order as {@link #EXTRA_SOURCES}. */
    public static final String EXTRA_TITLES = "titles";
    /** String[] of http(s) URLs or absolute file paths. */
    public static final String EXTRA_SOURCES = "sources";
    public static final String EXTRA_INDEX = "index";

    private static final String STATE_INDEX = "index";
    private static final int MAX_DIM = 4096;

    private String[] mTitles;
    private String[] mSources;
    private int mIndex;

    private ZoomImageView mImage;
    private ProgressBar mProgress;
    private TextView mTitle;
    private TextView mCount;
    private Button mPrev;
    private Button mNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitles = getIntent().getStringArrayExtra(EXTRA_TITLES);
        mSources = getIntent().getStringArrayExtra(EXTRA_SOURCES);
        if (mTitles == null || mSources == null || mSources.length == 0
                || mTitles.length != mSources.length) {
            finish();
            return;
        }
        mIndex = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_INDEX)
                : getIntent().getIntExtra(EXTRA_INDEX, 0);
        mIndex = Math.max(0, Math.min(mSources.length - 1, mIndex));

        buildUi();
        Helper.applySystemBarInsets(this);
        show(mIndex);
    }

    private void buildUi() {
        final int dp = Math.round(getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF202020);
        Button back = new Button(this);
        back.setText("<");
        back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back, new LinearLayout.LayoutParams(56 * dp, 48 * dp));
        mTitle = new TextView(this);
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTextSize(16);
        mTitle.setSingleLine(true);
        mTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(mTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mCount = new TextView(this);
        mCount.setTextColor(Color.WHITE);
        mCount.setTextSize(14);
        mCount.setPadding(12 * dp, 0, 16 * dp, 0);
        top.addView(mCount);
        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout frame = new FrameLayout(this);
        mImage = new ZoomImageView(this);
        mImage.setListener(new ZoomImageView.Listener() {
            @Override
            public void onSwipe(int direction) {
                show(mIndex + direction);
            }

            @Override
            public void onTap() {
                // nothing
            }
        });
        frame.addView(mImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mProgress = new ProgressBar(this);
        frame.addView(mProgress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (mSources.length > 1) {
            LinearLayout bottom = new LinearLayout(this);
            bottom.setOrientation(LinearLayout.HORIZONTAL);
            bottom.setGravity(Gravity.CENTER);
            bottom.setBackgroundColor(0xFF202020);
            mPrev = new Button(this);
            mPrev.setText("<");
            mPrev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    show(mIndex - 1);
                }
            });
            bottom.addView(mPrev, new LinearLayout.LayoutParams(0, 48 * dp, 1f));
            mNext = new Button(this);
            mNext.setText(">");
            mNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    show(mIndex + 1);
                }
            });
            bottom.addView(mNext, new LinearLayout.LayoutParams(0, 48 * dp, 1f));
            root.addView(bottom, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        setContentView(root);
    }

    private void show(int requested) {
        final int index = Math.max(0, Math.min(mSources.length - 1, requested));
        if (index != requested && requested != mIndex) {
            return; // already at the first / last picture
        }
        mIndex = index;
        mTitle.setText(mTitles[index]);
        if (mSources.length > 1) {
            mCount.setText((index + 1) + " / " + mSources.length);
            mPrev.setEnabled(index > 0);
            mNext.setEnabled(index < mSources.length - 1);
        }
        mProgress.setVisibility(View.VISIBLE);
        mImage.setImageBitmap(null);

        ImageLoader.Callback callback = new ImageLoader.Callback() {
            @Override
            public void onLoaded(Bitmap bitmap) {
                if (isFinishing() || index != mIndex) {
                    return; // the user already moved on
                }
                mProgress.setVisibility(View.GONE);
                if (bitmap == null) {
                    Helper.makeText(PictureViewerActivity.this,
                            getString(R.string.DocumentsPictureFailed), Toast.LENGTH_SHORT).show();
                } else {
                    mImage.setImageBitmap(bitmap);
                }
            }
        };
        String source = mSources[index];
        if (source.startsWith("http://") || source.startsWith("https://")) {
            ImageLoader.loadUrl(this, source, MAX_DIM, callback);
        } else {
            ImageLoader.loadFile(new File(source), MAX_DIM, callback);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_INDEX, mIndex);
    }
}

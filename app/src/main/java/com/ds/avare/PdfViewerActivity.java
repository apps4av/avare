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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.avare.documents.ZoomImageView;
import com.ds.avare.utils.Helper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-app PDF viewer for the Documents screen, built on {@link PdfRenderer}.
 * One page is shown at a time: swipe left / right, use the arrows or the
 * slider to change pages, pinch or double tap to zoom. The last page read of
 * each PDF is remembered.
 *
 * {@link PdfRenderer} does not expose text, so there is no text search.
 */
public class PdfViewerActivity extends AppCompatActivity {

    /** Absolute path of the PDF file. */
    public static final String EXTRA_PATH = "path";
    /** Title to show, defaults to the file name. */
    public static final String EXTRA_TITLE = "title";

    private static final String STATE_PAGE = "page";
    private static final String PREFS = "pdf_last_page";
    /** Largest bitmap we render, in pixels. */
    private static final int MAX_PIXELS = 8 * 1000 * 1000;
    private static final int MAX_SIDE = 4096;

    private ParcelFileDescriptor mFd;
    private PdfRenderer mRenderer;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Object mLock = new Object();

    private ZoomImageView mImage;
    private ProgressBar mProgress;
    private TextView mPageText;
    private SeekBar mSeek;
    private Button mPrev;
    private Button mNext;

    private String mPath;
    private int mPageCount;
    private int mPage;
    private Bitmap mBitmap;
    private volatile boolean mClosed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPath = getIntent().getStringExtra(EXTRA_PATH);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (mPath == null) {
            finish();
            return;
        }
        if (title == null) {
            title = new File(mPath).getName();
        }

        buildUi(title);
        Helper.applySystemBarInsets(this);

        try {
            mFd = ParcelFileDescriptor.open(new File(mPath), ParcelFileDescriptor.MODE_READ_ONLY);
            mRenderer = new PdfRenderer(mFd);
            mPageCount = mRenderer.getPageCount();
        } catch (Throwable t) {
            // missing, damaged, or password protected
            Helper.makeText(this, getString(R.string.DocumentsPdfFailed), Toast.LENGTH_LONG).show();
            closeRenderer();
            finish();
            return;
        }
        if (mPageCount <= 0) {
            Helper.makeText(this, getString(R.string.DocumentsPdfFailed), Toast.LENGTH_LONG).show();
            closeRenderer();
            finish();
            return;
        }

        mSeek.setMax(Math.max(0, mPageCount - 1));
        int page = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_PAGE, 0)
                : getSharedPreferences(PREFS, MODE_PRIVATE).getInt(mPath, 0);
        showPage(page);
    }

    private void buildUi(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        // top bar: back, title
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
        top.addView(back, new LinearLayout.LayoutParams(dp(56), dp(48)));
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mPageText = new TextView(this);
        mPageText.setTextColor(Color.WHITE);
        mPageText.setTextSize(14);
        mPageText.setPadding(dp(12), dp(12), dp(16), dp(12));
        mPageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askPage();
            }
        });
        top.addView(mPageText);
        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // page area
        FrameLayout frame = new FrameLayout(this);
        mImage = new ZoomImageView(this);
        mImage.setListener(new ZoomImageView.Listener() {
            @Override
            public void onSwipe(int direction) {
                showPage(mPage + direction);
            }

            @Override
            public void onTap() {
                // nothing, bars stay visible while flying
            }
        });
        frame.addView(mImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mProgress = new ProgressBar(this);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        frame.addView(mProgress, plp);
        root.addView(frame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // bottom bar: previous, slider, next
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setBackgroundColor(0xFF202020);
        mPrev = new Button(this);
        mPrev.setText("<");
        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPage(mPage - 1);
            }
        });
        bottom.addView(mPrev, new LinearLayout.LayoutParams(dp(64), dp(48)));
        mSeek = new SeekBar(this);
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updatePageText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                showPage(seekBar.getProgress());
            }
        });
        bottom.addView(mSeek, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mNext = new Button(this);
        mNext.setText(">");
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPage(mPage + 1);
            }
        });
        bottom.addView(mNext, new LinearLayout.LayoutParams(dp(64), dp(48)));
        root.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void updatePageText(int page) {
        mPageText.setText(getString(R.string.DocumentsPageOf, page + 1, mPageCount));
    }

    private void askPage() {
        if (mRenderer == null) {
            return;
        }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(getString(R.string.DocumentsPageOf, mPage + 1, mPageCount));
        new AlertDialog.Builder(this)
                .setTitle(R.string.DocumentsGoToPage)
                .setView(input)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            showPage(Integer.parseInt(input.getText().toString().trim()) - 1);
                        } catch (NumberFormatException ignored) {
                            // not a number, stay on this page
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    /** Render a page off the UI thread and show it when ready. */
    private void showPage(int requested) {
        if (mRenderer == null) {
            return;
        }
        final int page = Math.max(0, Math.min(mPageCount - 1, requested));
        mPage = page;
        mSeek.setProgress(page);
        updatePageText(page);
        mPrev.setEnabled(page > 0);
        mNext.setEnabled(page < mPageCount - 1);
        mProgress.setVisibility(View.VISIBLE);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(mPath, page).apply();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        final int targetWidth = 2 * Math.min(dm.widthPixels, dm.heightPixels);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = render(page, targetWidth);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mClosed || page != mPage) {
                            // closed, or the user already moved on to another page
                            if (bitmap != null) {
                                bitmap.recycle();
                            }
                            return;
                        }
                        mProgress.setVisibility(View.GONE);
                        if (bitmap == null) {
                            Helper.makeText(PdfViewerActivity.this,
                                    getString(R.string.DocumentsPdfFailed), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Bitmap old = mBitmap;
                        mBitmap = bitmap;
                        mImage.setImageBitmap(bitmap);
                        if (old != null) {
                            old.recycle();
                        }
                    }
                });
            }
        });
    }

    private Bitmap render(int page, int targetWidth) {
        synchronized (mLock) {
            if (mClosed || mRenderer == null) {
                return null;
            }
            PdfRenderer.Page p = null;
            try {
                p = mRenderer.openPage(page);
                float scale = (float) targetWidth / p.getWidth();
                int w = Math.max(1, Math.round(p.getWidth() * scale));
                int h = Math.max(1, Math.round(p.getHeight() * scale));
                // keep very large pages within memory limits
                float limit = Math.min(1f, Math.min(
                        (float) MAX_SIDE / Math.max(w, h),
                        (float) Math.sqrt((double) MAX_PIXELS / ((double) w * h))));
                w = Math.max(1, Math.round(w * limit));
                h = Math.max(1, Math.round(h * limit));
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            } catch (Throwable t) {
                return null;
            } finally {
                if (p != null) {
                    p.close();
                }
            }
        }
    }

    private void closeRenderer() {
        synchronized (mLock) {
            mClosed = true;
            try {
                if (mRenderer != null) {
                    mRenderer.close();
                }
            } catch (Throwable ignored) {
                // already closed
            }
            try {
                if (mFd != null) {
                    mFd.close();
                }
            } catch (Throwable ignored) {
                // already closed
            }
            mRenderer = null;
            mFd = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, mPage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
        closeRenderer();
    }
}

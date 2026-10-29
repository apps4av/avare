/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.R;

/**
 * Text toast drawn by the app at the vertical center of the right edge.
 * <p>
 * Platform text toasts ignore {@link Toast#setGravity} on Android 11+ when the
 * app targets API 30 or higher, and custom toast views are discarded from API 31.
 * This still subclasses {@link Toast} so existing {@code setText}/{@code show}
 * call sites stay the same, but {@link #show()} uses an app window.
 */
public class AvareToast extends Toast {

    private static final int SHORT_MS = 2000;
    private static final int LONG_MS = 3500;

    private static AvareToast sCurrent;

    private final Context mSource;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mDismiss = new Runnable() {
        @Override
        public void run() {
            cancel();
        }
    };

    private CharSequence mMessage = "";
    private Dialog mDialog;
    private TextView mTextView;
    private View mHostDecor;
    private View.OnAttachStateChangeListener mDetachListener;

    public AvareToast(Context context) {
        super(context.getApplicationContext() != null ? context.getApplicationContext() : context);
        mSource = context;
    }

    @Override
    public void setText(CharSequence s) {
        mMessage = s == null ? "" : s;
        bindMessage();
    }

    @Override
    public void show() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
            return;
        }
        if (mMessage.length() == 0) {
            return;
        }
        final Activity activity = hostActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            fallback();
            return;
        }
        if (sCurrent != null && sCurrent != this) {
            sCurrent.cancel();
        }
        sCurrent = this;
        try {
            ensureDialog(activity);
            bindMessage();
            place(activity);
            if (!mDialog.isShowing()) {
                mDialog.show();
                place(activity);
            }
            watchHost(activity);
        } catch (RuntimeException ex) {
            try {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            } catch (RuntimeException ignored) {
            }
            mDialog = null;
            mTextView = null;
            if (sCurrent == this) {
                sCurrent = null;
            }
            fallback();
            return;
        }
        mHandler.removeCallbacks(mDismiss);
        mHandler.postDelayed(mDismiss, durationMs());
    }

    @Override
    public void cancel() {
        mHandler.removeCallbacks(mDismiss);
        if (mHostDecor != null && mDetachListener != null) {
            mHostDecor.removeOnAttachStateChangeListener(mDetachListener);
        }
        mDetachListener = null;
        mHostDecor = null;
        if (mDialog != null) {
            try {
                if (mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            } catch (RuntimeException ignored) {
            }
            mDialog = null;
            mTextView = null;
        }
        if (sCurrent == this) {
            sCurrent = null;
        }
    }

    private void ensureDialog(Activity activity) {
        if (mDialog != null && mDialog.getOwnerActivity() == activity) {
            return;
        }
        if (mDialog != null) {
            try {
                mDialog.dismiss();
            } catch (RuntimeException ignored) {
            }
        }
        float density = activity.getResources().getDisplayMetrics().density;
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int chrome = (int) ((10 + 40 + 12 + 16) * density);
        int maxText = (int) (screenWidth * 0.62f) - chrome;
        View root = LayoutInflater.from(activity).inflate(R.layout.toast, null);
        TextView text = (TextView) root.findViewById(R.id.toast_text);
        text.setMaxWidth(Math.max(maxText, (int) (160 * density)));
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        Dialog dialog = new Dialog(activity, R.style.AvareToast);
        dialog.setOwnerActivity(activity);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(root);
        mDialog = dialog;
        mTextView = text;
    }

    private void place(Activity activity) {
        Window window = mDialog.getWindow();
        if (window == null) {
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        int margin = (int) (16 * density) + rightInset(activity);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setElevation(8 * density);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        lp.x = margin;
        lp.y = 0;
        lp.dimAmount = 0f;
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        lp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        window.setAttributes(lp);
    }

    private void watchHost(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (mHostDecor == decor && mDetachListener != null) {
            return;
        }
        if (mHostDecor != null && mDetachListener != null) {
            mHostDecor.removeOnAttachStateChangeListener(mDetachListener);
        }
        mHostDecor = decor;
        mDetachListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                cancel();
            }
        };
        decor.addOnAttachStateChangeListener(mDetachListener);
    }

    private void bindMessage() {
        if (mTextView == null) {
            return;
        }
        mTextView.setText(mMessage);
        View card = (View) mTextView.getParent();
        if (card != null) {
            card.setContentDescription(mMessage);
        }
    }

    private int durationMs() {
        return getDuration() == LENGTH_LONG ? LONG_MS : SHORT_MS;
    }

    private void fallback() {
        if (mMessage.length() == 0) {
            return;
        }
        Context context = mSource.getApplicationContext() != null
                ? mSource.getApplicationContext() : mSource;
        Toast.makeText(context, mMessage, getDuration()).show();
    }

    private Activity hostActivity() {
        Context context = mSource;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                while (activity.getParent() != null) {
                    activity = activity.getParent();
                }
                return activity;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static int rightInset(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        WindowInsets insets = decor.getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return insets.getInsets(WindowInsets.Type.systemBars()).right;
        }
        return insets.getSystemWindowInsetRight();
    }
}

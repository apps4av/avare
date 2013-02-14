/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.animation;

import com.ds.avare.R;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

/**
 * @author zkhan
 *
 */
public class AnimateButton {

    private boolean mShowing;
    private Context mContext;
    private Button mButton;
    private View mReplaces;
    
    /**
     * 
     */
    public AnimateButton(Context ctx, Button b, View replaces) {
        mContext = ctx;
        mShowing = false;
        mButton = b;
        if(null == replaces) {
            /*
             * Dummy
             */
            mReplaces = new View(ctx);
        }
        else {
            /*
             * The view this animate hides
             */
            mReplaces = replaces;
        }
    }
    
    /**
     * 
     * @param
     */
    public void animate(final boolean visible) {
        Animation a;
        
        /*
         * Animates a button from left to right.
         */
        if(visible) {
            /*
             * Bring the button out
             */
            a = AnimationUtils.loadAnimation(mContext, R.anim.xlate_right);
            mShowing = true;
        }
        else {
            /*
             * If not showing then dont take back in
             */
            if(!mShowing) {
                return;
            }
            /*
             * Take the button in
             */
            a = AnimationUtils.loadAnimation(mContext, R.anim.xlate_left);
            mShowing = false;
        }
        a.reset();
        a.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                if(!visible) {
                    /*
                     * Set invisible when not animating
                     */
                    mButton.setVisibility(Button.INVISIBLE);
                    mReplaces.setVisibility(Button.VISIBLE);
                }
                else {
                    /*
                     * Animate back
                     */
                    animate(false);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                if(visible) {
                    /*
                     * Set visible when animating
                     */
                    mReplaces.setVisibility(Button.INVISIBLE);
                    mButton.setVisibility(Button.VISIBLE);
                }
            }
            
        });            
        mButton.clearAnimation();
        mButton.startAnimation(a);
    }
}

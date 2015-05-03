/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
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
    private View mView;
    private View mReplaces[];
    private int mReplacesVis[];
    private int mDir;
    
    public static final int DIRECTION_L_R = 1;
    public static final int DIRECTION_R_L = 2;
    public static final int DIRECTION_B_U = 3;
    public static final int DIRECTION_U_B = 4;
    
    /**
     * 
     */
    public AnimateButton(Context ctx, View b, int direction, View... replaces) {
        mContext = ctx;
        mShowing = false;
        mView = b;
        mDir = direction;
        /*
         * The view this animate hides
         */
        mReplaces = replaces;
        
        if(null == replaces) {
            /*
             * Dummy
             */
            mReplaces = new View[1];
            mReplaces[0] = new View(ctx);
        }

        mReplacesVis = new int[mReplaces.length];
}

    
    /**
     * 
     * @param
     */
    public void animateBack() {

        Animation a;
        /*
         * If not showing then dont take back in
         */
        if(!mShowing) {
            return;
        }
        
        int id;
        switch(mDir) {
            case DIRECTION_L_R:
                id = R.anim.xlate_left;
                break;
            case DIRECTION_R_L:
                id = R.anim.xlate_right_end;
                break;
            default:
                id = R.anim.xlate_up_end;
                break;
                
        }
        /*
         * Take the button in
         */
        a = AnimationUtils.loadAnimation(mContext, id);
        mShowing = false;
        
        a.reset();
        a.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                    /*
                     * Set invisible when not animating
                     */
                    mView.setVisibility(Button.INVISIBLE);
                    for(int v = 0; v < mReplaces.length; v++) {
                        mReplaces[v].setVisibility(mReplacesVis[v]);
                    }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
            
        });            
        mView.clearAnimation();
        mView.startAnimation(a);

    }

    /**
     * 
     */
    public void stopAndHide() {
        mView.setAnimation(null);

        mView.setVisibility(Button.INVISIBLE);
        for(int v = 0; v < mReplaces.length; v++) {
          mReplaces[v].setVisibility(mReplacesVis[v]);
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
            int id;
            switch(mDir) {
                case DIRECTION_L_R:
                    id = R.anim.xlate_right;
                    break;
                case DIRECTION_R_L:
                    id = R.anim.xlate_left_end;
                    break;
                default:
                    id = R.anim.xlate_up;
                    break;
                    
            }
            /*
             * Bring the button out
             */
            a = AnimationUtils.loadAnimation(mContext, id);
            mShowing = true;
        }
        else {
            /*
             * If not showing then dont take back in
             */
            if(!mShowing) {
                return;
            }
            int id;
            switch(mDir) {
                case DIRECTION_L_R:
                    id = R.anim.xlate_left_delay;
                    break;
                case DIRECTION_R_L:
                    id = R.anim.xlate_right_end_delay;
                    break;
                default:
                    id = R.anim.xlate_up_end;
                    break;
                    
            }
            /*
             * Take the button in
             */
            a = AnimationUtils.loadAnimation(mContext, id);
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
                    mView.setVisibility(Button.INVISIBLE);
                    for(int v = 0; v < mReplaces.length; v++) {
                        mReplaces[v].setVisibility(mReplacesVis[v]);
                    }
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
                    for(int v = 0; v < mReplaces.length; v++) {
                        mReplaces[v].setVisibility(mReplacesVis[v]);
                    }
                    mView.setVisibility(Button.VISIBLE);
                }
            }
            
        });
        mView.clearAnimation();
        mView.startAnimation(a);
    }
    
    /**
     * 
     * @param
     */
    public void animate() {
        Animation a;

        /*
         * If out then dont animate
         */
        if(mShowing) {
            return;
        }

        /*
         * Animates a button from left to right.
         */
        int id;
        switch(mDir) {
            case DIRECTION_L_R:
                id = R.anim.xlate_right;
                break;
            case DIRECTION_R_L:
                id = R.anim.xlate_left_end;
                break;
            default:
                id = R.anim.xlate_up;
                break;
                
        }
        /*
         * Bring the button out
         */
        a = AnimationUtils.loadAnimation(mContext, id);
        mShowing = true;

        a.reset();
        a.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                    /*
                     * Set visible when animating
                     */
                    for(int v = 0; v < mReplaces.length; v++) {
                    	mReplacesVis[v] = mReplaces[v].getVisibility(); 
                    	mReplaces[v].setVisibility(Button.INVISIBLE);
                    }
                    mView.setVisibility(Button.VISIBLE);
            }
            
        });            
        mView.clearAnimation();
        mView.startAnimation(a);
        
    }
}

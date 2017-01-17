package com.ds.avare.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

public class FullScreenHandler {

    private boolean fullScreen;

    public FullScreenHandler(boolean fsPref) {
        if(fsPref && isFullScreenSupported()) {
            fullScreen = true;
        }
        else fullScreen = false;
    }

    private boolean isFullScreen() {
        return fullScreen;
    }

    /**
     * Activates immersive mode flags on <b>viewToFullScreen</b> if fullscreen preference is
     * enabled and the system supports it.
     * @param viewToFullScreen
     */
    @TargetApi(19)
    public void activateFullScreenFlags(View viewToFullScreen) {
        if(isFullScreen()) {
            viewToFullScreen.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    /**
     * Checks if fullscreen preference is enabled and if system supports it. If so, displays
     * the provided <b>dialog</b> in an immersive way.
     * @param dialog AlertDialog to show
     * @see <a href="http://stackoverflow.com/a/23207365">http://stackoverflow.com/a/23207365</a>
     */
    public void showAlertDialog(AlertDialog dialog) {
        if(isFullScreen()) {
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            dialog.show();
            activateFullScreenFlags(dialog.getWindow().getDecorView());
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        else {
            dialog.show();
        }
    }

    /**
     * If fullscreen is enabled and supported, adds a UI Visibility change listener to
     * <b>viewToFullScreen</b>
     * @param viewToFullScreen The view to add full screen UI change listener to.
     */
    @TargetApi(19)
    public void addFullScreenVisibilityChangeListener(final View viewToFullScreen) {
        if(isFullScreen()) {
            viewToFullScreen.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if (6 != visibility) {
                        activateFullScreenFlags(viewToFullScreen);
                    }
                }
            });
        }
    }

    private static boolean isFullScreenSupported() {
        if(19 <= Build.VERSION.SDK_INT) {
            return true;
        }
        else {
            return false;
        }
    }
}

/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.permissions;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.ds.avare.R;

import java.lang.reflect.Method;

/**
 * Created by zkhan on 12/17/15.
 */
public class PermissionInstaller {


    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public static final int PERMISSION_ACCOUNT = 1;
    public static final int PERMISSION_LOCATION = 2;
    public static final int PERMISSION_EXTERNAL_STORAGE = 3;
    public static final int PERMISSION_SETTINGS = 4;

    public static void processAnnotations(final Object obj) {
        if (Build.VERSION.SDK_INT >= 23) {
            // For new permissions system
            try {
                // Find all annotations in the activity
                final Activity act = (Activity)obj;
                Class cl = obj.getClass();
                // Check which ones need to be granted
                for (Method m : cl.getDeclaredMethods()) {
                    RequestPermission annotation = m.getAnnotation(RequestPermission.class);
                    if (annotation != null) {
                        // Check which permissions need to be added
                        final String rationale;
                        final String permission;
                        switch (annotation.permission()) {
                            case PERMISSION_ACCOUNT:
                                rationale = act.getString(R.string.RationaleAccount);
                                permission = android.Manifest.permission.GET_ACCOUNTS;
                                break;
                            case PERMISSION_LOCATION:
                                rationale = act.getString(R.string.RationaleLocation);
                                permission = Manifest.permission.ACCESS_FINE_LOCATION;
                                break;
                            case PERMISSION_EXTERNAL_STORAGE:
                                rationale = act.getString(R.string.RationaleExternalStorage);
                                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                                break;
                            case PERMISSION_SETTINGS:
                                rationale = act.getString(R.string.RationaleSettings);
                                permission = Manifest.permission.WRITE_SETTINGS;
                                break;
                            default:
                                continue;
                        }

                        int hasPermission = act.checkSelfPermission(permission);
                        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                            if (!act.shouldShowRequestPermissionRationale(permission)) {
                                // show why permissions is needed
                                new AlertDialog.Builder(act)
                                        .setMessage(rationale)
                                        .setPositiveButton(act.getString(R.string.yes),
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // Request them from user
                                                        if(permission == Manifest.permission.WRITE_SETTINGS) {
                                                            // This needs special handling
                                                            if(!Settings.System.canWrite(act)) {
                                                                Intent i = new Intent();
                                                                i.setAction(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                                                act.startActivity(i);
                                                            }
                                                        }
                                                        else {
                                                            // This is regular handling
                                                            act.requestPermissions(new String[]{permission}, REQUEST_CODE_ASK_PERMISSIONS);
                                                        }
                                                    }
                                                })
                                        .setNegativeButton(act.getString(R.string.no),
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // Show warning
                                                        Toast.makeText(act, act.getString(R.string.NoRationale), Toast.LENGTH_LONG).show();
                                                    }
                                                })
                                            .create()
                                            .show();
                                return;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
            }
        }
    }
}

/**
 * Backups properties so users uninstall and install the app does not lose properties like plans.
 * User will need to go to Android Settings->System->Backup to back up.
 */
package com.ds.avare;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class AppBackupAgent extends BackupAgentHelper {
    static final String PREFS_BACKUP_KEY = "prefs";

    @Override
    public void onCreate() {
        // file to backup, this is preferences file
        String file = getApplicationContext().getPackageName() + "_preferences";
        // share pref does all the code to backup
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, file);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
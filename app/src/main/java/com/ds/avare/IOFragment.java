package com.ds.avare;

import android.app.Fragment;

import java.io.File;

public class IOFragment extends Fragment {

    private static final String FOLDER = "recorded";

    public static String getFileSavePath(String input) {
        String fl = StorageService.getInstance().getPreferences().getUserDataFolder() + File.separatorChar + FOLDER;

        // make a recorded folder in main folder and put files in there.
        File file = new File(fl);

        try {
            file.mkdirs();
        }
        catch (Exception e) {

        }
        return fl + File.separatorChar + input;
    }

}

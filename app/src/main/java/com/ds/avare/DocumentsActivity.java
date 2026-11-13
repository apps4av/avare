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
import android.content.ContentProviderClient;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ds.avare.content.UserContract;
import com.ds.avare.content.UserDatabaseHelper;
import com.ds.avare.content.UserProvider;
import com.ds.avare.documents.ImageLoader;
import com.ds.avare.documents.WebDocs;
import com.ds.avare.utils.Helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Documents screen, ported from the avarex DocumentsScreen. It is reached from
 * the Pro icon on the map and is only opened for subscribers.
 *
 * It shows folders and files as a grid:
 * <ul>
 * <li>the FAA documents downloaded from the Download menu (AIM, 14 CFR,
 * Handbooks, ACS, Advisory Circulars), shown as PDFs,</li>
 * <li>the flight tracks folder,</li>
 * <li>the user's own folders and files (import, create folder, move, share,
 * delete),</li>
 * <li>internet weather pictures (WPC, SigWx, AIRMETs, forecasts, ...).</li>
 * </ul>
 * PDFs open in {@link PdfViewerActivity}, pictures in
 * {@link PictureViewerActivity}, and text, KML and GeoJSON in
 * {@link TextViewerActivity}.
 */
public class DocumentsActivity extends AppCompatActivity {

    /** Folders the FAA zips unzip into, inside the app files folder. */
    private static final String[] FAA_FOLDERS = {"AIM", "CFR14", "Handbooks", "ACS", "AC"};
    private static final String[] FAA_TITLES = {
            "FAA AIM", "FAA 14 CFR", "FAA Handbooks", "FAA ACS", "FAA Advisory Circulars"};
    private static final String TRACKS_FOLDER = "tracks";
    /** Where the user's own folders and files live. */
    private static final String USER_FOLDER = "documents";
    /** The app's saved data: plans, lists, aircraft, recent destinations. */
    private static final String USER_DB = "user.db";

    private static final String FILTER_ALL = "@all";
    private static final String FILTER_USER = "@user";

    private static final String STATE_FOLDER = "folder";
    private static final String STATE_FILTER = "filter";

    private static final String[] TEXT_EXTENSIONS = {"txt", "kml", "geojson", "json"};
    private static final String[] PICTURE_EXTENSIONS = {"gif", "webp", "bmp", "png", "jpg", "jpeg"};

    /** One tile in the grid. */
    private static final class Entry {
        static final int FOLDER = 0;
        static final int FILE = 1;
        static final int WEB = 2;
        /** The user.db tile: the app's own saved data, shared as a backup copy. */
        static final int USER_DATA = 3;

        final int kind;
        final String name;
        final File file;
        final String url;
        final String type;

        Entry(int kind, String name, File file, String url, String type) {
            this.kind = kind;
            this.name = name;
            this.file = file;
            this.url = url;
            this.type = type;
        }
    }

    private File mFilesDir;
    private File mUserDir;
    /** Folder being shown, null for the top level. */
    private File mFolder;
    private String mFilter = FILTER_ALL;

    private final ArrayList<Entry> mEntries = new ArrayList<Entry>();
    private final List<WebDocs.Item> mWeb = WebDocs.all();
    private final ArrayList<String> mFilterKeys = new ArrayList<String>();
    private Set<String> mCovers = new HashSet<String>();
    private Set<String> mThumbnails = new HashSet<String>();

    private GridAdapter mAdapter;
    private TextView mTitle;
    private TextView mEmpty;
    private Button mBack;
    private Button mNewFolder;
    private Button mImport;
    private Spinner mFilterSpinner;
    private OnBackPressedCallback mBackCallback;
    /** The picker was opened from the User Data tile, so the pick is a user.db backup. */
    private boolean mPickingUserData;

    private final ActivityResultLauncher<String[]> mImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    new androidx.activity.result.ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            boolean userData = mPickingUserData;
                            mPickingUserData = false;
                            if (uri != null) {
                                if (userData || "db".equals(extension(displayName(uri)))) {
                                    askRestoreUserData(uri);
                                } else {
                                    importFile(uri);
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.documents);
        Helper.applySystemBarInsets(this);

        mFilesDir = new File(Helper.getInternalFolder(this));
        mUserDir = new File(mFilesDir, USER_FOLDER);
        if (!mUserDir.exists()) {
            mUserDir.mkdirs();
        }
        mCovers = assetNames("folder_covers");
        mThumbnails = assetNames("pdf_thumbnails");

        if (savedInstanceState != null) {
            String folder = savedInstanceState.getString(STATE_FOLDER);
            if (folder != null && new File(folder).isDirectory()) {
                mFolder = new File(folder);
            }
            mFilter = savedInstanceState.getString(STATE_FILTER, FILTER_ALL);
        }

        mTitle = findViewById(R.id.documents_title);
        mEmpty = findViewById(R.id.documents_empty);
        mBack = findViewById(R.id.documents_back);
        mNewFolder = findViewById(R.id.documents_new_folder);
        mImport = findViewById(R.id.documents_import);
        mFilterSpinner = findViewById(R.id.documents_filter);
        GridView grid = findViewById(R.id.documents_grid);

        mAdapter = new GridAdapter();
        grid.setAdapter(mAdapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                open(mEntries.get(position));
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return showActions(mEntries.get(position));
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goUp();
            }
        });
        mNewFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askNewFolder();
            }
        });
        mImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImportLauncher.launch(new String[]{"*/*"});
            }
        });

        mBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                goUp();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, mBackCallback);

        buildFilter();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // an FAA download or delete may have happened since this was last shown
        if (mAdapter != null) {
            refresh();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mFolder != null) {
            outState.putString(STATE_FOLDER, mFolder.getAbsolutePath());
        }
        outState.putString(STATE_FILTER, mFilter);
    }

    // ------------------------------------------------------------------ filter

    private void buildFilter() {
        LinkedHashSet<String> types = new LinkedHashSet<String>();
        for (WebDocs.Item item : mWeb) {
            types.add(item.type);
        }
        mFilterKeys.clear();
        mFilterKeys.add(FILTER_ALL);
        mFilterKeys.add(FILTER_USER);
        mFilterKeys.addAll(types);

        ArrayList<String> labels = new ArrayList<String>();
        for (String key : mFilterKeys) {
            labels.add(filterLabel(key));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFilterSpinner.setAdapter(adapter);
        int selected = mFilterKeys.indexOf(mFilter);
        if (selected < 0) {
            selected = 0;
            mFilter = FILTER_ALL;
        }
        mFilterSpinner.setSelection(selected, false);
        mFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String key = mFilterKeys.get(position);
                if (!key.equals(mFilter)) {
                    mFilter = key;
                    refresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String filterLabel(String key) {
        if (FILTER_ALL.equals(key)) {
            return getString(R.string.DocumentsAll);
        }
        if (FILTER_USER.equals(key)) {
            return getString(R.string.DocumentsUser);
        }
        return key;
    }

    // ----------------------------------------------------------------- listing

    private void refresh() {
        mEntries.clear();
        boolean showFs = FILTER_ALL.equals(mFilter) || FILTER_USER.equals(mFilter) || mFolder != null;

        if (showFs) {
            addFolders();
        }
        if (mFolder == null) {
            for (WebDocs.Item item : mWeb) {
                if (FILTER_ALL.equals(mFilter) || item.type.equals(mFilter)) {
                    mEntries.add(new Entry(Entry.WEB, item.name, null, item.url, item.type));
                }
            }
        }
        if (showFs) {
            addFiles();
        }

        mTitle.setText(mFolder == null ? getString(R.string.DocumentsTitle) : folderTitle(mFolder));
        mBack.setVisibility(mFolder == null ? View.INVISIBLE : View.VISIBLE);
        mBackCallback.setEnabled(mFolder != null);
        mFilterSpinner.setVisibility(mFolder == null ? View.VISIBLE : View.GONE);
        mNewFolder.setVisibility(canWriteHere() ? View.VISIBLE : View.GONE);
        mAdapter.notifyDataSetChanged();

        if (mEntries.isEmpty()) {
            mEmpty.setText(getString(mFolder != null ? R.string.DocumentsFolderEmpty
                    : R.string.DocumentsEmpty));
            mEmpty.setVisibility(View.VISIBLE);
        } else {
            mEmpty.setVisibility(View.GONE);
        }
    }

    private void addFolders() {
        ArrayList<Entry> folders = new ArrayList<Entry>();
        if (mFolder == null) {
            for (int i = 0; i < FAA_FOLDERS.length; i++) {
                File dir = new File(mFilesDir, FAA_FOLDERS[i]);
                if (dir.isDirectory() && hasContent(dir)) {
                    folders.add(new Entry(Entry.FOLDER, FAA_TITLES[i], dir, null, FILTER_USER));
                }
            }
            File tracks = new File(mFilesDir, TRACKS_FOLDER);
            if (tracks.isDirectory() && hasContent(tracks)) {
                folders.add(new Entry(Entry.FOLDER, TRACKS_FOLDER, tracks, null, FILTER_USER));
            }
            addSubfolders(mUserDir, folders);
        } else {
            addSubfolders(mFolder, folders);
        }
        sortByName(folders);
        mEntries.addAll(folders);
    }

    private void addSubfolders(File parent, ArrayList<Entry> out) {
        File[] children = parent.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory() && !child.getName().startsWith(".")) {
                out.add(new Entry(Entry.FOLDER, child.getName(), child, null, FILTER_USER));
            }
        }
    }

    private void addFiles() {
        File dir = mFolder != null ? mFolder : mUserDir;
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        ArrayList<Entry> files = new ArrayList<Entry>();
        for (File child : children) {
            if (child.isFile() && !child.getName().startsWith(".") && isSupported(child.getName())) {
                files.add(new Entry(Entry.FILE, child.getName(), child, null, FILTER_USER));
            }
        }
        if (mFolder == null) {
            // user.db sits with the app's other data, not in the documents folder
            File db = new File(Helper.getExternalFolder(this), USER_DB);
            if (db.isFile()) {
                files.add(new Entry(Entry.USER_DATA, getString(R.string.DocumentsUserData), db, null, FILTER_USER));
            }
        }
        sortByName(files);
        mEntries.addAll(files);
    }

    private static void sortByName(ArrayList<Entry> list) {
        Collections.sort(list, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                int byName = a.name.compareToIgnoreCase(b.name);
                return byName != 0 ? byName : a.name.compareTo(b.name);
            }
        });
    }

    /** True when a folder holds at least one file, so emptied FAA folders are hidden. */
    private static boolean hasContent(File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File child : children) {
            if (child.getName().startsWith(".")) {
                continue;
            }
            if (child.isFile() || hasContent(child)) {
                return true;
            }
        }
        return false;
    }

    private String folderTitle(File folder) {
        if (folder.getParentFile() != null && folder.getParentFile().equals(mFilesDir)) {
            for (int i = 0; i < FAA_FOLDERS.length; i++) {
                if (FAA_FOLDERS[i].equals(folder.getName())) {
                    return FAA_TITLES[i];
                }
            }
        }
        return folder.getName();
    }

    private void goUp() {
        if (mFolder == null) {
            finish();
            return;
        }
        File parent = mFolder.getParentFile();
        if (parent == null || parent.equals(mUserDir) || parent.equals(mFilesDir)) {
            mFolder = null;
        } else {
            mFolder = parent;
        }
        refresh();
    }

    // ------------------------------------------------------------------ opening

    private void open(Entry entry) {
        try {
            switch (entry.kind) {
                case Entry.FOLDER:
                    mFolder = entry.file;
                    refresh();
                    break;
                case Entry.WEB:
                    openWebPictures(entry);
                    break;
                case Entry.USER_DATA:
                    shareUserData(entry);
                    break;
                default:
                    openFile(entry);
                    break;
            }
        } catch (Throwable t) {
            Helper.makeText(this, getString(R.string.DocumentsOpenFailed), Toast.LENGTH_SHORT).show();
        }
    }

    private void openWebPictures(Entry clicked) {
        ArrayList<String> titles = new ArrayList<String>();
        ArrayList<String> sources = new ArrayList<String>();
        int index = 0;
        for (Entry e : mEntries) {
            if (e.kind == Entry.WEB) {
                if (e == clicked) {
                    index = titles.size();
                }
                titles.add(e.name);
                sources.add(e.url);
            }
        }
        startPictures(titles, sources, index);
    }

    private void openFile(Entry entry) {
        String ext = extension(entry.name);
        if ("pdf".equals(ext)) {
            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_PATH, entry.file.getAbsolutePath());
            intent.putExtra(PdfViewerActivity.EXTRA_TITLE, entry.name);
            startActivity(intent);
        } else if (Arrays.asList(PICTURE_EXTENSIONS).contains(ext)) {
            ArrayList<String> titles = new ArrayList<String>();
            ArrayList<String> sources = new ArrayList<String>();
            int index = 0;
            for (Entry e : mEntries) {
                if (e.kind == Entry.FILE && Arrays.asList(PICTURE_EXTENSIONS).contains(extension(e.name))) {
                    if (e == entry) {
                        index = titles.size();
                    }
                    titles.add(e.name);
                    sources.add(e.file.getAbsolutePath());
                }
            }
            startPictures(titles, sources, index);
        } else {
            Intent intent = new Intent(this, TextViewerActivity.class);
            intent.putExtra(TextViewerActivity.EXTRA_PATH, entry.file.getAbsolutePath());
            intent.putExtra(TextViewerActivity.EXTRA_TITLE, entry.name);
            startActivity(intent);
        }
    }

    private void startPictures(List<String> titles, List<String> sources, int index) {
        Intent intent = new Intent(this, PictureViewerActivity.class);
        intent.putExtra(PictureViewerActivity.EXTRA_TITLES, titles.toArray(new String[0]));
        intent.putExtra(PictureViewerActivity.EXTRA_SOURCES, sources.toArray(new String[0]));
        intent.putExtra(PictureViewerActivity.EXTRA_INDEX, index);
        startActivity(intent);
    }

    // ------------------------------------------------------------------ actions

    /** True for files and folders the user made or imported, not FAA downloads. */
    private boolean isUserOwned(File file) {
        return isUnder(file, mUserDir);
    }

    private boolean isInTracks(File file) {
        return isUnder(file, new File(mFilesDir, TRACKS_FOLDER));
    }

    private static boolean isUnder(File file, File ancestor) {
        String path = file.getAbsolutePath();
        String base = ancestor.getAbsolutePath();
        return path.startsWith(base + File.separator);
    }

    /** New folders and imports go to the user's area only. */
    private boolean canWriteHere() {
        return mFolder == null || mFolder.equals(mUserDir) || isUserOwned(mFolder);
    }

    private File destinationFolder() {
        return mFolder != null && isUserOwned(mFolder) ? mFolder : mUserDir;
    }

    private boolean showActions(final Entry entry) {
        if (entry.kind == Entry.WEB) {
            return true;
        }
        final ArrayList<String> labels = new ArrayList<String>();
        final ArrayList<Runnable> actions = new ArrayList<Runnable>();

        if (entry.kind == Entry.USER_DATA) {
            labels.add(getString(R.string.DocumentsShareUserData));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    shareUserData(entry);
                }
            });
            labels.add(getString(R.string.DocumentsRestoreUserData));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    mPickingUserData = true;
                    mImportLauncher.launch(new String[]{"*/*"});
                }
            });
        } else if (entry.kind == Entry.FILE) {
            labels.add(getString(R.string.DocumentsShare));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    shareFiles(Collections.singletonList(entry.file), entry.name);
                }
            });
            if (isUserOwned(entry.file)) {
                labels.add(getString(R.string.DocumentsMove));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        askMove(entry);
                    }
                });
            }
        } else {
            labels.add(getString(R.string.DocumentsShareFolder));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    shareFolder(entry);
                }
            });
        }
        if (entry.kind != Entry.USER_DATA && (isUserOwned(entry.file) || isInTracks(entry.file))) {
            labels.add(getString(R.string.Delete));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    askDelete(entry);
                }
            });
        }

        new AlertDialog.Builder(this)
                .setTitle(entry.name)
                .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actions.get(which).run();
                    }
                })
                .show();
        return true;
    }

    private void askDelete(final Entry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.Delete)
                .setMessage(getString(R.string.DocumentsDeleteConfirm, entry.name))
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        delete(entry);
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    private void delete(Entry entry) {
        if (entry.kind == Entry.FOLDER && hasAnyChild(entry.file)) {
            Helper.makeText(this, getString(R.string.DocumentsDeleteFolderNotEmpty),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (entry.file.delete()) {
            Helper.makeText(this, getString(R.string.DocumentsDeleted), Toast.LENGTH_SHORT).show();
        } else {
            Helper.makeText(this, getString(R.string.Failed), Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private static boolean hasAnyChild(File dir) {
        String[] names = dir.list();
        if (names == null) {
            return false;
        }
        for (String name : names) {
            if (!name.equals(".nomedia")) {
                return true;
            }
        }
        return false;
    }

    private void askNewFolder() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.DocumentsFolderName);
        new AlertDialog.Builder(this)
                .setTitle(R.string.DocumentsNewFolder)
                .setView(input)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createFolder(input.getText().toString());
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    private void createFolder(String rawName) {
        String name = safeName(rawName);
        if (name.isEmpty()) {
            return;
        }
        File dir = new File(destinationFolder(), name);
        if (!dir.exists() && dir.mkdirs()) {
            Helper.makeText(this, getString(R.string.DocumentsFolderCreated), Toast.LENGTH_SHORT).show();
        } else {
            Helper.makeText(this, getString(R.string.DocumentsFolderFailed), Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private void askMove(final Entry entry) {
        final ArrayList<File> targets = new ArrayList<File>();
        final ArrayList<String> labels = new ArrayList<String>();
        File parent = entry.file.getParentFile();
        if (parent != null && !parent.equals(mUserDir)) {
            targets.add(mUserDir);
            labels.add(getString(R.string.DocumentsMoveRoot));
        }
        File[] children = mUserDir.listFiles();
        if (children != null) {
            ArrayList<File> dirs = new ArrayList<File>();
            for (File child : children) {
                if (child.isDirectory() && !child.getName().startsWith(".") && !child.equals(parent)) {
                    dirs.add(child);
                }
            }
            Collections.sort(dirs, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            for (File dir : dirs) {
                targets.add(dir);
                labels.add(dir.getName());
            }
        }
        if (targets.isEmpty()) {
            Helper.makeText(this, getString(R.string.DocumentsMoveNoFolders), Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.DocumentsMoveTo)
                .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File target = new File(targets.get(which), entry.file.getName());
                        if (!target.exists() && entry.file.renameTo(target)) {
                            Helper.makeText(DocumentsActivity.this, getString(R.string.DocumentsMoved),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Helper.makeText(DocumentsActivity.this, getString(R.string.DocumentsMoveFailed),
                                    Toast.LENGTH_SHORT).show();
                        }
                        refresh();
                    }
                })
                .show();
    }

    // ------------------------------------------------------------------ sharing

    private void shareFolder(Entry entry) {
        ArrayList<File> files = new ArrayList<File>();
        collectFiles(entry.file, files);
        if (files.isEmpty()) {
            Helper.makeText(this, getString(R.string.DocumentsFolderEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        shareFiles(files, entry.name);
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, out);
            } else if (!child.getName().startsWith(".") && isSupported(child.getName())) {
                out.add(child);
            }
        }
    }

    private void shareFiles(List<File> files, String title) {
        try {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            for (File f : files) {
                uris.add(FileProvider.getUriForFile(this, getPackageName() + ".provider.file", f));
            }
            Intent intent;
            if (uris.size() == 1) {
                intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                intent.setType(mimeType(files.get(0).getName()));
            } else {
                intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                intent.setType("*/*");
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, title));
        } catch (Throwable t) {
            Helper.makeText(this, getString(R.string.Failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Share a snapshot of user.db. The live database may be open, possibly with
     * a write-ahead log, so flush it and copy to the cache folder first.
     */
    private void shareUserData(final Entry entry) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File copy = new File(new File(getCacheDir(), "share"), USER_DB);
                final boolean ok = snapshotDatabase(entry.file, copy);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        if (ok) {
                            shareFiles(Collections.singletonList(copy), entry.name);
                        } else {
                            Helper.makeText(DocumentsActivity.this, getString(R.string.Failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }, "DocumentsUserData").start();
    }

    private boolean snapshotDatabase(File src, File dst) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(src.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            Cursor c = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null);
            c.moveToFirst();
            c.close();
        } catch (Throwable ignored) {
            // not in WAL mode or busy; the plain copy below is still the best we have
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                    // nothing to do
                }
            }
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            File dir = dst.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                return false;
            }
            in = new java.io.FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Throwable ignored) {
                // nothing to do
            }
        }
    }

    // ------------------------------------------------------------------- import

    private void importFile(final Uri uri) {
        final String name = safeName(displayName(uri));
        if (name.isEmpty() || !isSupported(name)) {
            Helper.makeText(this, getString(R.string.DocumentsImportUnsupported), Toast.LENGTH_LONG).show();
            return;
        }
        final File destDir = destinationFolder();
        final boolean toRoot = !destDir.equals(mFolder);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = copy(uri, new File(destDir, name));
                final boolean done = ok;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        Helper.makeText(DocumentsActivity.this,
                                getString(done ? R.string.DocumentsImportDone : R.string.DocumentsImportFailed),
                                Toast.LENGTH_SHORT).show();
                        if (done) {
                            if (toRoot) {
                                mFolder = null;
                            }
                            mFilter = FILTER_USER;
                            mFilterSpinner.setSelection(mFilterKeys.indexOf(FILTER_USER), false);
                        }
                        refresh();
                    }
                });
            }
        }, "DocumentsImport").start();
    }

    private boolean copy(Uri uri, File target) {
        File part = new File(target.getPath() + ".part");
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return false;
            }
            out = new FileOutputStream(part);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.close();
            out = null;
            if (target.exists() && !target.delete()) {
                return false;
            }
            return part.renameTo(target);
        } catch (Throwable t) {
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Throwable ignored) {
                // nothing more to do
            }
            if (part.exists()) {
                part.delete();
            }
        }
    }

    private String displayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null) {
                    return name;
                }
            }
        } catch (Throwable ignored) {
            // fall through to the last path segment
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String last = uri.getLastPathSegment();
        return last == null ? "" : last;
    }

    // ------------------------------------------------------- user.db restore

    private void askRestoreUserData(final Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.DocumentsRestoreUserData)
                .setMessage(R.string.DocumentsRestoreConfirm)
                .setPositiveButton(R.string.DocumentsRestore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restoreUserData(uri);
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    /**
     * Replace user.db with the picked file. The file is copied beside user.db
     * and checked first, so a bad pick never touches the current data.
     */
    private void restoreUserData(final Uri uri) {
        final File db = new File(Helper.getExternalFolder(this), USER_DB);
        final File staged = new File(db.getParentFile(), USER_DB + ".import");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int result = R.string.DocumentsRestoreFailed;
                if (copy(uri, staged)) {
                    if (!isUserDatabase(staged)) {
                        result = R.string.DocumentsRestoreInvalid;
                    } else if (swapUserDatabase(staged, db)) {
                        result = R.string.DocumentsRestoreDone;
                    }
                }
                if (staged.exists()) {
                    staged.delete();
                }
                final int message = result;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        Helper.makeText(DocumentsActivity.this, getString(message),
                                Toast.LENGTH_LONG).show();
                        refresh();
                    }
                });
            }
        }, "DocumentsRestore").start();
    }

    /** True when the file is a healthy SQLite database holding Avare user data. */
    private static boolean isUserDatabase(File file) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            if (db.getVersion() > UserDatabaseHelper.DB_VERSION) {
                return false; // saved by a newer Avare, which this one cannot read
            }
            c = db.rawQuery("PRAGMA quick_check(1)", null);
            if (!c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) {
                return false;
            }
            c.close();
            c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            List<String> tables = Arrays.asList(
                    UserContract.TABLE_PLAN, UserContract.TABLE_LIST, UserContract.TABLE_WNB,
                    UserContract.TABLE_RECENT, UserContract.TABLE_TAG, UserContract.TABLE_DRAW,
                    UserContract.TABLE_AIRCRAFT);
            while (c.moveToNext()) {
                if (tables.contains(c.getString(0))) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false; // not a database at all
        } finally {
            if (c != null) {
                c.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Put the staged database in place of user.db. The provider holds user.db
     * open, so it is closed first and reopened on the new file afterwards. The
     * old write-ahead log must go too, or it would be applied to the new file.
     */
    private boolean swapUserDatabase(File staged, File db) {
        resetUserProvider();
        boolean ok = staged.renameTo(db);
        String path = db.getPath();
        new File(path + "-wal").delete();
        new File(path + "-shm").delete();
        new File(path + "-journal").delete();
        resetUserProvider();
        return ok;
    }

    private void resetUserProvider() {
        ContentProviderClient client = null;
        try {
            client = getContentResolver().acquireContentProviderClient(UserContract.AUTHORITY_URI);
            if (client != null && client.getLocalContentProvider() instanceof UserProvider) {
                ((UserProvider) client.getLocalContentProvider()).resetDatabase();
            }
        } catch (Throwable ignored) {
            // the provider reopens on its own the next time it fails
        } finally {
            if (client != null) {
                client.release();
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String safeName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.trim().replace('/', '_').replace('\\', '_');
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        return name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.US);
    }

    private static boolean isSupported(String name) {
        String ext = extension(name);
        return "pdf".equals(ext)
                || Arrays.asList(TEXT_EXTENSIONS).contains(ext)
                || Arrays.asList(PICTURE_EXTENSIONS).contains(ext);
    }

    private static String mimeType(String name) {
        String ext = extension(name);
        if ("pdf".equals(ext)) {
            return "application/pdf";
        }
        if ("txt".equals(ext)) {
            return "text/plain";
        }
        if ("kml".equals(ext)) {
            return "application/vnd.google-earth.kml+xml";
        }
        if ("json".equals(ext) || "geojson".equals(ext)) {
            return "application/json";
        }
        if (Arrays.asList(PICTURE_EXTENSIONS).contains(ext)) {
            return "image/*";
        }
        return "*/*";
    }

    private Set<String> assetNames(String dir) {
        try {
            String[] names = getAssets().list(dir);
            if (names != null) {
                return new HashSet<String>(Arrays.asList(names));
            }
        } catch (Throwable ignored) {
            // no covers, the plain icons are used
        }
        return new HashSet<String>();
    }

    /** Asset path of the cover picture for a folder, or null. Same names avarex uses. */
    private String coverFor(File folder) {
        String key = folder.getName().trim().toLowerCase(Locale.US).replace('_', ' ').replaceAll("\\s+", " ");
        String asset = null;
        switch (key) {
            case "aim":
            case "faa aim":
                asset = "folder_covers/aim.jpg";
                break;
            case "cfr14":
            case "far":
            case "14 cfr":
            case "faa 14 cfr":
                asset = "folder_covers/far.jpg";
                break;
            case "handbooks":
            case "faa handbooks":
                asset = "pdf_thumbnails/PHAK_Pilots_Handbook_of_Aeronautical_Knowledge.png";
                break;
            case "acs":
            case "faa acs":
                asset = "folder_covers/acs.jpg";
                break;
            case "ac":
            case "faa ac":
            case "advisory circulars":
            case "faa advisory circulars":
                asset = "folder_covers/ac.jpg";
                break;
            case "notes":
                asset = "folder_covers/notes.jpg";
                break;
            case "tracks":
                asset = "folder_covers/tracks.jpg";
                break;
            default:
                break;
        }
        if (asset == null) {
            return null;
        }
        String[] parts = asset.split("/");
        Set<String> known = parts[0].equals("folder_covers") ? mCovers : mThumbnails;
        return known.contains(parts[1]) ? asset : null;
    }

    /** Asset path of the first-page thumbnail for a bundled FAA PDF, or null. */
    private String thumbnailFor(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String png = fileName.substring(0, dot) + ".png";
        return mThumbnails.contains(png) ? "pdf_thumbnails/" + png : null;
    }

    // ------------------------------------------------------------------ adapter

    private final class GridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(DocumentsActivity.this)
                        .inflate(R.layout.documents_item, parent, false);
            }
            Entry entry = mEntries.get(position);
            ImageView image = row.findViewById(R.id.documents_item_image);
            TextView name = row.findViewById(R.id.documents_item_name);
            TextView badge = row.findViewById(R.id.documents_item_badge);

            row.setBackgroundResource(entry.kind == Entry.FOLDER
                    ? R.drawable.documents_folder_bg : R.drawable.documents_card_bg);
            name.setText(entry.name);
            badge.setVisibility(View.GONE);

            if (entry.kind == Entry.FOLDER) {
                String cover = coverFor(entry.file);
                if (cover != null) {
                    ImageLoader.showAsset(DocumentsActivity.this, cover, image, 400, R.drawable.ic_folder);
                } else {
                    setIcon(image, R.drawable.ic_folder);
                }
            } else if (entry.kind == Entry.USER_DATA) {
                badge.setText("DB");
                badge.setVisibility(View.VISIBLE);
                if (mCovers.contains("user_data.jpg")) {
                    ImageLoader.showAsset(DocumentsActivity.this, "folder_covers/user_data.jpg", image,
                            400, R.drawable.ic_document);
                } else {
                    setIcon(image, R.drawable.ic_document);
                }
            } else if (entry.kind == Entry.WEB) {
                ImageLoader.showUrl(DocumentsActivity.this, entry.url, image, 400, R.drawable.ic_document);
            } else {
                String ext = extension(entry.name);
                badge.setText(ext.toUpperCase(Locale.US));
                badge.setVisibility(View.VISIBLE);
                String thumb = "pdf".equals(ext) ? thumbnailFor(entry.name) : null;
                if (thumb != null) {
                    ImageLoader.showAsset(DocumentsActivity.this, thumb, image, 400, R.drawable.ic_document);
                } else if (Arrays.asList(PICTURE_EXTENSIONS).contains(ext)) {
                    ImageLoader.showFile(entry.file, image, 400, R.drawable.ic_document);
                } else {
                    setIcon(image, R.drawable.ic_document);
                }
            }
            return row;
        }

        private void setIcon(ImageView image, int res) {
            image.setTag(null); // ignore any picture still loading for a recycled tile
            image.setImageResource(res);
        }
    }
}

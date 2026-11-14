/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ds.avare.logbook.LogEntry;
import com.ds.avare.logbook.LogbookStore;
import com.ds.avare.utils.Helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Flight log book: totals, the list of entries, and CSV import and export.
 */
public class LogbookActivity extends AppCompatActivity {

    private final List<LogEntry> mEntries = new ArrayList<LogEntry>();
    private TextView mHours;
    private TextView mLandings;
    private TextView mApproaches;
    private TextView mCount;
    private View mEmpty;
    private BaseAdapter mAdapter;

    private final ActivityResultLauncher<String[]> mImport =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    new androidx.activity.result.ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            if (uri != null) {
                                importCsv(uri);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF4F6F8);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(0xFF1A3650);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        Button back = chrome(getString(R.string.KmlBack));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        titleRow.addView(back);
        TextView title = new TextView(this);
        title.setText(R.string.LogBookTitle);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(12, 0, 8, 0);
        title.setSingleLine(true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(titleRow);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button imp = chrome(getString(R.string.LogBookImport));
        imp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImport.launch(new String[]{"text/*", "text/csv", "text/comma-separated-values", "*/*"});
            }
        });
        Button exp = chrome(getString(R.string.LogBookExport));
        exp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportCsv();
            }
        });
        Button info = chrome("?");
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(LogbookActivity.this)
                        .setMessage(R.string.LogBookInfo)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actions.addView(imp, actionLp);
        actions.addView(exp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(actions);
        root.addView(bar);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(16, 16, 16, 8);
        stats.setBackgroundColor(0xFFFFFFFF);
        mHours = stat(stats, getString(R.string.LogBookHours));
        mLandings = stat(stats, getString(R.string.LogBookLandings));
        mApproaches = stat(stats, getString(R.string.LogBookApproaches));
        Button dash = new Button(this);
        dash.setText(R.string.LogBookDashboard);
        dash.setAllCaps(false);
        dash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LogbookActivity.this, LogbookDashboardActivity.class));
            }
        });
        stats.addView(dash);
        root.addView(stats, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mCount = new TextView(this);
        mCount.setPadding(24, 16, 24, 8);
        mCount.setTextColor(0xFF616161);
        root.addView(mCount);

        mEmpty = emptyView();
        root.addView(mEmpty);

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        mAdapter = new BaseAdapter() {
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
                LinearLayout row = new LinearLayout(LogbookActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(24, 20, 24, 20);
                row.setBackgroundColor(0xFFFFFFFF);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(16, 8, 16, 0);
                row.setLayoutParams(lp);

                LogEntry e = mEntries.get(position);
                TextView hours = new TextView(LogbookActivity.this);
                hours.setText(String.format(Locale.US, "%.1f\nhrs", e.totalFlightTime));
                hours.setGravity(Gravity.CENTER);
                hours.setTextColor(0xFF1565C0);
                hours.setTypeface(Typeface.DEFAULT_BOLD);
                hours.setWidth(dp(64));
                row.addView(hours);

                LinearLayout mid = new LinearLayout(LogbookActivity.this);
                mid.setOrientation(LinearLayout.VERTICAL);
                TextView name = new TextView(LogbookActivity.this);
                String tail = e.aircraftIdentification.isEmpty() ? "" : " (" + e.aircraftIdentification + ")";
                name.setText(e.aircraftMakeModel + tail);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                name.setTextColor(0xFF212121);
                mid.addView(name);
                TextView sub = new TextView(LogbookActivity.this);
                String route = e.route.isEmpty() ? "" : "   " + e.route;
                sub.setText(e.dateLabel() + route);
                sub.setTextColor(0xFF757575);
                sub.setTextSize(13);
                mid.addView(sub);
                row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView land = new TextView(LogbookActivity.this);
                land.setText(String.format(Locale.US, "D:%d N:%d\nIAP:%d",
                        e.dayLandings, e.nightLandings, e.instrumentApproaches));
                land.setGravity(Gravity.END);
                land.setTextSize(12);
                land.setTextColor(0xFF616161);
                row.addView(land);
                return row;
            }
        };
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                open(mEntries.get(position));
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button add = new Button(this);
        add.setText(R.string.LogBookAdd);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(null);
            }
        });
        root.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        Helper.applySystemBarInsets(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        mEntries.clear();
        mEntries.addAll(LogbookStore.all(this));
        double hours = 0;
        int landings = 0;
        int approaches = 0;
        for (LogEntry e : mEntries) {
            hours += e.totalFlightTime;
            landings += e.dayLandings + e.nightLandings;
            approaches += e.instrumentApproaches;
        }
        mHours.setText(String.format(Locale.US, "%.1f", hours));
        mLandings.setText(String.valueOf(landings));
        mApproaches.setText(String.valueOf(approaches));
        mCount.setText(getString(R.string.LogBookCount, mEntries.size()));
        mEmpty.setVisibility(mEntries.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter.notifyDataSetChanged();
    }

    private void open(LogEntry entry) {
        Intent intent = new Intent(this, LogEntryActivity.class);
        if (entry != null) {
            intent.putExtra(LogEntryActivity.EXTRA_ID, entry.id);
        }
        startActivity(intent);
    }

    private void exportCsv() {
        if (mEntries.isEmpty()) {
            Helper.makeText(this, getString(R.string.LogBookExportEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getCacheDir(), "share");
            dir.mkdirs();
            File file = new File(dir, "logbook.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(LogbookStore.toCsv(mEntries).getBytes(StandardCharsets.UTF_8));
            out.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider.file", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.LogBookExport)));
        } catch (Throwable t) {
            Helper.makeText(this, getString(R.string.LogBookExportFailed), Toast.LENGTH_SHORT).show();
        }
    }

    private void importCsv(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String text = read(uri);
                final int saved = text == null ? -1 : LogbookStore.importCsv(LogbookActivity.this, text);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        if (saved < 0) {
                            Helper.makeText(LogbookActivity.this, getString(R.string.LogBookImportFailed), Toast.LENGTH_LONG).show();
                        } else {
                            Helper.makeText(LogbookActivity.this, getString(R.string.LogBookImportDone, saved), Toast.LENGTH_LONG).show();
                            reload();
                        }
                    }
                });
            }
        }, "LogbookImport").start();
    }

    private String read(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private Button chrome(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(0x00000000);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        return button;
    }

    private TextView stat(LinearLayout row, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(12, 0, 12, 0);
        TextView value = new TextView(this);
        value.setText("0");
        value.setTextSize(18);
        value.setTypeface(Typeface.DEFAULT_BOLD);
        value.setGravity(Gravity.CENTER);
        value.setTextColor(0xFF212121);
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextSize(11);
        caption.setGravity(Gravity.CENTER);
        caption.setTextColor(0xFF757575);
        col.addView(value);
        col.addView(caption);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return value;
    }

    private View emptyView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(32, 48, 32, 24);
        TextView text = new TextView(this);
        text.setText(R.string.LogBookEmpty);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(0xFF757575);
        box.addView(text);
        Button add = new Button(this);
        add.setText(R.string.LogBookAddFirst);
        add.setAllCaps(false);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(null);
            }
        });
        box.addView(add);
        return box;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

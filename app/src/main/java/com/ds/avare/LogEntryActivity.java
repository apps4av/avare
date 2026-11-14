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
import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.avare.logbook.LogEntry;
import com.ds.avare.logbook.LogbookStore;
import com.ds.avare.utils.Helper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Create or edit one logbook entry. Field names match the AvareX log book.
 */
public class LogEntryActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    private LogEntry mExisting;
    private final Map<String, EditText> mFields = new HashMap<String, EditText>();
    private Button mDateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra(EXTRA_ID);
        if (id != null) {
            List<LogEntry> all = LogbookStore.all(this);
            for (LogEntry e : all) {
                if (id.equals(e.id)) {
                    mExisting = e;
                    break;
                }
            }
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF4F6F8);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFF1A3650);
        bar.setPadding(8, 8, 8, 8);
        Button back = new Button(this);
        back.setText(R.string.KmlBack);
        back.setTextColor(0xFFFFFFFF);
        back.setBackgroundColor(0x00000000);
        back.setAllCaps(false);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back);
        TextView title = new TextView(this);
        title.setText(mExisting == null ? R.string.LogBookNew : R.string.LogBookEdit);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(12, 0, 0, 0);
        bar.addView(title);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 16, 24, 32);

        section(form, getString(R.string.LogBookSectionFlight));
        mDateButton = new Button(this);
        mDateButton.setAllCaps(false);
        mDateButton.setTextColor(0xFF212121);
        mDateButton.setBackgroundColor(0xFFFFFFFF);
        final Calendar cal = Calendar.getInstance();
        if (mExisting != null && mExisting.dateLabel().length() >= 10) {
            mDateButton.setText(mExisting.dateLabel());
        } else {
            mDateButton.setText(String.format(Locale.US, "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));
        }
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                String current = mDateButton.getText().toString();
                if (current.length() >= 10) {
                    try {
                        year = Integer.parseInt(current.substring(0, 4));
                        month = Integer.parseInt(current.substring(5, 7)) - 1;
                        day = Integer.parseInt(current.substring(8, 10));
                    } catch (Exception ignored) {
                    }
                }
                new DatePickerDialog(LogEntryActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int y, int m, int d) {
                        mDateButton.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
                    }
                }, year, month, day).show();
            }
        });
        form.addView(label(getString(R.string.LogBookDate)));
        form.addView(mDateButton);
        text(form, "aircraftIdentification", getString(R.string.LogBookTail), false);
        text(form, "aircraftMakeModel", getString(R.string.LogBookType), false);
        text(form, "route", getString(R.string.LogBookRoute), false);

        section(form, getString(R.string.LogBookSectionTime));
        number(form, "totalFlightTime", getString(R.string.LogBookTotal));
        number(form, "dayTime", getString(R.string.LogBookDay));
        number(form, "nightTime", getString(R.string.LogBookNight));
        number(form, "crossCountryTime", getString(R.string.LogBookCrossCountry));
        number(form, "soloTime", getString(R.string.LogBookSolo));

        section(form, getString(R.string.LogBookSectionPilot));
        number(form, "pilotInCommand", getString(R.string.LogBookPic));
        number(form, "copilot", getString(R.string.LogBookSic));
        number(form, "dualReceived", getString(R.string.LogBookDual));
        number(form, "instructor", getString(R.string.LogBookInstructorTime));
        number(form, "examiner", getString(R.string.LogBookExaminer));

        section(form, getString(R.string.LogBookSectionInstrument));
        number(form, "actualInstruments", getString(R.string.LogBookActual));
        number(form, "simulatedInstruments", getString(R.string.LogBookSimulated));
        whole(form, "instrumentApproaches", getString(R.string.LogBookApproachesField));
        number(form, "holdingProcedures", getString(R.string.LogBookHolds));

        section(form, getString(R.string.LogBookSectionLandings));
        whole(form, "dayLandings", getString(R.string.LogBookDayLandings));
        whole(form, "nightLandings", getString(R.string.LogBookNightLandings));

        section(form, getString(R.string.LogBookSectionTraining));
        number(form, "groundTime", getString(R.string.LogBookGround));
        number(form, "flightSimulator", getString(R.string.LogBookSimulator));
        text(form, "instructorName", getString(R.string.LogBookInstructorName), false);
        text(form, "instructorCertificate", getString(R.string.LogBookInstructorCert), false);

        section(form, getString(R.string.LogBookSectionRemarks));
        text(form, "remarks", getString(R.string.LogBookRemarks), true);

        Button save = new Button(this);
        save.setText(R.string.LogBookSave);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundColor(0xFF1565C0);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        form.addView(save);

        if (mExisting != null) {
            Button delete = new Button(this);
            delete.setText(R.string.LogBookDelete);
            delete.setTextColor(0xFFC62828);
            delete.setBackgroundColor(0xFFFFFFFF);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete();
                }
            });
            form.addView(delete);
        }

        fill(mExisting);
        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        Helper.applySystemBarInsets(this);
    }

    private void fill(LogEntry e) {
        if (e == null) {
            put("totalFlightTime", "0");
            put("dayTime", "0");
            put("nightTime", "0");
            put("crossCountryTime", "0");
            put("soloTime", "0");
            put("pilotInCommand", "0");
            put("copilot", "0");
            put("dualReceived", "0");
            put("instructor", "0");
            put("examiner", "0");
            put("actualInstruments", "0");
            put("simulatedInstruments", "0");
            put("instrumentApproaches", "0");
            put("holdingProcedures", "0");
            put("dayLandings", "0");
            put("nightLandings", "0");
            put("groundTime", "0");
            put("flightSimulator", "0");
            return;
        }
        put("aircraftIdentification", e.aircraftIdentification);
        put("aircraftMakeModel", e.aircraftMakeModel);
        put("route", e.route);
        put("totalFlightTime", num(e.totalFlightTime));
        put("dayTime", num(e.dayTime));
        put("nightTime", num(e.nightTime));
        put("crossCountryTime", num(e.crossCountryTime));
        put("soloTime", num(e.soloTime));
        put("pilotInCommand", num(e.pilotInCommand));
        put("copilot", num(e.copilot));
        put("dualReceived", num(e.dualReceived));
        put("instructor", num(e.instructor));
        put("examiner", num(e.examiner));
        put("actualInstruments", num(e.actualInstruments));
        put("simulatedInstruments", num(e.simulatedInstruments));
        put("instrumentApproaches", Integer.toString(e.instrumentApproaches));
        put("holdingProcedures", num(e.holdingProcedures));
        put("dayLandings", Integer.toString(e.dayLandings));
        put("nightLandings", Integer.toString(e.nightLandings));
        put("groundTime", num(e.groundTime));
        put("flightSimulator", num(e.flightSimulator));
        put("instructorName", e.instructorName);
        put("instructorCertificate", e.instructorCertificate);
        put("remarks", e.remarks);
    }

    private void save() {
        String type = value("aircraftMakeModel").trim();
        if (type.isEmpty()) {
            Helper.makeText(this, getString(R.string.LogBookTypeRequired), Toast.LENGTH_SHORT).show();
            return;
        }
        for (Map.Entry<String, EditText> field : mFields.entrySet()) {
            if (field.getValue().getInputType() == InputType.TYPE_CLASS_NUMBER
                    || (field.getValue().getInputType() & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
                String raw = field.getValue().getText().toString().trim();
                if (raw.isEmpty()) {
                    field.getValue().setText("0");
                    continue;
                }
                try {
                    Double.parseDouble(raw);
                } catch (NumberFormatException ex) {
                    Helper.makeText(this, getString(R.string.LogBookNumberRequired), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        LogEntry entry = mExisting == null ? new LogEntry() : mExisting;
        if (entry.id == null || entry.id.isEmpty()) {
            entry.id = Long.toString(System.currentTimeMillis());
        }
        entry.date = mDateButton.getText().toString();
        entry.aircraftMakeModel = type;
        entry.aircraftIdentification = value("aircraftIdentification").trim();
        entry.route = value("route").trim();
        entry.totalFlightTime = numOf("totalFlightTime");
        entry.dayTime = numOf("dayTime");
        entry.nightTime = numOf("nightTime");
        entry.crossCountryTime = numOf("crossCountryTime");
        entry.soloTime = numOf("soloTime");
        entry.pilotInCommand = numOf("pilotInCommand");
        entry.copilot = numOf("copilot");
        entry.dualReceived = numOf("dualReceived");
        entry.instructor = numOf("instructor");
        entry.examiner = numOf("examiner");
        entry.actualInstruments = numOf("actualInstruments");
        entry.simulatedInstruments = numOf("simulatedInstruments");
        entry.instrumentApproaches = (int) numOf("instrumentApproaches");
        entry.holdingProcedures = numOf("holdingProcedures");
        entry.dayLandings = (int) numOf("dayLandings");
        entry.nightLandings = (int) numOf("nightLandings");
        entry.groundTime = numOf("groundTime");
        entry.flightSimulator = numOf("flightSimulator");
        entry.instructorName = value("instructorName").trim();
        entry.instructorCertificate = value("instructorCertificate").trim();
        entry.remarks = value("remarks").trim();
        boolean ok = mExisting == null ? LogbookStore.insert(this, entry) : LogbookStore.update(this, entry);
        if (!ok && mExisting == null) {
            ok = LogbookStore.update(this, entry);
        }
        if (ok) {
            finish();
        } else {
            Helper.makeText(this, getString(R.string.KmlLogFailed), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.LogBookDeleteConfirm)
                .setPositiveButton(R.string.LogBookDelete, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        LogbookStore.delete(LogEntryActivity.this, mExisting.id);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void section(LinearLayout form, String title) {
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor(0xFF1565C0);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(16);
        text.setPadding(0, 28, 0, 8);
        form.addView(text);
    }

    private TextView label(String title) {
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor(0xFF616161);
        text.setPadding(0, 8, 0, 0);
        return text;
    }

    private void text(LinearLayout form, String key, String title, boolean multi) {
        form.addView(label(title));
        EditText edit = field();
        edit.setSingleLine(!multi);
        if (multi) {
            edit.setMinLines(3);
            edit.setGravity(Gravity.TOP);
        }
        form.addView(edit);
        mFields.put(key, edit);
    }

    private void number(LinearLayout form, String key, String title) {
        form.addView(label(title));
        EditText edit = field();
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit.setSingleLine(true);
        form.addView(edit);
        mFields.put(key, edit);
    }

    private void whole(LinearLayout form, String key, String title) {
        form.addView(label(title));
        EditText edit = field();
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setSingleLine(true);
        form.addView(edit);
        mFields.put(key, edit);
    }

    /** Dark text on a white field. The activity theme is dark, so an unstyled field is white on white. */
    private EditText field() {
        EditText edit = new EditText(this);
        edit.setTextColor(0xFF212121);
        edit.setHintTextColor(0xFF9E9E9E);
        edit.setBackgroundColor(0xFFFFFFFF);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        edit.setPadding(pad, pad, pad, pad);
        return edit;
    }

    private void put(String key, String value) {
        EditText edit = mFields.get(key);
        if (edit != null) {
            edit.setText(value == null ? "" : value);
        }
    }

    private String value(String key) {
        EditText edit = mFields.get(key);
        return edit == null || edit.getText() == null ? "" : edit.getText().toString();
    }

    private double numOf(String key) {
        try {
            return Double.parseDouble(value(key).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String num(double v) {
        if (v == Math.rint(v)) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }
}

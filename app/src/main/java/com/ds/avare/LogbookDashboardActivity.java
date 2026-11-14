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

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.avare.logbook.LogEntry;
import com.ds.avare.logbook.LogbookStore;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.BarChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Totals, currency, and bar charts for the log book, with year, type, and tail filters.
 */
public class LogbookDashboardActivity extends AppCompatActivity {

    private static final String[] REMARKS = {"Check Ride", "IPC", "Flight Review", "Favorite"};

    private List<LogEntry> mAll = new ArrayList<LogEntry>();
    private final TreeSet<String> mYears = new TreeSet<String>();
    private final TreeSet<String> mTypes = new TreeSet<String>();
    private final TreeSet<String> mTails = new TreeSet<String>();
    private final TreeSet<String> mRemarks = new TreeSet<String>();
    private LinearLayout mBody;
    private boolean mFiltersOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAll = LogbookStore.all(this);

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
        title.setText(R.string.LogBookDashboardTitle);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(12, 0, 0, 0);
        bar.addView(title);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        mBody = new LinearLayout(this);
        mBody.setOrientation(LinearLayout.VERTICAL);
        mBody.setPadding(24, 16, 24, 48);
        scroll.addView(mBody);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        Helper.applySystemBarInsets(this);
        rebuild();
    }

    private void rebuild() {
        mBody.removeAllViews();
        List<LogEntry> filtered = new ArrayList<LogEntry>();
        for (LogEntry e : mAll) {
            if (matches(e)) {
                filtered.add(e);
            }
        }
        mBody.addView(currencyCard());
        mBody.addView(summaryCard(filtered));
        mBody.addView(filterCard());
        chart(mBody, getString(R.string.LogBookChartYear), hoursByYear(filtered), 0xFF1565C0);
        chart(mBody, getString(R.string.LogBookChartType), hoursBy(filtered, true), 0xFF00897B);
        chart(mBody, getString(R.string.LogBookChartTail), hoursBy(filtered, false), 0xFF3949AB);
        chart(mBody, getString(R.string.LogBookChartCategory), hoursByCategory(filtered), 0xFF6A1B9A);
        chart(mBody, getString(R.string.LogBookChartProcedures), procedures(filtered), 0xFFEF6C00);
        mBody.addView(flightList(filtered));
    }

    private boolean matches(LogEntry e) {
        if (!mYears.isEmpty() && !mYears.contains(String.valueOf(e.year()))) {
            return false;
        }
        if (!mTypes.isEmpty() && !mTypes.contains(e.aircraftMakeModel)) {
            return false;
        }
        if (!mTails.isEmpty() && !mTails.contains(e.aircraftIdentification)) {
            return false;
        }
        if (!mRemarks.isEmpty()) {
            boolean any = false;
            for (String remark : mRemarks) {
                if (e.remarks != null && e.remarks.contains(remark)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        return true;
    }

    private View currencyCard() {
        long now = System.currentTimeMillis();
        long d90 = now - 90L * 24 * 60 * 60 * 1000;
        long d180 = now - 180L * 24 * 60 * 60 * 1000;
        int day = 0;
        int night = 0;
        int approaches = 0;
        for (LogEntry e : mAll) {
            long when = e.epochMs();
            if (when >= d90) {
                day += e.dayLandings;
                night += e.nightLandings;
            }
            if (when >= d180) {
                approaches += e.instrumentApproaches;
            }
        }
        LinearLayout card = card();
        card.addView(heading(getString(R.string.LogBookCurrency)));
        LinearLayout row = row();
        row.addView(currencyTile(getString(R.string.LogBookCurrencyDay), day + "/3", day >= 3));
        row.addView(currencyTile(getString(R.string.LogBookCurrencyNight), night + "/3", night >= 3));
        row.addView(currencyTile(getString(R.string.LogBookCurrencyIfr), approaches + "/6", approaches >= 6));
        card.addView(row);
        return card;
    }

    private View summaryCard(List<LogEntry> entries) {
        double total = 0, pic = 0, night = 0, xc = 0;
        int landings = 0, approaches = 0;
        for (LogEntry e : entries) {
            total += e.totalFlightTime;
            pic += e.pilotInCommand;
            night += e.nightTime;
            xc += e.crossCountryTime;
            landings += e.dayLandings + e.nightLandings;
            approaches += e.instrumentApproaches;
        }
        LinearLayout card = card();
        card.addView(heading(getString(R.string.LogBookSummary, entries.size())));
        LinearLayout row1 = row();
        row1.addView(tile(getString(R.string.LogBookTotalShort), String.format(Locale.US, "%.1f", total)));
        row1.addView(tile(getString(R.string.LogBookPic), String.format(Locale.US, "%.1f", pic)));
        row1.addView(tile(getString(R.string.LogBookNight), String.format(Locale.US, "%.1f", night)));
        card.addView(row1);
        LinearLayout row2 = row();
        row2.addView(tile(getString(R.string.LogBookCrossCountry), String.format(Locale.US, "%.1f", xc)));
        row2.addView(tile(getString(R.string.LogBookLandings), String.valueOf(landings)));
        row2.addView(tile(getString(R.string.LogBookApproaches), String.valueOf(approaches)));
        card.addView(row2);
        return card;
    }

    private View filterCard() {
        LinearLayout card = card();
        LinearLayout head = row();
        TextView title = heading(getString(R.string.LogBookFilters));
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!mYears.isEmpty() || !mTypes.isEmpty() || !mTails.isEmpty() || !mRemarks.isEmpty()) {
            Button clear = new Button(this);
            clear.setText(R.string.LogBookClear);
            clear.setAllCaps(false);
            clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mYears.clear();
                    mTypes.clear();
                    mTails.clear();
                    mRemarks.clear();
                    rebuild();
                }
            });
            head.addView(clear);
        }
        Button toggle = new Button(this);
        toggle.setText(mFiltersOpen ? R.string.LogBookHide : R.string.LogBookShow);
        toggle.setAllCaps(false);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFiltersOpen = !mFiltersOpen;
                rebuild();
            }
        });
        head.addView(toggle);
        card.addView(head);
        if (mFiltersOpen) {
            card.addView(chips(getString(R.string.LogBookFilterYear), options(true, false, false), mYears));
            card.addView(chips(getString(R.string.LogBookFilterType), options(false, true, false), mTypes));
            card.addView(chips(getString(R.string.LogBookFilterTail), options(false, false, true), mTails));
            TreeSet<String> remarks = new TreeSet<String>();
            Collections.addAll(remarks, REMARKS);
            card.addView(chips(getString(R.string.LogBookFilterRemarks), remarks, mRemarks));
        }
        return card;
    }

    private TreeSet<String> options(boolean year, boolean type, boolean tail) {
        TreeSet<String> set = new TreeSet<String>();
        for (LogEntry e : mAll) {
            if (year && e.year() > 0) {
                set.add(String.valueOf(e.year()));
            }
            if (type && e.aircraftMakeModel != null && !e.aircraftMakeModel.isEmpty()) {
                set.add(e.aircraftMakeModel);
            }
            if (tail && e.aircraftIdentification != null && !e.aircraftIdentification.isEmpty()) {
                set.add(e.aircraftIdentification);
            }
        }
        return set;
    }

    private View chips(String title, TreeSet<String> options, final TreeSet<String> selected) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 8, 0, 8);
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(0xFF616161);
        box.addView(label);
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        for (final String option : options) {
            final Button chip = new Button(this);
            chip.setText(option);
            chip.setAllCaps(false);
            chip.setTextSize(12);
            styleChip(chip, selected.contains(option));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selected.contains(option)) {
                        selected.remove(option);
                    } else {
                        selected.add(option);
                    }
                    rebuild();
                }
            });
            row.addView(chip);
        }
        scroller.addView(row);
        box.addView(scroller);
        return box;
    }

    private void styleChip(Button chip, boolean on) {
        chip.setTextColor(on ? 0xFFFFFFFF : 0xFF1565C0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40f);
        bg.setColor(on ? 0xFF1565C0 : 0xFFE3F2FD);
        chip.setBackground(bg);
    }

    private void chart(LinearLayout parent, String title, Map<String, Double> data, int color) {
        List<String> labels = new ArrayList<String>();
        List<Float> values = new ArrayList<Float>();
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (e.getValue() > 0) {
                labels.add(e.getKey());
                values.add(e.getValue().floatValue());
            }
        }
        if (labels.isEmpty()) {
            return;
        }
        LinearLayout card = card();
        card.addView(heading(title));
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        BarChartView chart = new BarChartView(this);
        String[] ls = labels.toArray(new String[0]);
        float[] vs = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vs[i] = values.get(i);
        }
        chart.setData(ls, vs, color);
        scroller.addView(chart);
        card.addView(scroller);
        parent.addView(card);
    }

    private Map<String, Double> hoursByYear(List<LogEntry> entries) {
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        TreeSet<String> years = new TreeSet<String>();
        Map<String, Double> raw = new LinkedHashMap<String, Double>();
        for (LogEntry e : entries) {
            if (e.year() <= 0) {
                continue;
            }
            String year = String.valueOf(e.year());
            years.add(year);
            raw.put(year, (raw.containsKey(year) ? raw.get(year) : 0) + e.totalFlightTime);
        }
        for (String year : years) {
            map.put(year, raw.get(year));
        }
        return map;
    }

    private Map<String, Double> hoursBy(List<LogEntry> entries, boolean type) {
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        for (LogEntry e : entries) {
            String key = type ? e.aircraftMakeModel : e.aircraftIdentification;
            if (key == null || key.isEmpty()) {
                continue;
            }
            map.put(key, (map.containsKey(key) ? map.get(key) : 0) + e.totalFlightTime);
        }
        return map;
    }

    private Map<String, Double> hoursByCategory(List<LogEntry> entries) {
        double solo = 0, dual = 0, instructor = 0, examiner = 0, pic = 0, sic = 0;
        double day = 0, night = 0, actual = 0, simulated = 0, xc = 0, holds = 0, sim = 0, ground = 0;
        for (LogEntry e : entries) {
            solo += e.soloTime;
            dual += e.dualReceived;
            instructor += e.instructor;
            examiner += e.examiner;
            pic += e.pilotInCommand;
            sic += e.copilot;
            day += e.dayTime;
            night += e.nightTime;
            actual += e.actualInstruments;
            simulated += e.simulatedInstruments;
            xc += e.crossCountryTime;
            holds += e.holdingProcedures;
            sim += e.flightSimulator;
            ground += e.groundTime;
        }
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        map.put("Solo", solo);
        map.put("Dual", dual);
        map.put("CFI", instructor);
        map.put("Examiner", examiner);
        map.put("PIC", pic);
        map.put("SIC", sic);
        map.put("Day", day);
        map.put("Night", night);
        map.put("IMC", actual);
        map.put("Hood", simulated);
        map.put("XC", xc);
        map.put("Holds", holds);
        map.put("Sim", sim);
        map.put("Ground", ground);
        return map;
    }

    private Map<String, Double> procedures(List<LogEntry> entries) {
        double day = 0, night = 0, approaches = 0;
        for (LogEntry e : entries) {
            day += e.dayLandings;
            night += e.nightLandings;
            approaches += e.instrumentApproaches;
        }
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        map.put("Day ldg", day);
        map.put("Night ldg", night);
        map.put("Appr", approaches);
        return map;
    }

    private View flightList(List<LogEntry> entries) {
        LinearLayout card = card();
        card.addView(heading(getString(R.string.LogBookFlightLog, Math.min(entries.size(), 20))));
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.LogBookNoMatch);
            empty.setTextColor(0xFF757575);
            empty.setPadding(0, 12, 0, 12);
            card.addView(empty);
            return card;
        }
        int shown = Math.min(20, entries.size());
        for (int i = 0; i < shown; i++) {
            LogEntry e = entries.get(i);
            TextView line = new TextView(this);
            String tail = e.aircraftIdentification.isEmpty() ? "" : " (" + e.aircraftIdentification + ")";
            line.setText(String.format(Locale.US, "%.1f  %s%s\n%s  %s",
                    e.totalFlightTime, e.aircraftMakeModel, tail, e.dateLabel(), e.route));
            line.setPadding(0, 12, 0, 12);
            line.setTextColor(0xFF212121);
            card.addView(line);
        }
        if (entries.size() > 20) {
            TextView more = new TextView(this);
            more.setText(getString(R.string.LogBookMore, entries.size() - 20));
            more.setGravity(Gravity.CENTER);
            more.setTextColor(0xFF757575);
            card.addView(more);
        }
        return card;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView heading(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF1565C0);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(16);
        view.setPadding(0, 0, 0, 12);
        return view;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private View tile(String label, String value) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(4, 8, 4, 8);
        TextView v = new TextView(this);
        v.setText(value);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextSize(16);
        v.setTextColor(0xFF212121);
        TextView l = new TextView(this);
        l.setText(label);
        l.setGravity(Gravity.CENTER);
        l.setTextSize(11);
        l.setTextColor(0xFF757575);
        col.addView(v);
        col.addView(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(lp);
        return col;
    }

    private View currencyTile(String label, String value, boolean current) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(12, 16, 12, 16);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        bg.setColor(current ? 0x2232A852 : 0x22F9A825);
        bg.setStroke(2, current ? 0xFF2E7D32 : 0xFFEF6C00);
        col.setBackground(bg);
        TextView mark = new TextView(this);
        mark.setText(current ? getString(R.string.LogBookCurrent) : getString(R.string.LogBookNotCurrent));
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(current ? 0xFF2E7D32 : 0xFFEF6C00);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        TextView v = new TextView(this);
        v.setText(value);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        TextView l = new TextView(this);
        l.setText(label);
        l.setGravity(Gravity.CENTER);
        l.setTextSize(11);
        l.setTextColor(0xFF616161);
        col.addView(mark);
        col.addView(v);
        col.addView(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(6, 0, 6, 0);
        col.setLayoutParams(lp);
        return col;
    }
}

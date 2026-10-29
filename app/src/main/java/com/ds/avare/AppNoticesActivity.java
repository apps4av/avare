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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.ds.avare.utils.AppNoticeService;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.StoredAppNotice;

import java.util.ArrayList;
import java.util.List;

/**
 * Apps4Av messages saved on this device until the pilot deletes one.
 */
public class AppNoticesActivity extends BaseActivity {

    private List<StoredAppNotice> mNotices = new ArrayList<StoredAppNotice>();
    private NoticeAdapter mAdapter;
    private ListView mList;
    private TextView mEmpty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_notices);
        Helper.applySystemBarInsets(this);

        mList = (ListView) findViewById(R.id.app_notices_list);
        mEmpty = (TextView) findViewById(R.id.app_notices_empty);
        mAdapter = new NoticeAdapter();
        mList.setAdapter(mAdapter);
        reload();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressedExit();
    }

    private void reload() {
        mNotices = AppNoticeService.getInstance().saved();
        mAdapter.notifyDataSetChanged();
        boolean empty = mNotices.isEmpty();
        mEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        mList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private class NoticeAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mNotices.size();
        }

        @Override
        public Object getItem(int position) {
            return mNotices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(AppNoticesActivity.this)
                        .inflate(R.layout.app_notice_row, parent, false);
            }
            final StoredAppNotice notice = mNotices.get(position);
            TextView title = (TextView) row.findViewById(R.id.app_notice_title);
            TextView body = (TextView) row.findViewById(R.id.app_notice_body);
            TextView url = (TextView) row.findViewById(R.id.app_notice_url);
            Button delete = (Button) row.findViewById(R.id.app_notice_delete);
            title.setText(notice.getTitle());
            body.setText(notice.getBody());
            if (notice.getUrl() == null) {
                url.setVisibility(View.GONE);
            } else {
                url.setVisibility(View.VISIBLE);
                url.setText(notice.getUrl());
            }
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppNoticeService.getInstance().deleteSaved(notice);
                    reload();
                }
            });
            return row;
        }
    }
}

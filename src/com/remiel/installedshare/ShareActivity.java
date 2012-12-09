/*
 *   Copyright 2012 Remiel.C.Lee
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.remiel.installedshare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ShareActivity extends Activity {
	private static final String TAG = "ShareActivity";
	private static final boolean DEBUG = true;
	
	private Context mContext;
	
	private ListView mListView;
	private Button mShare;
	
	private ApkItemAdapter mPkgItemAdapter;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.main);
		mContext = this;
		
		mShare = (Button) findViewById(R.id.clean);
		mShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPkgItemAdapter.cleanAllSelection();
			}
		});
		
		mListView = (ListView) findViewById(R.id.pkgs);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mPkgItemAdapter.toggleCheckBox(position);
			}
		});
		
		mShare = (Button) findViewById(R.id.share);
		mShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = generateIntent();
				if (intent != null) {
					startActivity(intent);
				} else {
					Toast.makeText(ShareActivity.this, "There is no file to send!", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		mPkgItemAdapter = new ApkItemAdapter(getResolveInfos());
		mListView.setAdapter(mPkgItemAdapter);
	}
	
	private List<ResolveInfo> getResolveInfos() {
		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final PackageManager packageManager = mContext.getPackageManager();
        return packageManager.queryIntentActivities(intent, 0);
	}
	
	private final class ApkItemAdapter extends BaseAdapter {
		private final ArrayList<ApkItem> mApkList = new ArrayList<ApkItem>();
		
		public ApkItemAdapter(List<ResolveInfo> infos) {
			final PackageManager packageManager = mContext.getPackageManager();
			for (ResolveInfo info : infos) {
				if (info.system)
					continue;
				CharSequence label = info.loadLabel(packageManager);
				String dirPath = info.activityInfo.applicationInfo.publicSourceDir;
				if (DEBUG) Log.v(TAG, "app label = " + label + ";dirPath " + dirPath);
				mApkList.add(new ApkItem(label, dirPath));
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ApkItem item = mApkList.get(position);
			if (convertView == null)
				convertView = LayoutInflater.from(ShareActivity.this).inflate(R.layout.apk_item, null);
			TextView label = (TextView) convertView.findViewById(R.id.label_text);
			CheckBox cb = (CheckBox) convertView.findViewById(R.id.checker);
			label.setText(item.mLabelName);
			cb.setChecked(item.mChecked);
			
			return convertView;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public Object getItem(int position) {
			return mApkList.get(position);
		}
		
		@Override
		public int getCount() {
			return mApkList.size();
		}
		
		public void toggleCheckBox(int index) {
			ApkItem item = mApkList.get(index);
			item.mChecked = !item.mChecked;
			notifyDataSetChanged();
		}
		
		public ArrayList<String> getSelections() {
			ArrayList<String> array = new ArrayList<String>();
			for (ApkItem item : mApkList) {
				if (item.mChecked) {
					if (item.mPath == null || item.mPath.isEmpty()) {
						Toast.makeText(mContext, "App(" + item.mLabelName + ") has no public source path, skip!", Toast.LENGTH_SHORT).show();
					} else {
						File f = new File(item.mPath);
						if (f.canRead())
							array.add(item.mPath);
						else
							Toast.makeText(mContext, "The public source path of app(" + item.mLabelName + ") can't be read, skip!", Toast.LENGTH_SHORT).show();
					}
				}
			}
			if (array.isEmpty())
				return null;
			else
				return array;
		}
		
		public void cleanAllSelection() {
			for (ApkItem item : mApkList) {
				item.mChecked = false;
			}
			notifyDataSetChanged();
		}
	};
	
	private Intent generateIntent() {
		ArrayList<String> files = mPkgItemAdapter.getSelections();
		if (files == null)
			return null;
		ArrayList<Uri> uris = new ArrayList<Uri>();
		for (String f : files) {
			Uri uri = Uri.fromFile(new File(f));
			if (DEBUG) Log.v(TAG, "URI = " + uri);
			uris.add(uri);
		}
		Intent intent;
		int size = uris.size();
		if (size > 1) {
			intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
	        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		} else if (size == 1) {
			intent = new Intent(Intent.ACTION_SEND);
	        intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
		} else {
			return null;
		}
		
		intent.setType("*/*");
        return intent;
	}
	
	private class ApkItem {
		private final CharSequence mLabelName;
		private final String mPath;
		public boolean mChecked;
		public ApkItem(CharSequence pkg, String path) {
			mLabelName = pkg;
			mPath = path;
		}
	}
}

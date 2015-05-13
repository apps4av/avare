package com.ds.avare.cap;

import java.util.LinkedList;

import android.os.AsyncTask;

public class CapChartFetcher {
	
	private LinkedList<Chart> mCharts;
	private CapChartTask mTask;
	
	public CapChartFetcher() {
		mCharts = null;
	}
	
	public LinkedList<Chart> getCharts() {
		return mCharts;
	}

	public void fetch() {
		if (mTask != null) {
			if (mTask.getStatus() == AsyncTask.Status.RUNNING) {
				mTask.cancel(true);
			}
		}
		
		mTask = new CapChartTask();
		mTask.execute();
	}
	
	private class CapChartTask extends AsyncTask<Object, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Object... params) {
			Thread.currentThread().setName("CAP");
			
			mCharts = new LinkedList<Chart>();
			mCharts.add(new Chart(ChartIdentifier.SEA, new LatLng(49, -125), new LatLng(44.5, -117)));
			
			return true;
		}
	}
}

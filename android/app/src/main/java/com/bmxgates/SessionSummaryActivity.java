package com.bmxgates;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bmxgates.database.GateSession;
import com.bmxgates.database.GateSession.GateTime;

public class SessionSummaryActivity extends Activity {

	private static final String BMX_SESSION_SUMMARY_ACTIVITY = "BMXSessionSummaryActivity";

	BMXGateApplication application;
	
	GateSession gateSession; 
	
	ArrayAdapter<GateTime> arrayAdapter;
	
	long sessionId;
	
	private BroadcastReceiver databaseOpenReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			initGateSession();
		}
	};



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session_summary);
	
		application = (BMXGateApplication) getApplication();
		
		sessionId = getIntent().getExtras().getLong("sessionId");

		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(DateFormat.getDateInstance().format(new Date(sessionId)));

		ListView listView = (ListView) findViewById(R.id.summaryDetailHistoryListView);
		arrayAdapter = new ArrayAdapter<GateTime>(this, android.R.layout.simple_list_item_1){
			public View getView(int position, View convertView, ViewGroup parent) {
				View view;

		        if (convertView == null) {
		        	LayoutInflater mInflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		            view = mInflater.inflate(R.layout.gate_times_list_view, parent, false);
		        } else {
		            view = convertView;
		        }
		        
		        GateTime gateTime = getItem(position);
		        
		        int count = arrayAdapter.getCount();
		        //gate number
		        TextView textView = (TextView) view.findViewById(R.id.gateNumber);
		        textView.setTextColor(Color.WHITE);
		        textView.setText("#" + (count - position));
		        
		        //gate time
		        textView = (TextView) view.findViewById(R.id.gateTime);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(Stopwatch.formatTime(gateTime.getTime()));

		        //gate best diff
		        textView = (TextView) view.findViewById(R.id.bestDiff);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(Stopwatch.formatTime(gateTime.bestDiff()));

		        //gate avg diff
		        textView = (TextView) view.findViewById(R.id.avgDiff);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(Stopwatch.formatTime(gateTime.avgDiff()));

		        return view;
			}
		};
		listView.setAdapter(arrayAdapter);
		

		initGateSession();
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.session_summary, menu);
//		return true;
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
	        return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(databaseOpenReceiver);

		super.onPause();
	}

	@Override
	protected void onResume() {
		LocalBroadcastManager.getInstance(this).registerReceiver(databaseOpenReceiver, new IntentFilter("database-open"));

		super.onResume();
	}
	
	private void initGateSession() {
		Log.i(BMX_SESSION_SUMMARY_ACTIVITY, "Loading history");
		SQLiteDatabase database = application.getDatabase();
		if (database != null){
			if (gateSession == null)
				gateSession = new GateSession(database, sessionId);
			
			TextView textView = (TextView) findViewById(R.id.summaryRange);
			long[] range = gateSession.range();
			textView.setText("Range: " + 
					Stopwatch.formatTime(range[0]) + " - " + Stopwatch.formatTime(range[1])
			);

			textView = (TextView) findViewById(R.id.summaryDetailGate);
			textView.setText( "Gates: " + gateSession.gates() ); 

			textView = (TextView) findViewById(R.id.summaryBest);
			textView.setText( "Best: " + 
					Stopwatch.formatTime(gateSession.best())
					);

			textView = (TextView) findViewById(R.id.summaryAvg);
			textView.setText("Avg: " + 
					Stopwatch.formatTime(gateSession.avg())
					);

			textView = (TextView) findViewById(R.id.summaryWorst);
			textView.setText("Worst: " +
					Stopwatch.formatTime(gateSession.worst())
					);
			
			arrayAdapter.addAll(gateSession.getHistory());
			
//			View graphicalView = SessionGraphFragment.initChart(this, gateSession);
//			LinearLayout layout = (LinearLayout) findViewById(R.id.summaryGraph);
//		    layout.addView(graphicalView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//		    		LinearLayout.LayoutParams.WRAP_CONTENT));
		}


	}


}

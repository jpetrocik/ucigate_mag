package com.bmxgates;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bmxgates.database.GateSessionHistory;
import com.bmxgates.database.GateSessionHistory.SessionSummary;

public class SessionHistoryActivity extends Activity {

	private static final String BMX_SESSION_HISTORY_ACTIVITY = "BMXSessionHistoryActivity";

	private BMXGateApplication application;

	private GateSessionHistory gateSessionHistory;
	
	private ListView sessionListView;
	
	private ArrayAdapter<SessionSummary> arrayAdapter;
	
	private BroadcastReceiver databaseOpenReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			loadGateSession();
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session_history);
		
		application = (BMXGateApplication) getApplication();

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Gate History");
		
		sessionListView = (ListView) findViewById(R.id.summaryDetailHistoryListView);
		sessionListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent newIntent = new Intent(SessionHistoryActivity.this, SessionSummaryActivity.class);
				SessionSummary session = (SessionSummary) sessionListView.getAdapter().getItem(position);
				newIntent.putExtra("sessionId", session.getSessionId());
				startActivity(newIntent);
            }
        });
		
		arrayAdapter = new ArrayAdapter<SessionSummary>(this, android.R.layout.simple_list_item_1){
			DateFormat dateFormat = DateFormat.getDateInstance();

			public View getView(int position, View convertView, ViewGroup parent) {
				View view;

		        if (convertView == null) {
		        	LayoutInflater mInflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		            view = mInflater.inflate(R.layout.session_history_list_view, parent, false);
		        } else {
		            view = convertView;
		        }
		        
		        SessionSummary sessionSummary = getItem(position);
		        
		        //sessionId
		        TextView textView = (TextView) view.findViewById(R.id.sessionDate);
		        textView.setTextColor(Color.WHITE);
				Date sessionDate = new Date(sessionSummary.getSessionId());
		        textView.setText(dateFormat.format(sessionDate));
		        
		        //session gate
		        textView = (TextView) view.findViewById(R.id.sessionGates);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(String.valueOf(sessionSummary.getGates()));

		        //session best
		        textView = (TextView) view.findViewById(R.id.sessionDst);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(String.valueOf(sessionSummary.getDst()));

		        //session best
		        textView = (TextView) view.findViewById(R.id.sessionBest);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(Stopwatch.formatTime(sessionSummary.getBest()));

		        //gate avg diff
		        textView = (TextView) view.findViewById(R.id.sessionAvg);
		        textView.setTextColor(Color.WHITE);
		        textView.setText(Stopwatch.formatTime(sessionSummary.getAvg()));

		        return view;
			}
		};
		sessionListView.setAdapter(arrayAdapter);
		
		loadGateSession();

	}

	private void loadGateSession() {
		Log.i(BMX_SESSION_HISTORY_ACTIVITY, "Loading gate session");
		SQLiteDatabase database = application.getDatabase();
		if (database != null){
			if (gateSessionHistory == null)
				gateSessionHistory = new GateSessionHistory(database);
			
			arrayAdapter.clear();

			List<SessionSummary> results = gateSessionHistory.loadHistory();
			arrayAdapter.addAll(results);
		}
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.history, menu);
//		return true;
//	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
	        return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(this).registerReceiver(databaseOpenReceiver, new IntentFilter("database-open"));

		Log.i(BMX_SESSION_HISTORY_ACTIVITY, "resumed");
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(databaseOpenReceiver);

		super.onPause();

		Log.i(BMX_SESSION_HISTORY_ACTIVITY, "paused");
	}

}

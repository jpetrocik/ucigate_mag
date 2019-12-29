package com.bmxgates;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	BMXGateApplication bmxGateApplication;
	
	TextView messageView;
	
	private BroadcastReceiver applicationReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(BMXGateApplication.MESSAGE_RECEIVED)){
				String msg = intent.getExtras().getString(BMXGateApplication.MESSAGE_EXTRA_NAME);
				if (msg != null){
					Spanned formattedMsg = Html.fromHtml(msg);
					messageView.setText(formattedMsg);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getActionBar().setTitle("BMXGates.com");

		bmxGateApplication = (BMXGateApplication)getApplication();
		
		//hook up pratice button
		Button button = (Button) findViewById(R.id.gatePraticeButton);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(MainActivity.this, GateActivity.class);
				startActivity(myIntent);
			}
		});

		//hook up history button
		button = (Button) findViewById(R.id.gateHistoryButton);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(MainActivity.this, SessionHistoryActivity.class);
				startActivity(myIntent);
				
			}
		});
		
		messageView = (TextView) findViewById(R.id.textView1);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		LocalBroadcastManager broadcastReceiver = LocalBroadcastManager.getInstance(this);
		broadcastReceiver.registerReceiver(applicationReceiver, new IntentFilter(BMXGateApplication.MESSAGE_RECEIVED));

		bmxGateApplication.obtainMarketingMessage();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settingsMenuItem:
			Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(myIntent);

			return true;
		case R.id.btReconnectMenuItem:
			bmxGateApplication.reconnect();
		default:
			return super.onOptionsItemSelected(item);
		}
	}	

}

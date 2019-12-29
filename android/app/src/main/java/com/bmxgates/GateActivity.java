package com.bmxgates;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bmxgates.database.GateSession.GateTime;

public class GateActivity extends Activity {

	private static final String BMX_GATE_ACTIVITY = "BMXGateActivity";

	public enum STATUS { ON, OFF };

	private BMXGateApplication application;

	private ImageView redLed, greenLed, yellow1Led, yellow2Led;

	private TextView timerTextView;

	private TextView bestTextView;

	private TextView avgTextView;

	private ListView historyLogView;

	private ArrayAdapter<GateTime> arrayAdapter;

	private Button startCadenceButton;

	private Typeface digitalFont;

	private Dialog dialog;
	
	/**
	 * Called when the bluetooth connects.  Might happen when the connection is lost then
	 * restored while on this screen
	 */
	private BroadcastReceiver bluetoothConnectReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//show connection dialog
			connectionRestored();
		}
	};

	/**
	 * Called when connections fails, closing this activity.
	 */
	private BroadcastReceiver bluetoothConnectFailedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};
	
	/**
	 * Called when the bluetooth connection is lost. 
	 */
	private BroadcastReceiver bluetoothDisconnectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			connectionLost();
		}
	};

	/**
	 * Called when the database is opened
	 */
	private BroadcastReceiver databaseOpenReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshGateSessionDetails();
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gate);

		application = (BMXGateApplication) getApplication();

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Gate Pratice");
		
		//sets the font for the stop watch
		digitalFont = Typeface.createFromAsset(getAssets(), "digital-7.ttf");
		timerTextView = setupDigitalTextView((TextView) findViewById(R.id.timer));
		bestTextView = setupDigitalTextView((TextView) findViewById(R.id.bestTextView));
		avgTextView = setupDigitalTextView((TextView) findViewById(R.id.avgTextView));

		greenLed = (ImageView) findViewById(R.id.greenLed);
		yellow1Led = (ImageView) findViewById(R.id.yellow1Led);
		yellow2Led = (ImageView) findViewById(R.id.yellow2Led);
		redLed = (ImageView) findViewById(R.id.redLed);

		//setup start button and disable until connected
		startCadenceButton = (Button) findViewById(R.id.start_button);
		startCadenceButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (application.getStopwatch() == null || !application.getStopwatch().isRunning()){
					sendCommand(Commands.START_CADENCE, "");
				} else {
					//in reset mode
					toggleGreenLight(STATUS.OFF);
					toggleYellow1Light(STATUS.OFF);
					toggleYellow2Light(STATUS.OFF);
					toggleRedLight(STATUS.OFF);
					resetTimer();
				}
			}
		});
		startCadenceButton.setEnabled(false);

		//if in portrait update history log
		historyLogView = (ListView) findViewById(R.id.historyView);
		if (historyLogView != null){
			//setup history log		
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
			        textView.setTypeface(digitalFont);
			        textView.setText("#" + (count - position));
			        
			        //gate time
			        textView = (TextView) view.findViewById(R.id.gateTime);
			        textView.setTextColor(Color.WHITE);
			        textView.setTypeface(digitalFont);
			        textView.setText(Stopwatch.formatTime(gateTime.getTime()));

			        //gate best diff
			        textView = (TextView) view.findViewById(R.id.bestDiff);
			        textView.setTextColor(Color.WHITE);
			        textView.setTypeface(digitalFont);
			        textView.setText(Stopwatch.formatTime(gateTime.bestDiff()));

			        //gate avg diff
			        textView = (TextView) view.findViewById(R.id.avgDiff);
			        textView.setTextColor(Color.WHITE);
			        textView.setTypeface(digitalFont);
			        textView.setText(Stopwatch.formatTime(gateTime.avgDiff()));

			        return view;
				}
			};
			historyLogView.setAdapter(arrayAdapter);
			
		}

		//reconnect stopwatch
		if (application.getStopwatch() != null)
			application.getStopwatch().setTextView(timerTextView);
		
		Log.i(BMX_GATE_ACTIVITY, "created");
	}

	private TextView setupDigitalTextView(TextView textView){
		if (textView != null){
			textView.setTypeface(digitalFont);
		}
		return textView;
	}
	
	@Override
	public void onResume() {
		super.onResume();

		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectReceiver, new IntentFilter(BluetoothSerial.BLUETOOTH_CONNECTED));
		LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectFailedReceiver, new IntentFilter(BluetoothSerial.BLUETOOTH_FAILED));
		LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDisconnectReceiver, new IntentFilter(BluetoothSerial.BLUETOOTH_DISCONNECTED));
		LocalBroadcastManager.getInstance(this).registerReceiver(databaseOpenReceiver, new IntentFilter("database-open"));

		if (!application.isConnected()){
			connectionLost();
		}  else {
			startCadenceButton.setEnabled(true);
		}
		
		refreshGateSessionDetails();

		application.setSerialHandler(new GateActivitySerialHandler(this));
		
		Log.i(BMX_GATE_ACTIVITY, "resumed");
	}

	private void refreshGateSessionDetails(){
		if (application.currentGateSession() != null) {
			updateHistoryView();
			updateAvgTime();
			updateBestTIme();
		}
	}
	
	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectFailedReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDisconnectReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(databaseOpenReceiver);

		application.removeSerialHandler();

		super.onPause();

		Log.i(BMX_GATE_ACTIVITY, "paused");
	} 

	@Override
	protected void onStop() {
		Log.i(BMX_GATE_ACTIVITY, "stopping");
		super.onStop();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.practice, menu);
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
	        return true;
		case R.id.newSession:
			final String[] defaultDistances = new String[] {"0", "1", "2", "3", "4", "5", "10", "15", "30", "35", "40"};
			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("New Session Distaince")
					.setItems(defaultDistances, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					application.currentGateSession().startNewSession(Integer.parseInt(defaultDistances[which]));
					refreshGateSessionDetails();
				}
				
			});
			builder.create().show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Sends command back to control box
	 * 
	 * @param cmd
	 * @param args
	 */
	protected void sendCommand(Commands cmdEnum, String args){
		try {
			application.sendCommand(cmdEnum, args);
		} catch (IOException e) {
			connectionLost();
			application.reconnect();
		}
	}

	protected void showErrorDialog(String msg) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set title
		alertDialogBuilder.setTitle("Error");

		// set dialog message
		alertDialogBuilder.setMessage(msg).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	public void toggleGreenLight(STATUS status) {
		if (status == STATUS.ON)
			greenLed.setImageDrawable(getResources().getDrawable(R.drawable.green_led_on));
		else
			greenLed.setImageDrawable(getResources().getDrawable(R.drawable.green_led));
	}

	public void toggleYellow1Light(STATUS status) {
		if (status == STATUS.ON)
			yellow1Led.setImageDrawable(getResources().getDrawable(R.drawable.yellow_led_on));
		else
			yellow1Led.setImageDrawable(getResources().getDrawable(R.drawable.yellow_led));
	}

	public void toggleYellow2Light(STATUS status) {
		if (status == STATUS.ON)
			yellow2Led.setImageDrawable(getResources().getDrawable(R.drawable.yellow_led_on));
		else
			yellow2Led.setImageDrawable(getResources().getDrawable(R.drawable.yellow_led));
	}

	public void toggleRedLight(STATUS status) {
		if (status == STATUS.ON)
			redLed.setImageDrawable(getResources().getDrawable(R.drawable.red_led_on));
		else 
			redLed.setImageDrawable(getResources().getDrawable(R.drawable.red_led));
	}

	protected void startTimer(){
		startCadenceButton.setText("Reset");

		if (application.getStopwatch() != null)
			application.getStopwatch().cancel(true);
		
		application.setStopwatch(new Stopwatch(timerTextView));
		application.getStopwatch().execute();
	}

	protected void stopTimer(long elapsedTime){
		startCadenceButton.setText("Start");
		
		if (application.getStopwatch() != null){
			application.getStopwatch().stop(Math.abs(elapsedTime));
		}
		
		//elapsedTime is zero'd out if beyond threshold, see GateSession
		if (elapsedTime > 0){
			if (elapsedTime == application.currentGateSession().best()){
				timerTextView.setTextColor(getResources().getColor(R.color.bestTime));
			}
			
			//not present in portrait 
			if (arrayAdapter != null){
				List<GateTime> history = application.currentGateSession().getHistory();
				arrayAdapter.clear();
				arrayAdapter.addAll(history);
			}
	
			updateAvgTime();
	
			updateBestTIme();
		} else {
			timerTextView.setTextColor(getResources().getColor(R.color.badTime));
		}
		
	}

	public void resetTimer() {
		startCadenceButton.setText("Start");

		//reset best time color back to standard
		timerTextView.setTextColor(getResources().getColor(R.color.white));

		if (application.getStopwatch() != null)
			application.getStopwatch().stop(0);
	}

	private void updateBestTIme(){
		//not present in portrait 
		if (bestTextView != null){
			long best = application.currentGateSession().best();
			if (best > 0){
				bestTextView.setText("BEST: " + 
						Stopwatch.formatTime(best)
						);
			} else {
				bestTextView.setText("BEST: --.----");
			}
		}
	}
	
	private void updateAvgTime(){
		//not present in portrait 
		if (avgTextView != null){
			long avg = application.currentGateSession().avg();
			if (avg > 0){
				avgTextView.setText("AVG: " + 
						Stopwatch.formatTime(avg)
						);
			} else {
				avgTextView.setText("AVG: --.---");
			}			
		}
	}
	
	private void updateHistoryView(){
		if (arrayAdapter != null){
			arrayAdapter.clear();
			for( GateTime gateTime : application.currentGateSession().getHistory()){
				arrayAdapter.add(gateTime);
			}
		}
	}
	
	protected void connectionRestored(){
		Log.i(BMX_GATE_ACTIVITY, "Connection restored");
		
		dialog.dismiss();
		startCadenceButton.setEnabled(true);
	}
	
	protected void connectionLost(){
		startCadenceButton.setEnabled(false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.bluetooth_logo)
		.setTitle("Connecting...");

		dialog = builder.create();
		dialog.show();

	}
}

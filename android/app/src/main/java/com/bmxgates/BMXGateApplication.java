package com.bmxgates;

import android.app.Application;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bmxgates.BluetoothSerial.MessageHandler;
import com.bmxgates.database.GateSession;
import com.bmxgates.database.GateSession.GateTime;
import com.bmxgates.database.SqlLiteHelper;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class BMXGateApplication extends Application implements MessageHandler{

	private static final String BMX_GATE_APPLICATION = "BMXGateApplication";

	protected static final String MESSAGE_RECEIVED = "message-recieved";

	protected static final String MESSAGE_EXTRA_NAME = BMXGateApplication.class.getPackage().toString() + ".Message";

	private SQLiteOpenHelper sqlLiteOpenHelper;

	private SQLiteDatabase database;

	private BluetoothSerial bluetoothSerial; 
	
	private Stopwatch currentStopwatch;

	private SerialHandler serialHandler;
	
	private GateSession gateSession;
	
	private String marketingMessage;

	private AsyncTask<Void, Void, String> messageDownloader;
	
	@Override
	public void onCreate() {
		super.onCreate();

		obtainMarketingMessage();

		sqlLiteOpenHelper = new SqlLiteHelper(this);
		openDatabase();

		//do last because bluetooth takes time
		bluetoothSerial = new BluetoothSerial(this, this, "HC-0");
		bluetoothSerial.onResume();
		bluetoothSerial.connect();
		
	}

	private class  DownloadMessage extends AsyncTask<Void, Void, String> {

			@Override
			protected String doInBackground(Void... params) {
				try {
					URL url = new URL("http://www.bmxgates.com/m.html");
					
					InputStream inStream = url.openStream();
					byte[] data = ByteStreams.toByteArray(inStream);
					
					return new String(data);
					
				} catch (Exception e) {
					Log.e(BMX_GATE_APPLICATION, "Unable to download message");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(String result) {
				if (result != null){
					
					marketingMessage = result;
					
					Intent intent = new Intent(BMXGateApplication.MESSAGE_RECEIVED);
					intent.putExtra(BMXGateApplication.MESSAGE_EXTRA_NAME, result);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
			}
	}

	public boolean isConnected() {
		return bluetoothSerial.connected;
	}

	public AsyncTask<Void, Void, SQLiteDatabase> openDatabase(){
		return new AsyncTask<Void, Void, SQLiteDatabase>() {
			@Override
			protected SQLiteDatabase doInBackground(Void... params) {
				database = sqlLiteOpenHelper.getWritableDatabase();

				Intent intent = new Intent("database-open");
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

				return database;
			}
		}.execute();
	}

	@Override
	public void onTerminate() {
		Log.i(BMX_GATE_APPLICATION, "BMXGateApplication terminating");

		if (currentStopwatch != null)
			currentStopwatch.cancel(true);

		bluetoothSerial.close();

		super.onTerminate();
	}

	public SQLiteDatabase getDatabase() {
		return database;
	}

	public Stopwatch getStopwatch() {
		return currentStopwatch;
	}

	public void setStopwatch(Stopwatch currentStopwatch) {
		this.currentStopwatch = currentStopwatch;
	}

	public GateSession currentGateSession(){
		if (database != null && gateSession == null) {
				gateSession = new GateSession(database);
		}
		
		return gateSession;
	}

	@Override
	public int read(int size, byte[] buffer) {
		int i = 0;

		// not a valid cmd
		if (size < 5)
			return 0;

		// look for cmd seq
		for (; i < size; i++) {

			// found cmd seq
			if (buffer[i] == 'B' && buffer[i += 1] == 'M' && buffer[i += 1] == 'X') { //i=2
				try {
					byte[] data = Arrays.copyOf(buffer, size);
					int cmdSize = 0 | data[i += 1]; //i=3					
					String cmd = new String(data, i += 1, cmdSize);
					int argSize = 0 | data[i += cmdSize];
					String args = new String(data, i += 1, argSize);
					i += argSize;

					handleMessage(cmd, args);

					return i+1;
				} catch (IndexOutOfBoundsException e) {
					Log.d(BMX_GATE_APPLICATION, "Incomplete message");
					return 0;
				} catch (Throwable t) {
					Log.i(BMX_GATE_APPLICATION, "Failed processing message: " + t.getMessage());
				}
			}
		}
		return i;
	}

	public void handleMessage(String cmd, String args) {

		Log.i(BMX_GATE_APPLICATION, "Recieved " + cmd + ":" + args);
		try {
			Commands event = Commands.valueOf(cmd);
			switch (event) {
			case EVNT_TIMER_1:
				
				long elapsedTime = Long.parseLong(args);
				if (elapsedTime>0){
					GateTime gateTime = gateSession.addTime(elapsedTime);
					if (gateTime == null){
						//convert to negative to indicate bad time and have UI handle properly
						args = "-" + args;
					}
				}
				
				break;
			default:
				break;
			}
		} catch (Throwable t) {
			Log.e(BMX_GATE_APPLICATION,"Invalid command " + cmd + ":" + args);
		}

		if (serialHandler != null){
			Message message = serialHandler.obtainMessage();

			// convert cmd/arg to bundle
			Bundle cmdMsg = new Bundle();
			cmdMsg.putString(SerialHandler.CMD, cmd);
			cmdMsg.putString(SerialHandler.ARGS, args);
			message.setData(cmdMsg);

			serialHandler.sendMessage(message);
		}
	}

	public void setSerialHandler(SerialHandler serialHandler) {
		this.serialHandler=serialHandler;
	}

	public void removeSerialHandler() {
		this.serialHandler=null;
	}

	public void sendCommand(Commands cmdEnum, String args) throws IOException {
		if (isConnected()){
			String cmd = cmdEnum.toString();

			int cmdSize = cmd.length();
			int argSize = args.length();
			bluetoothSerial.write("BMX".getBytes());
			bluetoothSerial.write((char)cmdSize);
			bluetoothSerial.write(cmd.getBytes());
			bluetoothSerial.write((char)argSize);
			bluetoothSerial.write(args.getBytes());
		}
	}

	public void obtainMarketingMessage(){
		if (marketingMessage==null && messageDownloader==null) {
			messageDownloader = new DownloadMessage().execute();
		} else if (marketingMessage != null ){
			Intent intent = new Intent(BMXGateApplication.MESSAGE_RECEIVED);
			intent.putExtra(BMXGateApplication.MESSAGE_EXTRA_NAME, marketingMessage);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
		}
	}

	public void reconnect() {
		bluetoothSerial.close();
		bluetoothSerial.connect();
	}

}

package com.bmxgates.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.psoft.math.MathUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


public class GateSession {

	private static final String BMX_GATE_SESSION = "BMXGateSession";

	SQLiteDatabase database;
	
	final List<GateTime> times = new ArrayList<GateTime>();

	final List<GateTime> rawTimes = new ArrayList<GateTime>();

	int distance;
	
	long currentSession = 0;
	
	public GateSession(SQLiteDatabase database) {
		this.database = database;

		findLastestSession();
		
		loadDatabase();
	}

	public GateSession(SQLiteDatabase database, long sessionId) {
		this.database = database;

		currentSession = sessionId;
		
		loadDatabase();
	}

	public GateSession(SQLiteDatabase database, long sessionId, int distance, List<Long> allTimes) {
		this.database = database;
		this.currentSession = sessionId;
		this.distance = distance;
		
		//populate times
		for (long time : allTimes){
			internalAdd(time);
		}

	}

	protected void findLastestSession(){
		Cursor results = database.rawQuery("SELECT MAX(" + SqlLiteHelper.COLUMN_SESSION + ") FROM " + SqlLiteHelper.TABLE_GATE_TIMES, null);
		if (results.moveToNext())
			currentSession = results.getLong(0);
		
		if (currentSession == 0) {
			currentSession = System.currentTimeMillis();
			distance = 30;
		}
		
		Log.i(BMX_GATE_SESSION, "Loading current session " + currentSession);
	}
	
	public void startNewSession(int distance){
		currentSession = System.currentTimeMillis();
		this.distance = distance;
		
		times.clear();
		rawTimes.clear();
	}
	
	/**
	 * Load time history from current session
	 */
	protected void loadDatabase(){
		if (database != null){
			Cursor results = database.query(SqlLiteHelper.TABLE_GATE_TIMES, 
					SqlLiteHelper.GATE_TIMES_COLUMNS, 
					SqlLiteHelper.COLUMN_SESSION + "=?", 
					new String[] {String.valueOf(currentSession)}, 
					null, null, SqlLiteHelper.COLUMN_TIMESTAMP + " asc", null);
			int columnIndex = results.getColumnIndex(SqlLiteHelper.COLUMN_TIME);
			while (results.moveToNext()){
				long elapsedTime = results.getLong(columnIndex);
				internalAdd(elapsedTime);
			}
		}
	}

	public GateTime addTime(long time){
		
		GateTime gateTime = internalAdd(time);
		
		ContentValues values = new ContentValues();
		values.put(SqlLiteHelper.COLUMN_TIME, time);
		values.put(SqlLiteHelper.COLUMN_DISTANCE, distance);
		values.put(SqlLiteHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());
		values.put(SqlLiteHelper.COLUMN_SESSION, currentSession);
		database.insert(SqlLiteHelper.TABLE_GATE_TIMES, null, values);


		if (!times.contains(gateTime))
			return null;
		
		return gateTime;
	}
	
	/**
	 * Adds the time to the history, then checks for outliers and removes them.
	 */
	protected GateTime internalAdd(long time){
		
		//add to internal time history list
		GateTime gateTime = new GateTime(time);
		rawTimes.add(0,gateTime);
		
		
		//calculate boundary threshold on all times and filter
		long maxTime = MathUtils.boundryThreshold(splitTimes(rawTimes));
		times.clear();
		for (GateTime rawGateTime : rawTimes){
			if (rawGateTime.getTime()<= maxTime)
				times.add(rawGateTime);
		}
		
		return gateTime;
	}
	
	public long[] range(){
		int size = times.size();
		if (size==0)
			return new long[] {0,0};

		long[] rawTimes = splitTimes(times);
		if (size>2)
			return MathUtils.trimmedRange(rawTimes, 0.05);
		else
			return MathUtils.range(rawTimes);
}
	
	
	public long avg(){
		int size = times.size();
		if (size==0)
			return 0;

		long[] rawTimes = splitTimes(times);
		if (size>2)
			return MathUtils.trimmedMean(rawTimes, 0.05);
		else
			return MathUtils.mean(rawTimes);
	}
	
	public long best(){
		if (times.size()==0)
			return 0;
		
		long best = Long.MAX_VALUE;
		for (GateTime t : times){
			best = Math.min(best,  t.getTime());
		}

		return best;
	}

	public long worst() {
		if (times.size()==0)
			return 0;
		
		long worst = Long.MIN_VALUE;
		for (GateTime t : times){
			worst = Math.max(worst,  t.getTime());
		}

		return worst;
	}


	public List<GateTime> getHistory() {
		return Collections.unmodifiableList(times);
	}
	
	public int gates() {
		return times.size();
	}

	/**
	 * Returns just the time
	 */
	private static long[] splitTimes(List<GateTime> gateTimes) {
		int size = gateTimes.size();
		long[] times = new long[size];
		
		for(int i=0;i<size;i++){
			times[i]=gateTimes.get(i).time;
		}
		
		return times;
	}
	
	public class GateTime {
		long time;
		
		public GateTime(long time){
			this.time=time;
		}
		
		public long getTime(){
			return time;
		}
		
		public long avgDiff(){
			return time-avg();
		}
		
		public long bestDiff(){
			return time-best();
		}
		
	}
}

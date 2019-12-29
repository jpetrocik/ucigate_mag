package com.bmxgates.database;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class GateSessionHistory {

	SQLiteDatabase database;
	
	public GateSessionHistory(SQLiteDatabase database) {
		this.database = database;
	}

	public List<SessionSummary> loadHistory(){
		List<SessionSummary> summary = new ArrayList<SessionSummary>();
		
		//fetch are records and build gate sessions
		Cursor results = database.query(SqlLiteHelper.TABLE_GATE_TIMES, 
				SqlLiteHelper.GATE_TIMES_COLUMNS, 
				null,
				null,
				null, null, SqlLiteHelper.COLUMN_SESSION + " desc, " + SqlLiteHelper.COLUMN_TIMESTAMP + " asc", null);
		
		int sessionIndex = results.getColumnIndex(SqlLiteHelper.COLUMN_SESSION);
		int timeIndex = results.getColumnIndex(SqlLiteHelper.COLUMN_TIME);
		int distanceIndex = results.getColumnIndex(SqlLiteHelper.COLUMN_DISTANCE);
		
		long currentSessionId = -1;
		long currentTime = -1;
		int currentDistance = -1;
		List<Long> gateTimes = null;
		while(results.moveToNext()){
			long sessionId = results.getLong(sessionIndex);
			
			if (currentSessionId != sessionId)
				
				//create session and new time array for next session, all currentXX value are previous loop so safe to use here.
				if (gateTimes != null){
					GateSession gateSession = new GateSession(database, currentSessionId, currentDistance, gateTimes);
					summary.add(0, new SessionSummary(gateSession.currentSession, gateSession.best(), gateSession.avg(), gateTimes.size(), gateSession.distance));
					
					gateTimes = new ArrayList<Long>();
				} else {
					
					//first time through loop
					gateTimes = new ArrayList<Long>();
			} 

			currentTime = results.getLong(timeIndex);
			currentDistance = results.getInt(distanceIndex);
			currentSessionId = sessionId;

			gateTimes.add(0, currentTime);
		}

		//save last session
		if (gateTimes != null){
			GateSession gateSession = new GateSession(database, currentSessionId, currentDistance, gateTimes);
			summary.add(
				new SessionSummary(gateSession.currentSession, gateSession.best(), gateSession.avg(), gateTimes.size(), gateSession.distance)
			);
		}

		return summary;
	}
	
	public class SessionSummary {
		
		long sessionId;
		
		int dst;

		long best;
		
		long avg;
		
		int gates;

		public SessionSummary(long sessionId, long best, long avg, int gates, int dst){
			this.sessionId=sessionId;
			this.best=best;
			this.avg=avg;
			this.gates=gates;
			this.dst=dst;
		}

		public long getSessionId() {
			return sessionId;
		}

		public long getBest() {
			return best;
		}

		public long getAvg() {
			return avg;
		}

		public int getGates() {
			return gates;
		}

		public int getDst() {
			return dst;
		}

		public void setDst(int dst) {
			this.dst = dst;
		}
		
	}
}

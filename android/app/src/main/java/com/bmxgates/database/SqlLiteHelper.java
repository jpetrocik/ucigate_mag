package com.bmxgates.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SqlLiteHelper extends SQLiteOpenHelper {

  public static final String TABLE_GATE_TIMES = "gate_times";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_TIMESTAMP = "timestamp";
  public static final String COLUMN_TIME = "time";
  public static final String COLUMN_DISTANCE = "distance";
  public static final String COLUMN_SESSION = "session";
  public static final String[] GATE_TIMES_COLUMNS = new String[] {COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_TIME, COLUMN_DISTANCE, COLUMN_SESSION};
  
  private static final String DATABASE_NAME = "bmxgates.db";
  private static final int DATABASE_VERSION = 4;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table " + TABLE_GATE_TIMES + "(" + 
		  COLUMN_ID + " integer primary key autoincrement, " + 
		  COLUMN_TIMESTAMP + " INTEGER not null, " +
		  COLUMN_TIME + " integer not null, " +
		  COLUMN_DISTANCE + " integer not null, " + 
		  COLUMN_SESSION + " integer not null " + 
		  ")";

  public SqlLiteHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(SqlLiteHelper.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_GATE_TIMES);
    onCreate(db);
  }

} 
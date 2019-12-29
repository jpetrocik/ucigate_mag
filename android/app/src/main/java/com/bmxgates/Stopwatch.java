package com.bmxgates;

import android.os.AsyncTask;
import android.widget.TextView;

/**
 * StopWatch is an AsyncTask that updates a TextView with the elapsed time.  AnsycTask is used to ensure
 * UI remains responsive.
 * 
 * @author jpetrocik
 *
 */
public class Stopwatch extends AsyncTask<Void, Long, Long> {

	private final static long REFRESH_RATE = 25;

	private TextView textView;
	
	private long startTime = 0;
	
	private long reportedTime = 0;
	
	private boolean stopped = true;
	
	
	public Stopwatch(TextView textView) {
		this.textView=textView;
	}

	@Override
	protected void onPreExecute() {
		startTime = System.currentTimeMillis();
		stopped = false;
	}

	/**
	 * Loops every REFRESH_RATE posting updates to the UI.  Loop
	 * is throttles by the REFRESH_RATE.  This effects how
	 * often the UI is updated
	 */
	@Override
	protected Long doInBackground(Void... params) {
		while(!stopped){
			long elapsedTime = System.currentTimeMillis() - startTime;
			publishProgress(elapsedTime);
			
			//Throttles the loop and UI updates
			try {
				Thread.sleep(REFRESH_RATE);
			} catch (InterruptedException e) {
				stopped=true;
				break;
			}
		}
		
		return reportedTime;
	}

	/**
	 * Calle when the UI needs to be updated
	 */
	@Override
	protected void onProgressUpdate(Long... elapsedTime) {
			textView.setText(formatTime(elapsedTime[0]));
     }

	 /**
	  * Just update the UI again.
	  */
     protected void onPostExecute(Long elapsedTime) {
    	 onProgressUpdate(elapsedTime);
     }
     
     /**
      * Called to stop the timer.  The elaspedTime is 
      * passed from the control box.  This value is returned
      * from doInBackground
      * 
      * @param elaspedTime
      */
     public void stop(long elaspedTime){
    	 reportedTime = elaspedTime;
    	 
    	 //if not running but we have new time
    	 //manually call onProgressUpdate to 
    	 //update UI
    	 if (stopped){
    		 onProgressUpdate(elaspedTime);
    	 }
    	 
    	 stopped=true;
     }
     
     public boolean isRunning(){
    	 return !stopped;
     }
     
     public static String formatTime(long time){
			long sec = Math.abs(time) / 1000;
			long milis = Math.abs(time) % 1000;
			String sign = (time<0)?"-":"";
			
			return String.format(sign + "%02d", sec) + "." + String.format("%03d", milis);
     }

	public void setTextView(TextView textView) {
		this.textView=textView;
	}
}

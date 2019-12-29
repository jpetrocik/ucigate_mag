package com.bmxgates;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.bmxgates.GateActivity.STATUS;

/**
 * SerialHandler handles bridging the gap between the background serial reader thread and the UI thread.
 * This class currently requires reference to GateActivity, so it's not very reusable.  This should be changed
 * to listen for messages.  
 * 
 * @author jpetrocik
 *
 */
public class GateActivitySerialHandler extends SerialHandler {

	private GateActivity gateActivity;

	public GateActivitySerialHandler(GateActivity mainActivity) {
		this.gateActivity = mainActivity;
	}


	@Override
	public void handleMessage(Message msg) {

		try {
			Bundle data = msg.getData();
			Commands event = Commands.valueOf(data.getString(CMD));

			switch (event) {
			case EVNT_OK_RIDERS:
				break;
			case EVNT_RIDERS_READY:
				break;
			case EVNT_RED_LIGHT:
				gateActivity.toggleRedLight(STATUS.ON);
				break;
			case EVNT_YELLOW_1_LIGHT:
				gateActivity.toggleYellow1Light(STATUS.ON);
				break;
			case EVNT_YELLOW_2_LIGHT:
				gateActivity.toggleYellow2Light(STATUS.ON);
				break;
			case EVNT_GREEN_LIGHT:
				gateActivity.toggleGreenLight(STATUS.ON);
				gateActivity.startTimer();
				break;
			case EVNT_CADENCE_STARTED:
				gateActivity.toggleRedLight(STATUS.OFF);
				gateActivity.toggleYellow1Light(STATUS.OFF);
				gateActivity.toggleYellow2Light(STATUS.OFF);
				gateActivity.toggleGreenLight(STATUS.OFF);
				gateActivity.resetTimer();
				break;
			case EVNT_TIMER_1:
				gateActivity.toggleRedLight(STATUS.OFF);
				gateActivity.toggleYellow1Light(STATUS.OFF);
				gateActivity.toggleYellow2Light(STATUS.OFF);
				gateActivity.toggleGreenLight(STATUS.OFF);
				String elaspeTime = data.getString(ARGS);
				gateActivity.stopTimer(Long.parseLong(elaspeTime));
				break;
			default:
				break;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			gateActivity.showErrorDialog(t.getMessage());
		}
	}
}

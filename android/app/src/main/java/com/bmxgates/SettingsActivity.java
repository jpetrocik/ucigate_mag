package com.bmxgates;

import java.io.IOException;
import java.util.List;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference, String defaultValue) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), defaultValue));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("user_name"),"");
			bindPreferenceSummaryToValue(findPreference("password"),"");
		}
	}

	/**
	 * This fragment shows notification preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class CadenceBoxPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_cadence_box);

			bindPreferenceSummaryToValue(findPreference("sw_version"),"N/A");
			bindPreferenceSummaryToValue(findPreference("hw_version"),"N/A");
}
		
		@Override
		public void onResume(){
			super.onResume();
			
			BMXGateApplication bmxGateApplication = (BMXGateApplication) getActivity().getApplication();
			
			bmxGateApplication.setSerialHandler(new SerialHandler() {
			
				@Override
				public void handleMessage(Message msg) {

					try {
						Bundle data = msg.getData();
						Commands event = Commands.valueOf(data.getString(CMD));
						String version = data.getString(ARGS);

						switch (event) {
						case SW_VERSION:
							EditTextPreference swVersion = (EditTextPreference) findPreference("sw_version");
							swVersion.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
							sBindPreferenceSummaryToValueListener.onPreferenceChange(swVersion, version);
							
							break;
						case HW_VERSION:
							EditTextPreference hwVersion = (EditTextPreference) findPreference("hw_version");
							hwVersion.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
							sBindPreferenceSummaryToValueListener.onPreferenceChange(hwVersion, version);
							
							break;
						default:
							break;
						}
					} catch(Exception e){
						Log.e("BMXSettings", "Failed to process message");
					}
				}
			});
			
			/*
			 * Will not return value if still connecting
			 */
			try {
				bmxGateApplication.sendCommand(Commands.GET, "SW_VERSION");
				bmxGateApplication.sendCommand(Commands.GET, "HW_VERSION");
			} catch (IOException e) {
				Log.i("BMXSettings", "Unable to obtain sw version");
				e.printStackTrace();
			}
		}
	}

}

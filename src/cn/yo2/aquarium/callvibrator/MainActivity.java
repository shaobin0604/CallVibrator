package cn.yo2.aquarium.callvibrator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

import com.hlidskialf.android.preference.SeekBarPreference;

public class MainActivity extends PreferenceActivity implements OnPreferenceChangeListener, 
	OnSharedPreferenceChangeListener {
	
	
	private CheckBoxPreference mOutgoingCallPrefs;
	private CheckBoxPreference mIncomingCallPrefs;
	private CheckBoxPreference mEndCallPrefs;
	private CheckBoxPreference mReminderPrefs;
	
	private CheckBoxPreference mShowNotificationPres;
	
	private SeekBarPreference mVibrateTime;
	private ListPreference mReminderInterval;
	
	private Preference mAboutPrefs;
	
	private SharedPreferences mSharedPreferences;
	
	private String mVersionName;
	
	private String mAppVersionName = "";
	private String mAppVersionCode = "";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.prefs);
        
        PackageManager packageManager = getPackageManager();
        try {
			PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);
			
			mAppVersionName = info.versionName;
			mAppVersionCode = String.valueOf(info.versionCode);
		} catch (NameNotFoundException e) {
			// the current package should be exist all times
		}
        
        mSharedPreferences = getPreferenceManager().getSharedPreferences();

        mOutgoingCallPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_outgoing_call));
        mOutgoingCallPrefs.setOnPreferenceChangeListener(this);
        
        mIncomingCallPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_incoming_call));
        mIncomingCallPrefs.setOnPreferenceChangeListener(this);
        
        mEndCallPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_end_call));
        mEndCallPrefs.setOnPreferenceChangeListener(this);
        
        mReminderPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_reminder));
        mReminderPrefs.setOnPreferenceChangeListener(this);
        
        mVibrateTime = (SeekBarPreference) findPreference(getString(R.string.prefs_key_vibrate_time));
        mVibrateTime.setOnPreferenceChangeListener(this);
        
        mReminderInterval = (ListPreference) findPreference(getString(R.string.prefs_key_reminder_interval));
        mReminderInterval.setOnPreferenceChangeListener(this);
        
        mShowNotificationPres = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_show_notification));
        mShowNotificationPres.setOnPreferenceChangeListener(this);
        
        mAboutPrefs = findPreference(getString(R.string.prefs_key_about));
        mVersionName = getString(R.string.prefs_summary_about, mAppVersionName);
		mAboutPrefs.setSummary(mVersionName);
		
        Intent intent = new Intent(this, CallStateService.class);
        startService(intent);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();

		updateSeekBarPreferenceSummary(mVibrateTime, mSharedPreferences.getInt(getString(R.string.prefs_key_vibrate_time), 80));
		updatePreferenceSummary(mReminderInterval, mSharedPreferences.getString(getString(R.string.prefs_key_reminder_interval), "45"));
		
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
    

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (getString(R.string.prefs_key_vibrate_time).equals(key)) {
			updateSeekBarPreferenceSummary(mVibrateTime, mSharedPreferences.getInt(key, 80));
		} else if (getString(R.string.prefs_key_reminder_interval).equals(key)) {
			updatePreferenceSummary(mReminderInterval, mSharedPreferences.getString(key, "45"));
		}
	}
	
	private void updateSeekBarPreferenceSummary(SeekBarPreference preference, int value) {
		preference.setSummary(value + getString(R.string.text_ms));
	}

	private void updatePreferenceSummary(ListPreference preference, String value) {
		int index = preference.findIndexOfValue(value);
		CharSequence[] entries = preference.getEntries();
		
		if (index < entries.length) {
			preference.setSummary(entries[index]);
		} else {
			preference.setSummary(null);
		}
		
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mOutgoingCallPrefs ||
				preference == mIncomingCallPrefs ||
				preference == mEndCallPrefs ||
				preference == mVibrateTime ||
				preference == mShowNotificationPres ||
				preference == mReminderPrefs ||
				preference == mReminderInterval ) {
			
			Intent intent = new Intent(this, CallStateService.class);
	        startService(intent);
		}
		
		return true;
	}
	
}
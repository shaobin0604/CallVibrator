package cn.yo2.aquarium.callvibrator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlidskialf.android.preference.SeekBarPreference;

public class MainActivity extends PreferenceActivity implements OnPreferenceChangeListener, 
	OnSharedPreferenceChangeListener, OnPreferenceClickListener {
	
	private static final int DIALOG_ABOUT = 1000;
	
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
		mAboutPrefs.setOnPreferenceClickListener(this);
		
        Intent intent = new Intent(this, CallStateService.class);
        startService(intent);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
		case DIALOG_ABOUT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(R.drawable.ic_launcher);
			builder.setTitle(R.string.app_name);
			
			LayoutInflater inflater = LayoutInflater.from(this);
			
			LinearLayout root = (LinearLayout) inflater.inflate(R.layout.about_dialog, null);
			TextView versionName = (TextView) root.findViewById(R.id.versionName);
			versionName.setText(mVersionName);
			
			TextView versionCode = (TextView) root.findViewById(R.id.versionCode);
			versionCode.setText(getString(R.string.text_build, mAppVersionCode));
			
			builder.setView(root);
			
			builder.setPositiveButton(R.string.text_more, new OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(getString(R.string.url_more)));
					
					startActivity(intent);
				}
			});
			
			builder.setNegativeButton(android.R.string.cancel, null);
			
			return builder.create();

		default:
			break;
		}
    	return super.onCreateDialog(id);
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
    
    
    
	public boolean onPreferenceClick(Preference preference) {
		if (preference == mAboutPrefs) {
			showDialog(DIALOG_ABOUT);
			return true;
		}
		return false;
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
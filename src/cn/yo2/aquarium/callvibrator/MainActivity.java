package cn.yo2.aquarium.callvibrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import cn.yo2.aquarium.logutils.MyLog;

import com.hlidskialf.android.preference.SeekBarPreference;

public class MainActivity extends PreferenceActivity implements OnPreferenceChangeListener, 
	OnSharedPreferenceChangeListener, OnPreferenceClickListener {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String APP_LOG = "CallVibrator.log";
    
	private static final int DLG_COLLECT_LOG_OK = 1000;
	private static final int DLG_COLLECT_LOG_FAIL = 2000;
	
	private CheckBoxPreference mOutgoingCallPrefs;
	private CheckBoxPreference mIncomingCallPrefs;
	private CheckBoxPreference mEndCallPrefs;
	private CheckBoxPreference mReminderPrefs;
	
	private CheckBoxPreference mShowNotificationPres;
	
	private SeekBarPreference mVibrateTime;
	private ListPreference mReminderInterval;
	
	private CheckBoxPreference mAutoStartPrefs;
	private Preference mAboutPrefs;
	private Preference mCollectLog;
	
	private SharedPreferences mSharedPreferences;
	
	private String mVersionName;
	
	private String mAppVersionName = "?";
	private String mAppVersionCode = "?";
	
	private Context mContext;
	
	private CollectLogTask mCollectLogTask;
	
	private String mAdditionalInfo;
	
	private String mFormat = "time";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mContext = this;
        
        addPreferencesFromResource(R.xml.prefs);
        
        PackageManager packageManager = getPackageManager();
        try {
			PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);
			
			mAppVersionName = info.versionName;
			mAppVersionCode = String.valueOf(info.versionCode);
		} catch (NameNotFoundException e) {
			// the current package should be exist all times
		}
		
		mAdditionalInfo = getString(R.string.device_info_fmt, mAppVersionName, Build.MODEL, Build.VERSION.RELEASE, Build.DISPLAY);
        
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
        
        mAutoStartPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_auto_start));
        mAutoStartPrefs.setOnPreferenceChangeListener(this);
        
        mAboutPrefs = findPreference(getString(R.string.prefs_key_about));
        mVersionName = getString(R.string.prefs_summary_about, mAppVersionName);
		mAboutPrefs.setSummary(mVersionName);
		
		mCollectLog = findPreference(getString(R.string.prefs_key_collect_log));
		mCollectLog.setOnPreferenceClickListener(this);
		
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
    

    
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (mCollectLog == preference) {
			collectAndSendLog(mAdditionalInfo, mFormat, null, null);
			
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
		} else if (preference == mAutoStartPrefs) {
			setBootCompleteReceiverEnabled((Boolean) newValue);
		}
		
		return true;
	}
	
	public void setBootCompleteReceiverEnabled(boolean enabled) {
		MyLog.d("enabled = " + enabled);
		PackageManager packageManager = getPackageManager();
		ComponentName componentName = new ComponentName(this, BootCompleteReceiver.class);
		int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
		packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP);
	}
	
	@SuppressWarnings("unchecked")
	void collectAndSendLog(String additionalInfo, String format, String buffer, String[] filterSpecs) {
        /*Usage: logcat [options] [filterspecs]
        options include:
          -s              Set default filter to silent.
                          Like specifying filterspec '*:s'
          -f <filename>   Log to file. Default to stdout
          -r [<kbytes>]   Rotate log every kbytes. (16 if unspecified). Requires -f
          -n <count>      Sets max number of rotated logs to <count>, default 4
          -v <format>     Sets the log print format, where <format> is one of:

                          brief process tag thread raw time threadtime long

          -c              clear (flush) the entire log and exit
          -d              dump the log and then exit (don't block)
          -g              get the size of the log's ring buffer and exit
          -b <buffer>     request alternate ring buffer
                          ('main' (default), 'radio', 'events')
          -B              output the log in binary
        filterspecs are a series of
          <tag>[:priority]

        where <tag> is a log component tag (or * for all) and priority is:
          V    Verbose
          D    Debug
          I    Info
          W    Warn
          E    Error
          F    Fatal
          S    Silent (supress all output)

        '*' means '*:d' and <tag> by itself means <tag>:v

        If not specified on the commandline, filterspec is set from ANDROID_LOG_TAGS.
        If no filterspec is found, filter defaults to '*:I'

        If not specified with -v, format is set from ANDROID_PRINTF_LOG
        or defaults to "brief"*/

        ArrayList<String> list = new ArrayList<String>();
        
        if (format != null) {
            list.add("-v");
            list.add(format);
        }
        
        if (buffer != null) {
            list.add("-b");
            list.add(buffer);
        }

        if (filterSpecs != null){
            for (String filterSpec : filterSpecs) {
                list.add(filterSpec);
            }
        }
        
        mCollectLogTask = (CollectLogTask) new CollectLogTask(additionalInfo).execute(list);
    }
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DLG_COLLECT_LOG_OK: {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.msg_collect_log_success);
			builder.setNegativeButton(android.R.string.ok, null);
			return builder.create(); 
		}
		
		case DLG_COLLECT_LOG_FAIL: {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.msg_collect_log_fail);
			builder.setNeutralButton(android.R.string.ok, null);
			return builder.create();
		}
		default:
			break;
		}
		return super.onCreateDialog(id);
	}
    
    private class CollectLogTask extends AsyncTask<ArrayList<String>, Void, Boolean>{
    	
    	
		private ProgressDialog mProgressDialog;
		private String mAdditionalInfo;
		
    	
        public CollectLogTask(String additionalInfo) {
			super();
			mAdditionalInfo = additionalInfo;
		}

		@Override
        protected void onPreExecute(){
            showProgressDialog(getString(R.string.acquiring_log_progress_dialog_message));
        }
        
        void showProgressDialog(String message){
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(message);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }
        
        @Override
        protected Boolean doInBackground(ArrayList<String>... params){
            final StringBuilder log = new StringBuilder();
            FileWriter fw = null; 
            if (mAdditionalInfo != null){
            	MyLog.d("Add additional info");
                log.append(mAdditionalInfo);
                log.append(LINE_SEPARATOR);
            }
            
            try {
                ArrayList<String> commandLine = new ArrayList<String>();
                commandLine.add("logcat");//$NON-NLS-1$
                commandLine.add("-d");//$NON-NLS-1$
                commandLine.add("-s");
                commandLine.add("CallVibratorApp:D");
                ArrayList<String> arguments = ((params != null) && (params.length > 0)) ? params[0] : null;
                if (null != arguments){
                    commandLine.addAll(arguments);
                }
                
                String[] commandArray = commandLine.toArray(new String[0]);
                
                MyLog.d("Try to execute: " + Arrays.toString(commandArray));
                
				Process process = Runtime.getRuntime().exec(commandArray);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                String line;
                while ((line = bufferedReader.readLine()) != null){ 
                    log.append(line);
                    log.append(LINE_SEPARATOR); 
                }
                
                MyLog.d("Collect log OK");
                
                File appFile = new File(Environment.getExternalStorageDirectory(), APP_LOG);
                
                MyLog.d("Open file to write log: " + appFile.getAbsolutePath());
                
                fw = new FileWriter(appFile);
                
                MyLog.d("Open file OK");
                
                fw.write(log.toString());
                
                MyLog.d("Write log OK");
                
                return true;
            } catch (IOException e){
                MyLog.e("CollectLogTask.doInBackground failed", e);//$NON-NLS-1$
                return false;
            } finally {
            	if (fw != null) {
            		try {
						fw.close();
					} catch (IOException e) {
						MyLog.e("Close FileWriter Error", e);
					}
            	}
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
        	dismissProgressDialog();
            if (result){
            	showDialog(DLG_COLLECT_LOG_OK);
            } else {
            	showDialog(DLG_COLLECT_LOG_FAIL);
            }
        }
        
        
        private void dismissProgressDialog(){
            if (null != mProgressDialog && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }
    }
}
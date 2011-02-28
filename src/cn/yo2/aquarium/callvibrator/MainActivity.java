package cn.yo2.aquarium.callvibrator;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class MainActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	
	private CheckBoxPreference mDialCallPrefs;
	private CheckBoxPreference mAnswerCallPrefs;
	private CheckBoxPreference mEndCallPrefs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.prefs);
        
        mDialCallPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_dial_call));
        mDialCallPrefs.setOnPreferenceChangeListener(this);
        
        mAnswerCallPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_answer_call));
        mAnswerCallPrefs.setOnPreferenceChangeListener(this);
        
        mEndCallPrefs = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_end_call));
        mEndCallPrefs.setOnPreferenceChangeListener(this);
        
        Utils.sendCommand(this, Utils.buidOptionsFromPrefs(this));
    }
    
    
    
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean dialCallChecked = false;
		boolean answerCallChecked = false;
		boolean endCallChecked = false;
		
		if (mDialCallPrefs == preference) {
			dialCallChecked = (Boolean) newValue;
			answerCallChecked = mAnswerCallPrefs.isChecked();
			endCallChecked = mEndCallPrefs.isChecked();
		} else if (mAnswerCallPrefs == preference) {
			dialCallChecked = mDialCallPrefs.isChecked();
			answerCallChecked = (Boolean) newValue;
			endCallChecked = mEndCallPrefs.isChecked();
		} else if (mEndCallPrefs == preference) {
			dialCallChecked = mDialCallPrefs.isChecked();
			answerCallChecked = mAnswerCallPrefs.isChecked();
			endCallChecked = (Boolean) newValue;
		}
		
		Utils.sendCommand(this, Utils.buildOptions(dialCallChecked, answerCallChecked, endCallChecked));
		
		return true;
	}
	
}
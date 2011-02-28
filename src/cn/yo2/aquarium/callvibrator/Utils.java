package cn.yo2.aquarium.callvibrator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utils {
	public static int buidOptionsFromPrefs(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean dialCallChecked = sharedPreferences.getBoolean(context.getString(R.string.prefs_key_dial_call), false);
		boolean answerCallChecked = sharedPreferences.getBoolean(context.getString(R.string.prefs_key_answer_call), false);
		boolean endCallChecked = sharedPreferences.getBoolean(context.getString(R.string.prefs_key_end_call), false);
	
		return buildOptions(dialCallChecked, answerCallChecked, endCallChecked);
	}
	
	public static int buildOptions(boolean dialCallChecked, boolean answerCallChecked, boolean endCallChecked) {
		int options = 0;
		
		if (dialCallChecked) {
			options |= CallStateService.DIAL_CALL;
		} 
		
		if (answerCallChecked) {
			options |= CallStateService.ANSWER_CALL;
		}
		
		if (endCallChecked) {
			options |= CallStateService.END_CALL;
		}
		
		return options;
	}
	
	public static void sendCommand(Context context, int options) {
		
		Intent intent = new Intent(context, CallStateService.class);
		
		if (options == 0) {
			context.stopService(intent);
		} else {
			intent.putExtra(CallStateService.EXTRAS_START_OPTIONS, options);
			context.startService(intent);
		}
	}
	
	
}

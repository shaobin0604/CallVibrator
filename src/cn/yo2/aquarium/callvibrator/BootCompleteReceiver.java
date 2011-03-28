package cn.yo2.aquarium.callvibrator;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
			boolean autoStart = sharedPreferences.getBoolean(context.getString(R.string.prefs_key_auto_start), false);
			
			if (autoStart) {
				Intent service = new Intent(context, CallStateService.class);
		        context.startService(service);
			}
		}
	}

}

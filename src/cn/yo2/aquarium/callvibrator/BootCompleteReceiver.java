package cn.yo2.aquarium.callvibrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import cn.yo2.aquarium.logutils.MyLog;

public class BootCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		MyLog.d("intent ACTION = " + action);
		
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
			boolean autoStart = sharedPreferences.getBoolean(context.getString(R.string.prefs_key_auto_start), false);
			
			if (autoStart) {
				Intent service = new Intent(context, CallStateService.class);
		        context.startService(service);
			}
		}
		
	}
}

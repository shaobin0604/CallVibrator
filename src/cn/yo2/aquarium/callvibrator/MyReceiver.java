package cn.yo2.aquarium.callvibrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import cn.yo2.aquarium.logutils.MyLog;

public class MyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		MyLog.d("intent ACTION = " + action);
		
		if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
			Intent service = new Intent(context, CallStateService.class);
	        context.startService(service);
		} else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
				Intent service = new Intent(context, CallStateService.class);
				context.startService(service);
			}
		}
		
	}
}

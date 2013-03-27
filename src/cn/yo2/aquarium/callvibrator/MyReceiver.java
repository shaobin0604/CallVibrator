
package cn.yo2.aquarium.callvibrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import cn.yo2.aquarium.logutils.Slog;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Slog.d("intent ACTION = " + action);

        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            Intent service = new Intent(context, CallStateService.class);
            service.setAction(CallStateService.ACTION_OUTGOING_CALL);
            service.putExtra(Intent.EXTRA_PHONE_NUMBER, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
            context.startService(service);
        } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {

            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                Intent service = new Intent(context, CallStateService.class);
                service.setAction(CallStateService.ACTION_INCOMING_CALL);
                service.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER,
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
                context.startService(service);
            }
        }

    }
}

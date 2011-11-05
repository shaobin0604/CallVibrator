package cn.yo2.aquarium.callvibrator;

import cn.yo2.aquarium.logutils.MyLog;
import android.app.Application;
import android.content.res.Configuration;

public class CallVibratorApp extends Application {
	private static final String TAG = CallVibratorApp.class.getSimpleName();

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		MyLog.d("onConfigurationChanged() newConfig = " + newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		MyLog.initLog(TAG, true, false);
		MyLog.d("onCreate()");
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		MyLog.d("onLowMemory()");
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		MyLog.d("onTerminate()");
	}
	
}

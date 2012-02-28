package cn.yo2.aquarium.callvibrator;

import android.app.Application;
import android.content.res.Configuration;
import cn.yo2.aquarium.logutils.MyLog;

public class CallVibratorApp extends Application {
	static final String TAG = CallVibratorApp.class.getSimpleName();

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		MyLog.d("onConfigurationChanged() newConfig = " + newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		MyLog.initLog(TAG, true, true); // only log level above info in release version
//		MyLog.initLog(TAG, true, false); // log everything in debug version
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

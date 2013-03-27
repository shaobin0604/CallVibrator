package cn.yo2.aquarium.callvibrator;

import android.app.Application;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import cn.yo2.aquarium.logutils.Slog;

public class CallVibratorApp extends Application {
	static final String TAG = CallVibratorApp.class.getSimpleName();
	
    private static final int EXECUTE_COMMAND_WAIT_TIME = 5 * 1000;
    private static final String READ_LOGS_PERMISSION = "android.permission.READ_LOGS";
    
    public static final int OUTGOING_CALL_AVAILABLE = 0;
    public static final int OUTGOING_CALL_UNAVAILABLE_NO_SU = 1;
    public static final int OUTGOING_CALL_UNAVAILABLE_ACCESS_NOT_GIVEN = 2;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_IO_EXCEPTION = 3;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_ROOTTOOLS_EXCEPTION = 4;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_TIMEOUT_EXCEPTION = 5;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Slog.d("onConfigurationChanged() newConfig = " + newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
//		MyLog.initLog(TAG, true, true); // only log level above info in release version
		Slog.initLog(TAG, true, false); // log everything in debug version
		Slog.d("onCreate()");
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Slog.d("onLowMemory()");
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		Slog.d("onTerminate()");
	}
	
	public boolean isReadLogsPermissionGranted() {
		PackageManager pm = getPackageManager();
		return pm.checkPermission(READ_LOGS_PERMISSION, getPackageName()) == PackageManager.PERMISSION_GRANTED;
	}
	
    public boolean isSDKVersionBelowJellyBean() {
        return Build.VERSION.SDK_INT < 16;
    }
	
	/*
     * Google has changed policy of android.permission.READ_LOGS in Jelly Bean
     * 
     * see https://github.com/shaobin0604/CallVibrator/issues/1
     */
    public int checkOutgoingCallFeasible() {
        if (isSDKVersionBelowJellyBean()) {
            Slog.i("SDK below Jelly Bean");
            return OUTGOING_CALL_AVAILABLE;
        }
        Slog.i("SDK above Jelly Bean");

        if (!RootTools.isRootAvailable()) {
            Slog.i("root not available");
            return OUTGOING_CALL_UNAVAILABLE_NO_SU;
        }
        Slog.i("root available");

        if (!RootTools.isAccessGiven()) {
            Slog.i("root access not given");
            return OUTGOING_CALL_UNAVAILABLE_ACCESS_NOT_GIVEN;
        }
        Slog.i("root access given");
        
        final String grantPermissionCommand = String.format("pm grant %s %s", getPackageName(), READ_LOGS_PERMISSION);
        
        try {
            List<String> output = RootTools.sendShell(
                    grantPermissionCommand, EXECUTE_COMMAND_WAIT_TIME);
            for (String outputline : output) {
                Slog.i(grantPermissionCommand + " returns: " + outputline);
            }
           	return OUTGOING_CALL_AVAILABLE;
        } catch (IOException e) {
            Slog.e("Error grant permission ", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_IO_EXCEPTION;
        } catch (RootToolsException e) {
            Slog.e("Error grant permission ", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_ROOTTOOLS_EXCEPTION;
        } catch (TimeoutException e) {
            Slog.e("Error grant permission ", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_TIMEOUT_EXCEPTION;
        }
    }


}

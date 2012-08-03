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

import cn.yo2.aquarium.logutils.MyLog;

public class CallVibratorApp extends Application {
	static final String TAG = CallVibratorApp.class.getSimpleName();
	
    private static final int EXECUTE_COMMAND_WAIT_TIME = 5 * 1000;
    private static final String GRANT_READ_LOGS_PERMISSION_COMMAND = "pm grant cn.yo2.aquarium.callvibrator android.permission.READ_LOGS";
    
    public static final int OUTGOING_CALL_AVAILABLE = 0;
    public static final int OUTGOING_CALL_UNAVAILABLE_NO_SU = 1;
    public static final int OUTGOING_CALL_UNAVAILABLE_ACCESS_NOT_GIVEN = 2;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_IO_EXCEPTION = 3;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_ROOTTOOLS_EXCEPTION = 4;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_TIMEOUT_EXCEPTION = 5;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		MyLog.d("onConfigurationChanged() newConfig = " + newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
//		MyLog.initLog(TAG, true, true); // only log level above info in release version
		MyLog.initLog(TAG, true, false); // log everything in debug version
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
	
	public boolean isReadLogsPermissionGranted() {
		PackageManager pm = getPackageManager();
		return pm.checkPermission("android.permission.READ_LOGS", getPackageName()) == PackageManager.PERMISSION_GRANTED;
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
            MyLog.i("SDK below Jelly Bean");
            return OUTGOING_CALL_AVAILABLE;
        }
        MyLog.i("SDK above Jelly Bean");

        if (!RootTools.isRootAvailable()) {
            MyLog.i("root not available");
            return OUTGOING_CALL_UNAVAILABLE_NO_SU;
        }
        MyLog.i("root available");

        if (!RootTools.isAccessGiven()) {
            MyLog.i("root access not given");
            return OUTGOING_CALL_UNAVAILABLE_ACCESS_NOT_GIVEN;
        }
        MyLog.i("root access given");
        
        try {
            List<String> output = RootTools.sendShell(
                    GRANT_READ_LOGS_PERMISSION_COMMAND, EXECUTE_COMMAND_WAIT_TIME);
            for (String outputline : output) {
                MyLog.i(GRANT_READ_LOGS_PERMISSION_COMMAND + "return: " + outputline);
            }
            
            if (isReadLogsPermissionGranted()) {
            	return OUTGOING_CALL_AVAILABLE;
            } else {
            	return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_ROOTTOOLS_EXCEPTION;
            }
            
        } catch (IOException e) {
            // something went wrong, deal with it here
            e.printStackTrace();
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_IO_EXCEPTION;
        } catch (RootToolsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_ROOTTOOLS_EXCEPTION;
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_TIMEOUT_EXCEPTION;
        }
    }


}

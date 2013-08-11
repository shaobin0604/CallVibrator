package cn.yo2.aquarium.callvibrator;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.app.Application;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import cn.yo2.aquarium.logutils.MyLog;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.exceptions.RootToolsException;
import com.stericson.RootTools.execution.Command;

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
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_INTERRUPTED_EXCEPTION = 6;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_DENIED_EXCEPTION = 7;
    public static final int OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_UNKNOWN_ERROR = 8;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		MyLog.d("onConfigurationChanged() newConfig = " + newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {
		    MyLog.initLog(TAG, true, false); // log everything in debug version
		} else {
		    MyLog.initLog(TAG, true, true); // only log level above info in release version
		}
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
        
        final String grantPermissionCommand = String.format("pm grant %s %s", getPackageName(), READ_LOGS_PERMISSION);
        
        try {
            Command command = new Command(0, EXECUTE_COMMAND_WAIT_TIME, grantPermissionCommand) {
                
                @Override
                public void output(int id, String line) {
                    MyLog.i(grantPermissionCommand + " returns: " + line);
                }
            };
            RootTools.getShell(true).add(command).waitForFinish();
            
            Thread.sleep(1000); // sleep 1 second
            
            boolean ret = isReadLogsPermissionGranted();
            
            MyLog.d("is read log permission granted: " + ret);
            
           	return (ret ? OUTGOING_CALL_AVAILABLE : OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_UNKNOWN_ERROR);
        } catch (IOException e) {
            MyLog.e("Error grant permission ", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_IO_EXCEPTION;
        } catch (TimeoutException e) {
            MyLog.e("Error grant permission ", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_TIMEOUT_EXCEPTION;
        } catch (InterruptedException e) {
            MyLog.e("Error grant permission", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_INTERRUPTED_EXCEPTION;
        } catch (RootDeniedException e) {
            MyLog.e("Error grant permission", e);
            return OUTGOING_CALL_UNAVAILABLE_EXECUTE_COMMAND_DENIED_EXCEPTION;
        }
    }


}

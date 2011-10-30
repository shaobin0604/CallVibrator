package cn.yo2.aquarium.callvibrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import cn.yo2.aquarium.logutils.MyLog;

public class CallStateService extends Service {
	private static final int ONE_MINUTE_IN_MILLIS = 60 * 1000;
	
	public static final int DEFAULT_VIBRATE_TIME = 80;
	public static final String DEFAULT_REMINDER_TIME_STR = "45";
	
	private TelephonyManager mTelephonyManager;
	private int mLastCallState;
	private int mCurrCallState;

	private Vibrator mVibrator;

	private OutgoingCallWorkerThread mWorkerThread;
	
	private boolean mInCall;
	
	private boolean mReminder;
	private int mReminderIntervalMillis;
	
	private boolean mShowNotification;
	
	private boolean mListenOutgoingCall;
	private boolean mListenIncomingCall;
	private boolean mListenEndCall;

	private long mVibrateTime;
	
	private static final int WHAT_REMINDER_VIBRATE = 1;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT_REMINDER_VIBRATE: {
				if (mReminder && mInCall) {
					MyLog.d("one minute plus interval time out, vibrate");
					mVibrator.vibrate(mVibrateTime);
					MyLog.d("wait 1 minute for next vibrate");
					this.sendEmptyMessageDelayed(WHAT_REMINDER_VIBRATE, ONE_MINUTE_IN_MILLIS);
				}
				break;
			}
			default:
				break;
			}
		}
		
	};

	private PhoneStateListener mListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			mLastCallState = mCurrCallState;
			mCurrCallState = state;
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE: {
				MyLog.d("[onCallStateChanged] TelephonyManager.CALL_STATE_IDLE -> "
						+ incomingNumber);
				
				mInCall = false;
				mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
				
				if (mListenEndCall) {
					if (mLastCallState == TelephonyManager.CALL_STATE_OFFHOOK || mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
						MyLog.i("[onCallStateChanged] Call End, >>>>> vibrate <<<<<");
						
						mVibrator.vibrate(mVibrateTime);
					}
				}
				
				stopWorkerThread();
				
				break;
			}
			case TelephonyManager.CALL_STATE_OFFHOOK: {
				MyLog.d("TelephonyManager.CALL_STATE_OFFHOOK -> "
						+ incomingNumber);

				if (mLastCallState == TelephonyManager.CALL_STATE_IDLE) {
					MyLog.d("[onCallStateChanged] start outgoing call, ring");
					if (mListenOutgoingCall || mReminder ) {
						MyLog.d("[onCallStateChanged] outgoing call, start worker thread");
						startWorkerThread();
					}
				} else if (mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
					MyLog.d("[onCallStateChanged] incoming call in call");
					mInCall = true;
					if (mListenIncomingCall) {
						MyLog.i("[onCallStateChanged] incoming Call answered, >>>>> vibrate <<<<<");
						mVibrator.vibrate(mVibrateTime);
					}
					
				}
				break;
			}
			case TelephonyManager.CALL_STATE_RINGING: {
				MyLog.d("[onCallStateChanged] TelephonyManager.CALL_STATE_RINGING -> "
						+ incomingNumber);
				if (mLastCallState == TelephonyManager.CALL_STATE_IDLE) {
					MyLog.d("[onCallStateChanged] call state from idle to ringing");
				} else if (mLastCallState == TelephonyManager.CALL_STATE_OFFHOOK) {
					MyLog.d("[onCallStateChanged] call state from offhook to ringing");
				}
				
				break;
			}
			default:
				break;
			}
		}

	};
	
	private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
	
	private static CharSequence formatTimeStr(long inTimeInMillis) {
		return DF.format(new Date(inTimeInMillis));
	}

	private void startWorkerThread() {
		stopWorkerThread();

		mWorkerThread = new OutgoingCallWorkerThread();

		mWorkerThread.start();
	}

	private void stopWorkerThread() {
		if (mWorkerThread != null && mWorkerThread.isAlive()) {
			mWorkerThread.requestKill();
			
			try {
				MyLog.d("Before join WorkerThread");
				mWorkerThread.join();
				MyLog.d("After join WorkerThread");
			} catch (InterruptedException e) {
				// TODO: handle exception
				MyLog.e("mWorkerThread.join() interrupted");
			}
		}

		
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		MyLog.d("OnCreate");

		mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

		mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		mListenOutgoingCall = sharedPreferences.getBoolean(getString(R.string.prefs_key_outgoing_call), false);
		mListenIncomingCall = sharedPreferences.getBoolean(getString(R.string.prefs_key_incoming_call), false);
		mListenEndCall = sharedPreferences.getBoolean(getString(R.string.prefs_key_end_call), false);
		
		mReminder = sharedPreferences.getBoolean(getString(R.string.prefs_key_reminder), false);

		MyLog.d("Outgoing Call -> " + mListenOutgoingCall);
		MyLog.d("Incoming Call -> " + mListenIncomingCall);
		MyLog.d("End      Call -> " + mListenEndCall);
		MyLog.d("Reminder      -> " + mReminder);
		
		if (!mListenOutgoingCall && !mListenIncomingCall && !mListenEndCall && !mReminder ) {
			MyLog.d("Nothing to listen, stop service");
			
			stopSelf(startId);
			return START_STICKY;
		}
		
		mVibrateTime = Integer.valueOf(sharedPreferences.getInt(getString(R.string.prefs_key_vibrate_time), DEFAULT_VIBRATE_TIME));
		MyLog.d(String.format("Vibrate Time =  %d ms", mVibrateTime));
		
		mReminderIntervalMillis = Integer.valueOf(sharedPreferences.getString(getString(R.string.prefs_key_reminder_interval), DEFAULT_REMINDER_TIME_STR)) * 1000;
		MyLog.d(String.format("Reminder[%s], millis =  %d", String.valueOf(mReminder), mReminderIntervalMillis));
		
		mShowNotification = sharedPreferences.getBoolean(getString(R.string.prefs_key_show_notification), false);
		
		MyLog.d("Show Notification = " + mShowNotification);
		
		if (mShowNotification) {
			showNotification();
		} else {
			cancelNotification();
		}
		
		return START_STICKY;
	}

	private void cancelNotification() {
		stopForeground(true);
	}

	private void showNotification() {
		Notification notification = new Notification(R.drawable.ic_stat, getString(R.string.stat_running), System.currentTimeMillis());
		
		final String on = getString(R.string.on);
		final String off = getString(R.string.off);
		
		String contentText = getString(R.string.content_text, 
				mListenOutgoingCall ? on : off,
				mListenIncomingCall ? on : off,
				mListenEndCall ? on :off);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		
		notification.setLatestEventInfo(this, getString(R.string.stat_running), contentText, contentIntent);
		
		startForeground(R.xml.prefs, notification);
	}

	private class OutgoingCallWorkerThread extends Thread {

		private static final int CONNECT_TIME_DELAY = 1000;
		
		private volatile boolean mIsThreadKill;

		public synchronized void requestKill() {
			mIsThreadKill = true;
		}

		public synchronized boolean killRequested() {
			return mIsThreadKill;
		}

		public void run() {
			Process process = null;
			BufferedReader reader = null;
			try {
				Runtime runtime = Runtime.getRuntime();
				// run logcat
				process = runtime.exec("/system/bin/logcat -b radio -s GSM:D");
			
				reader = new BufferedReader(new InputStreamReader(process
						.getInputStream()));
			
				String line;
			
				while (!killRequested()) {
					line = reader.readLine();
			
					if (!TextUtils.isEmpty(line)) {
						logLine(line);
					}
				}
			} catch (IOException e) {
				MyLog.e("Error when execute logcat", e);
			} finally {
				MyLog.d("Exit close resource");
			
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						MyLog.e("Reader close error", e);
					}
			
				}
			
				if (process != null) {
					process.destroy();
				}
			}
		}

		private void logLine(String line) {
			MyLog.d(line);

			long time = System.currentTimeMillis();

			Pattern pattern = Pattern
					.compile("\\[GSMConn\\] onConnectedInOrOut: connectTime=(\\d+)");

			Matcher matcher = pattern.matcher(line);

			if (matcher.find()) {
				long connectTime = Long.valueOf(matcher.group(1));

				MyLog.d("now     time -> " + formatTimeStr(time));
				MyLog.d("connect time -> " + formatTimeStr(connectTime));

				if (Math.abs(time - connectTime) <= CONNECT_TIME_DELAY) {
					MyLog.i(">>>>> outgoing call in call <<<<<");
					
					mInCall = true;
					
					if (mListenOutgoingCall) {
						MyLog.i("Outgoing call in call, >>>>> Vibrate <<<<<");
						mVibrator.vibrate(mVibrateTime);
					}
					
					if (mReminder) {
						MyLog.i("[outgoing call in call] --> send vibrate interval delayed " + mReminderIntervalMillis + " millis");
						
						mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
						mHandler.sendEmptyMessageDelayed(WHAT_REMINDER_VIBRATE, mReminderIntervalMillis);
					}
					
					
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		MyLog.d("OnDestroy");
		mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
		
		stopWorkerThread();
		
		stopForeground(true);

		mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
	}
}

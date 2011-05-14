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
import android.util.Log;

public class CallStateService extends Service {
	private static final String TAG = CallStateService.class.getSimpleName();
	
	private static final int ONE_MINUTE_IN_MILLIS = 60 * 1000;
	
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
	private static final int WHAT_REMINDER_ONE_MINUTE = 2;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT_REMINDER_VIBRATE:
				if (mReminder && mInCall) {
					Log.d(TAG, "one minute plus interval time out, vibrate");
					mVibrator.vibrate(80);
				}
				break;
			case WHAT_REMINDER_ONE_MINUTE:
				if (mReminder && mInCall) {
					Log.d(TAG, "one minute time out, send interval time out delayed " + mReminderIntervalMillis + " millis");
					this.sendEmptyMessageDelayed(WHAT_REMINDER_VIBRATE, mReminderIntervalMillis);
					
					Log.d(TAG, "one minute time out, send one minute time out delayed");
					this.sendEmptyMessageDelayed(WHAT_REMINDER_ONE_MINUTE, ONE_MINUTE_IN_MILLIS);
				}
				break;
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
			case TelephonyManager.CALL_STATE_IDLE:
				Log.d(TAG, "[onCallStateChanged] TelephonyManager.CALL_STATE_IDLE -> "
						+ incomingNumber);
				
				mInCall = false;
				mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
				mHandler.removeMessages(WHAT_REMINDER_ONE_MINUTE);
				
				if (mListenEndCall) {
					if (mLastCallState == TelephonyManager.CALL_STATE_OFFHOOK || mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
						Log.d(TAG, "[onCallStateChanged] Call End, vibrate");
						
						mVibrator.vibrate(mVibrateTime);
					}
				}
				
				stopWorkerThread();
				
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.d(TAG, "TelephonyManager.CALL_STATE_OFFHOOK -> "
						+ incomingNumber);

				if (mLastCallState == TelephonyManager.CALL_STATE_IDLE) {
					Log.d(TAG, "[onCallStateChanged] start outgoing call, ring");
					if (mListenOutgoingCall || mReminder ) {
						Log.d(TAG, "[onCallStateChanged] outgoing call, start worker thread");
						startWorkerThread();
					}
				} else if (mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
					Log.d(TAG, "[onCallStateChanged] incoming call in call");
					mInCall = true;
					if (mListenIncomingCall) {
						Log.d(TAG, "[onCallStateChanged] incoming Call answered, vibrate");
						mVibrator.vibrate(mVibrateTime);
					}
					
					if (mReminder) {
						Log.d(TAG, "[incoming call in call] --> send vibrate interval delayed " + mReminderIntervalMillis + " millis");
						
						mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
						mHandler.sendEmptyMessageDelayed(WHAT_REMINDER_VIBRATE, mReminderIntervalMillis);
						
						Log.d(TAG, "[incoming call in call] --> send one minute delayed");
						
						mHandler.removeMessages(WHAT_REMINDER_ONE_MINUTE);
						mHandler.sendEmptyMessageDelayed(WHAT_REMINDER_ONE_MINUTE, ONE_MINUTE_IN_MILLIS);
					}
				}
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				
				Log.d(TAG, "[onCallStateChanged] TelephonyManager.CALL_STATE_RINGING -> "
						+ incomingNumber);
				break;

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
				Log.d(TAG, "Before join WorkerThread");
				mWorkerThread.join();
				Log.d(TAG, "After join WorkerThread");
			} catch (InterruptedException e) {
				// TODO: handle exception
				Log.e(TAG, "mWorkerThread.join() interrupted");
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

		Log.d(TAG, "OnCreate");

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

		Log.d(TAG, "Outgoing Call -> " + mListenOutgoingCall);
		Log.d(TAG, "Incoming Call -> " + mListenIncomingCall);
		Log.d(TAG, "End      Call -> " + mListenEndCall);
		Log.d(TAG, "Reminder      -> " + mReminder);
		
		if (!mListenOutgoingCall && !mListenIncomingCall && !mListenEndCall && !mReminder ) {
			Log.d(TAG, "Nothing to listen, stop service");
			
			stopSelf(startId);
			return START_STICKY;
		}
		
		mVibrateTime = Integer.valueOf(sharedPreferences.getString(getString(R.string.prefs_key_vibrate_time), "80"));
		Log.d(TAG, String.format("Vibrate Time =  %d ms", mVibrateTime));
		
		mReminderIntervalMillis = Integer.valueOf(sharedPreferences.getString(getString(R.string.prefs_key_reminder_interval), "45")) * 1000;
		Log.d(TAG, String.format("Reminder[%s], millis =  %d", String.valueOf(mReminder), mReminderIntervalMillis));
		
		mShowNotification = sharedPreferences.getBoolean(getString(R.string.prefs_key_show_notification), false);
		
		Log.d(TAG, "Show Notification = " + mShowNotification);
		
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
				Log.e(TAG, "Error when execute logcat", e);
			} finally {
				Log.i(TAG, "Exit close resource");
			
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.e(TAG, "Reader close error", e);
					}
			
				}
			
				if (process != null) {
					process.destroy();
				}
			}
		}

		private void logLine(String line) {
			Log.d(TAG, line);

			long time = System.currentTimeMillis();

			Pattern pattern = Pattern
					.compile("\\[GSMConn\\] onConnectedInOrOut: connectTime=(\\d+)");

			Matcher matcher = pattern.matcher(line);

			if (matcher.find()) {
				long connectTime = Long.valueOf(matcher.group(1));

				Log.d(TAG, "now     time -> " + formatTimeStr(time));
				Log.d(TAG, "connect time -> " + formatTimeStr(connectTime));

				if (Math.abs(time - connectTime) <= CONNECT_TIME_DELAY) {
					Log.d(TAG, "************ outgoing call in call ***************");
					
					mInCall = true;
					
					if (mListenOutgoingCall) {
						mVibrator.vibrate(mVibrateTime);
					}
					
					if (mReminder) {
						Log.d(TAG, "[outgoing call in call] --> send vibrate interval delayed " + mReminderIntervalMillis + " millis");
						
						mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
						mHandler.sendEmptyMessageDelayed(WHAT_REMINDER_VIBRATE, mReminderIntervalMillis);
						
						Log.d(TAG, "[outgoing call in call] --> send one minute delayed");
						
						mHandler.removeMessages(WHAT_REMINDER_ONE_MINUTE);
						mHandler.sendEmptyMessageDelayed(WHAT_REMINDER_ONE_MINUTE, ONE_MINUTE_IN_MILLIS);
					}
					
					
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(TAG, "OnDestroy");
		mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
		mHandler.removeMessages(WHAT_REMINDER_ONE_MINUTE);
		
		stopWorkerThread();
		
		stopForeground(true);

		mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
	}
}

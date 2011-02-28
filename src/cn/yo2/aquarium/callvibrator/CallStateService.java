package cn.yo2.aquarium.callvibrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class CallStateService extends Service {
	private static final String PACKAGE = CallStateService.class.getPackage().getName();
	private static final String TAG = CallStateService.class.getSimpleName();
	
	
	public static final int DIAL_CALL   = 0x00000001;
	public static final int ANSWER_CALL = 0x00000002;
	public static final int END_CALL    = 0x00000004;
	
	public static final String EXTRAS_START_OPTIONS = PACKAGE + ".extras";

	private TelephonyManager mTelephonyManager;
	private int mLastCallState;
	private int mCurrCallState;

	private Vibrator mVibrator;

	private DialCallWorkerThread mWorkerThread;
	
	private boolean mListenDialCall;
	private boolean mListenAnswerCall;
	private boolean mListenEndCall;

	private PhoneStateListener mListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			mLastCallState = mCurrCallState;
			mCurrCallState = state;
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				Log.d(TAG, "TelephonyManager.CALL_STATE_IDLE -> "
						+ incomingNumber);
				
				if (mListenEndCall) {
					if (mLastCallState == TelephonyManager.CALL_STATE_OFFHOOK || mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
						Log.d(TAG, "Call End, vibrate");
						
						vibrate();
					}
				}
				
				stopWorkerThread();
				
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.d(TAG, "TelephonyManager.CALL_STATE_OFFHOOK -> "
						+ incomingNumber);
				if (mListenDialCall) {
					if (mLastCallState == TelephonyManager.CALL_STATE_IDLE) {
						Log.d(TAG, "Dial Call, start worker thread");
						startWorkerThread();
					}
				}
				
				if (mListenAnswerCall) {
					if (mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
						Log.d(TAG, "Answer Call, vibrate");
						vibrate();
					}
				}
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				Log.d(TAG, "TelephonyManager.CALL_STATE_RINGING -> "
						+ incomingNumber);
				break;

			default:
				break;
			}
		}

	};

	private void startWorkerThread() {
		stopWorkerThread();

		mWorkerThread = new DialCallWorkerThread();

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
		Bundle extras = intent.getExtras();
		
		if (extras != null) {
			int options = extras.getInt(EXTRAS_START_OPTIONS);
			
			if (options != 0) {
				mListenDialCall = ((options & DIAL_CALL) != 0);
				mListenAnswerCall = ((options & ANSWER_CALL) != 0);
				mListenEndCall = ((options & END_CALL) != 0);
			}
		}
		
		Log.d(TAG, "Dial Call   -> " + mListenDialCall);
		Log.d(TAG, "Answer Call -> " + mListenAnswerCall);
		Log.d(TAG, "End Call    -> " + mListenEndCall);
		
		Notification notification = new Notification(R.drawable.ic_stat, getString(R.string.stat_running), System.currentTimeMillis());
		
		final String on = getString(R.string.on);
		final String off = getString(R.string.off);
		
		String contentText = getString(R.string.content_text, 
				mListenDialCall ? on : off,
				mListenAnswerCall ? on : off,
				mListenEndCall ? on :off);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		
		notification.setLatestEventInfo(this, getString(R.string.stat_running), contentText, contentIntent);
		
		startForeground(R.xml.prefs, notification);
		
		return START_STICKY;
	}

	private class DialCallWorkerThread extends Thread {

		private volatile boolean mIsThreadKill;

		public synchronized void requestKill() {
			mIsThreadKill = true;
		}

		public synchronized boolean killRequested() {
			return mIsThreadKill;
		}

		public void run() {
			runLog();
		}

		private void runLog() {
			Process process = null;
			BufferedReader reader = null;
			try {
				Runtime runtime = Runtime.getRuntime();

				// TODO: not good, try another method
				// clean log
				// runtime.exec("/system/bin/logcat -c -b radio");

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

				Log.d(TAG, "time -> " + time);
				Log.d(TAG, "connectTime -> " + connectTime);

				if (Math.abs(time - connectTime) < 100) {
					Log.d(TAG, "************ Vibrate ***************");

					vibrate();
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(TAG, "OnDestroy");

		stopWorkerThread();
		
		stopForeground(true);

		mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
	}

	private void vibrate() {
		mVibrator.vibrate(50);
	}
}


package cn.yo2.aquarium.callvibrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
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

    // private AlarmManager mAlarmManager;

    private WakeLock mWakeLock;

    private int mLastCallState;
    private int mCurrCallState;

    private Vibrator mVibrator;

    private OutgoingCallWorkerThread mWorkerThread;

    private boolean mInCall;

    private boolean mReminder;
    private int mReminderIntervalMillis;

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

    // -------------- use AlarmManager to do timer -----------------------------

    // private static final String ACTION_REMINDER_VIBRATE =
    // "ACTION_REMINDER_VIBRATE";
    //
    // private BroadcastReceiver mTimerReceiver = new BroadcastReceiver() {
    //
    // @Override
    // public void onReceive(Context context, Intent intent) {
    // if (ACTION_REMINDER_VIBRATE.equals(intent.getAction())) {
    // if (mReminder && mInCall) {
    // MyLog.d("one minute plus interval time out, vibrate");
    // mVibrator.vibrate(mVibrateTime);
    // MyLog.d("wait 1 minute for next vibrate");
    //
    // PendingIntent operation = PendingIntent.getBroadcast(context, 0, new
    // Intent(ACTION_REMINDER_VIBRATE), 0);
    //
    // mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
    // SystemClock.elapsedRealtime() + ONE_MINUTE_IN_MILLIS, operation);
    // }
    // }
    // }
    // };

    // -------------------------------------------------------------------------

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

                    if (mLastCallState == TelephonyManager.CALL_STATE_OFFHOOK
                            || mLastCallState == TelephonyManager.CALL_STATE_RINGING) {
                        if (mListenEndCall) {
                            MyLog.i("[onCallStateChanged] Call End, >>>>> vibrate <<<<<");
                            mVibrator.vibrate(mVibrateTime);
                        }
                        MyLog.i("[onCallStateChanged] Call End from offhook or ringing, stop self");
                        stopSelf();
                    }

                    stopWorkerThread();

                    releaseWakeLock();

                    break;
                }
                case TelephonyManager.CALL_STATE_OFFHOOK: {
                    MyLog.d("TelephonyManager.CALL_STATE_OFFHOOK -> "
                            + incomingNumber);

                    if (mLastCallState == TelephonyManager.CALL_STATE_IDLE) {
                        MyLog.d("[onCallStateChanged] start outgoing call, ring");
                        if (mListenOutgoingCall || mReminder) {
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

    private boolean isCDMASim() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telephonyManager.getSimState();
        if (simState != TelephonyManager.SIM_STATE_READY) {
            // unknown sim op
            return false;
        }

        /*
         * China Mobile: 46000, 46002, 46007 China Unicom: 46001 China Telecom:
         * 46003
         */
        String simOp = telephonyManager.getSimOperator();

        return "46003".equals(simOp);
    }

    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    private static CharSequence formatTimeStr(long inTimeInMillis) {
        return DF.format(new Date(inTimeInMillis));
    }

    private void startWorkerThread() {
        stopWorkerThread();

        int mode = isCDMASim() ? OutgoingCallWorkerThread.OPERATOR_MODE_CDMA
                : OutgoingCallWorkerThread.OPERATOR_MODE_GSM;

        mWorkerThread = new OutgoingCallWorkerThread(mode);
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

    private void acquireWakeLock() {
        MyLog.d("try acquireWakeLock");
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, CallVibratorApp.TAG);
            mWakeLock.acquire();
            MyLog.d("acquireWakeLock OK");
        }

    }

    private void releaseWakeLock() {
        MyLog.d("try releaseWakeLock");
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
            MyLog.d("releaseWakeLock OK");
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

        // mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
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

        if (!mListenOutgoingCall && !mListenIncomingCall && !mListenEndCall && !mReminder) {
            MyLog.d("Nothing to listen, stop service");

            stopSelf(startId);
            return START_STICKY;
        }

        mVibrateTime = Integer.valueOf(sharedPreferences.getInt(getString(R.string.prefs_key_vibrate_time),
                DEFAULT_VIBRATE_TIME));
        MyLog.d(String.format("Vibrate Time =  %d ms", mVibrateTime));

        mReminderIntervalMillis = Integer.valueOf(sharedPreferences.getString(
                getString(R.string.prefs_key_reminder_interval), DEFAULT_REMINDER_TIME_STR)) * 1000;
        MyLog.d(String.format("Reminder[%s], millis =  %d", String.valueOf(mReminder), mReminderIntervalMillis));

        return START_STICKY;
    }

    private class OutgoingCallWorkerThread extends Thread {
        public static final int OPERATOR_MODE_GSM = 0;
        public static final int OPERATOR_MODE_CDMA = 1;

        private static final int CONNECT_TIME_DELAY = 1000;

        private volatile boolean mIsRequestKill;
        private volatile boolean mProcessDestroied;

        private int mOperatorMode;
        private Process mProcess;

        public OutgoingCallWorkerThread() {
            this(OPERATOR_MODE_GSM);
        }

        public OutgoingCallWorkerThread(int operatorMode) {
            super();
            mOperatorMode = operatorMode;
        }

        public synchronized void requestKill() {
            mIsRequestKill = true;
            destroyProcess();
        }

        public synchronized boolean killRequested() {
            return mIsRequestKill;
        }

        public void run() {
            MyLog.d("OutgoingCallWorkerThread in mode = " + (mOperatorMode == 0 ? "GSM" : "CDMA"));

            // run logcat
            String regex = "\\[GSMConn\\] onConnectedInOrOut: connectTime=(\\d+)";
            String command = "/system/bin/logcat -b radio -s GsmConnection:D GSM:D";
            switch (mOperatorMode) {
                case OPERATOR_MODE_CDMA:
                    command = "/system/bin/logcat -b radio -s CDMA:D";
                    regex = "\\[CDMAConn\\] onConnectedInOrOut: connectTime=(\\d+)";
                    break;
                default:
                    break;
            }

            Pattern pattern = Pattern.compile(regex);
            mProcess = null;
            BufferedReader reader = null;
            try {
                Runtime runtime = Runtime.getRuntime();

                mProcess = runtime.exec(command);

                reader = new BufferedReader(new InputStreamReader(mProcess
                        .getInputStream()));

                String line;

                while (!killRequested()) {
                    MyLog.d(">>>>> readLine");
                    line = reader.readLine();
                    MyLog.d("<<<<< readLine");
                    if (!TextUtils.isEmpty(line)) {
                        logLine(pattern, line);
                    }
                }
                mProcess.waitFor();
            } catch (IOException e) {
                MyLog.e("Error when execute logcat", e);
            } catch (InterruptedException e) {
                MyLog.e("wait logcat process interrupted", e);
            } finally {
                MyLog.d("Exit close resource");

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        MyLog.e("Reader close error", e);
                    }

                }

                destroyProcess();
            }
        }

        private void destroyProcess() {
            if (mProcess != null && !mProcessDestroied) {
                mProcess.destroy();
                mProcessDestroied = true;
            }
        }

        private void logLine(Pattern pattern, String line) {
            MyLog.d(line);

            long time = System.currentTimeMillis();

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
                        MyLog.i("[outgoing call in call] --> send vibrate interval delayed " + mReminderIntervalMillis
                                + " millis");

                        mHandler.removeMessages(WHAT_REMINDER_VIBRATE);
                        mHandler.sendEmptyMessageDelayed(WHAT_REMINDER_VIBRATE, mReminderIntervalMillis);

                        acquireWakeLock();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.d("OnDestroy E");

        mHandler.removeMessages(WHAT_REMINDER_VIBRATE);

        stopWorkerThread();

        stopForeground(true);

        mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);

        releaseWakeLock();

        MyLog.d("OnDestroy X");
    }
}

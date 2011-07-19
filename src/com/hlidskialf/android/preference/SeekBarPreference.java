/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.hlidskialf.android.preference;

import cn.yo2.aquarium.callvibrator.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	private SeekBar mSeekBar;
	private TextView mValueText;
	private Context mContext;

	private String mSuffix;
	private int mDefault, mMax, mValue = 0;
	
	private int mUIMax, mUIValue;
	

	private static final int MIN_VIBRATE_TIME = 40; // millis

	private static final String TAG = "SeekBarPreference";

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		mSuffix = context.getString(R.string.text_ms);
		mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
		mMax = attrs.getAttributeIntValue(androidns, "max", 100);
	}

	@Override
	protected View onCreateDialogView() {
		Log.d(TAG, "onCreateDialogView");
		
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(32);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		
		return layout;
	}
	
	private static int getUIValue(int persistValue) {
		return persistValue - MIN_VIBRATE_TIME;
	}
	
	private static int getPersistValue(int uiValue) {
		return uiValue + MIN_VIBRATE_TIME;
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		Log.d(TAG, "onBindDialogView");
		
		if (shouldPersist()) {
			mValue = getPersistedInt(mDefault);
			mUIValue = getUIValue(mValue);
		}

		mUIMax = getUIValue(mMax);
		
		String t = String.valueOf(mValue);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		mSeekBar.setMax(mUIMax);
		mSeekBar.setProgress(mUIValue);
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		if (restore) {
			mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
		} else {
			mValue = (Integer) defaultValue;
		}
		mUIValue = getUIValue(mValue);
	}

	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		mUIValue = value;
		mValue = getPersistValue(mUIValue);
		String t = String.valueOf(mValue);
		
		Log.d(TAG, "onProgressChanged -> t = " + t);
		
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			mUIValue = mSeekBar.getProgress();
			mValue = getPersistValue(mUIValue);
			
			if (shouldPersist()) {
				persistInt(mValue);
			}
			callChangeListener(new Integer(mValue));
		}
	}

	public void onStartTrackingTouch(SeekBar seek) {
	}

	public void onStopTrackingTouch(SeekBar seek) {
	}
}

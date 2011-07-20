package cn.yo2.aquarium.callvibrator;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutPreference extends DialogPreference {
	
	private Context mContext;
	
	private String mAppVersionName = "";
	private String mAppVersionCode = "";

	public AboutPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		
		PackageManager packageManager = mContext.getPackageManager();
        try {
			PackageInfo info = packageManager.getPackageInfo(mContext.getPackageName(), 0);
			
			mAppVersionName = info.versionName;
			mAppVersionCode = String.valueOf(info.versionCode);
		} catch (NameNotFoundException e) {
			// the current package should be exist all times
		}
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);
		
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(R.string.app_name);
		
		LayoutInflater inflater = LayoutInflater.from(mContext);
		
		LinearLayout root = (LinearLayout) inflater.inflate(R.layout.about_dialog, null);
		TextView versionName = (TextView) root.findViewById(R.id.versionName);
		versionName.setText(mContext.getString(R.string.prefs_summary_about, mAppVersionName));
		
		TextView versionCode = (TextView) root.findViewById(R.id.versionCode);
		versionCode.setText(mContext.getString(R.string.text_build, mAppVersionCode));
		
		builder.setView(root);
		
		builder.setPositiveButton(R.string.text_more, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(mContext.getString(R.string.url_more)));
				
				mContext.startActivity(intent);
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, null);
	}
}

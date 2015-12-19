package com.odwbo.voice;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

// 语音理解＋合成
public class BaseActivity extends FragmentActivity {
	static String TAG = "odwbo";
	private Toast mToast;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
	}

	public void log(final String text) {
		if (Constants.SHOW_TIP) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mToast.setText(text);
					mToast.show();
				}
			});
		} else {
			Log.i(TAG, text);
		}
	}
}

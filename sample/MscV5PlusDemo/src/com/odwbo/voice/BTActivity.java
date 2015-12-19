package com.odwbo.voice;

import kr.re.Dev.ArduinoEcho.BTManager;
import android.os.Bundle;
import android.util.Log;

// 蓝牙
public class BTActivity extends BaseActivity {
	private BTManager mBTManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBTManager = new BTManager() {
			@Override
			public void receiveStringData(String data) {
				super.receiveStringData(data);
				onReceiveTextMessage(data);
			}
		};
		if (mBTManager.create(getApplicationContext())) {
			mBTManager.initDeviceListDialog(this);
			mBTManager.showDeviceListDialog();
			mBTManager.resume(getApplicationContext());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBTManager != null) {
			mBTManager.destroy();
			mBTManager = null;
		}
	}

	public void onSendTextMesage(String text) {
		Log.i("Chat", "send<--------" + text);
		if (mBTManager != null) {
			mBTManager.sendStringData(text);
		}
	}

	public void onReceiveTextMessage(String text) {
		Log.i("Chat", "receive<--------" + text);
	}
}

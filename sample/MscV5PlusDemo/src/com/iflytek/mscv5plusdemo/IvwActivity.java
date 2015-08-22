package com.iflytek.mscv5plusdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class IvwActivity extends Activity implements OnClickListener{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ivw_activity);
		
		((Button) findViewById(R.id.btn_wake)).setOnClickListener(IvwActivity.this);
		((Button) findViewById(R.id.btn_oneshot)).setOnClickListener(IvwActivity.this);
		
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
		case R.id.btn_wake:
			intent = new Intent(IvwActivity.this, WakeDemo.class);
			startActivity(intent);
			break;
			
		case R.id.btn_oneshot:
			intent = new Intent(IvwActivity.this, OneShotDemo.class);
			startActivity(intent);
			break;
			
		default:
			break;
		}
	}
}

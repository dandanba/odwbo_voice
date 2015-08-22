package com.iflytek.mscv5plusdemo;


import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;

public class WakeDemo extends Activity implements OnClickListener {
	private String TAG = "ivw";
	private Toast mToast;
	private TextView textView;
	// 语音唤醒对象
	private VoiceWakeuper mIvw;
	// 唤醒结果内容
	private String resultString;
	// 设置门限值 ： 门限值越低越容易被唤醒
	private TextView tvThresh;
	private SeekBar seekbarThresh;
	private final static int MAX = 60;
	private final static int MIN = -20;
	private int curThresh = MIN;
	private String threshStr = "门限值：";
	// 引擎类型

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.wake_activity);
		
		initUi();
		
		// 加载识唤醒地资源，resPath为本地识别资源路径
		StringBuffer param = new StringBuffer();
		String resPath = ResourceUtil.generateResourcePath(WakeDemo.this,
				RESOURCE_TYPE.assets, "ivw/" + getString(R.string.app_id) + ".jet");
		param.append(ResourceUtil.IVW_RES_PATH + "=" + resPath);
		param.append("," + ResourceUtil.ENGINE_START + "=" + SpeechConstant.ENG_IVW);
		boolean ret = SpeechUtility.getUtility().setParameter(
				ResourceUtil.ENGINE_START, param.toString());
		if (!ret) {
			Log.d(TAG, "启动本地引擎失败！");
		}
		// 初始化唤醒对象
		mIvw = VoiceWakeuper.createWakeuper(this, null);
	}

	/**
	 * 
	 */
	private void initUi() {
		findViewById(R.id.btn_start).setOnClickListener(this);
		findViewById(R.id.btn_stop).setOnClickListener(this);
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		textView = (TextView) findViewById(R.id.txt_show_msg);
		tvThresh = (TextView)findViewById(R.id.txt_thresh);
		seekbarThresh = (SeekBar)findViewById(R.id.seekBar_thresh);
		seekbarThresh.setMax(MAX - MIN);
		seekbarThresh.setProgress(0);
		tvThresh.setText(threshStr + MIN);
		seekbarThresh.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				curThresh = seekbarThresh.getProgress() + MIN;
				tvThresh.setText(threshStr + curThresh);
			}
		});
		
	}

	@Override
	public void onClick(View v) {
		Log.d("value", "curThresh:" + curThresh);
		switch (v.getId()) {
		case R.id.btn_start:
			//非空判断，防止因空指针使程序崩溃
			mIvw = VoiceWakeuper.getWakeuper();
			if(mIvw != null) {
				resultString = "";
				textView.setText(resultString);
				// 清空参数
				mIvw.setParameter(SpeechConstant.PARAMS, null);
				// 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
				mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:"
						+ curThresh);
				// 设置唤醒模式
				mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
				// 设置持续进行唤醒
				mIvw.setParameter(SpeechConstant.KEEP_ALIVE, "1");
				mIvw.startListening(mWakeuperListener);
			} else {
				showTip("唤醒未初始化");
			}
			break;
	
		case R.id.btn_stop:
			mIvw = VoiceWakeuper.getWakeuper();
			if (mIvw != null) {
				mIvw.stopListening();
			} else {
				showTip("唤醒未初始化");
			}
			break;
			
		default:
			break;
		}		
	}


	private WakeuperListener mWakeuperListener = new WakeuperListener() {

		@Override
		public void onResult(WakeuperResult result) {
			try {
				String text = result.getResultString();
				JSONObject object;
				object = new JSONObject(text);
				StringBuffer buffer = new StringBuffer();
				buffer.append("【RAW】 "+text);
				buffer.append("\n");
				buffer.append("【操作类型】"+ object.optString("sst"));
				buffer.append("\n");
				buffer.append("【唤醒词id】"+ object.optString("id"));
				buffer.append("\n");
				buffer.append("【得分】" + object.optString("score"));
				buffer.append("\n");
				buffer.append("【前端点】" + object.optString("bos"));
				buffer.append("\n");
				buffer.append("【尾端点】" + object.optString("eos"));
				resultString =buffer.toString();
			} catch (JSONException e) {
				resultString = "结果解析出错";
				e.printStackTrace();
			}
			textView.setText(resultString);
		}

		@Override
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onBeginOfSpeech() {
			showTip("开始说话");
		}

		@Override
		public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {

		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy WakeDemo");
		mIvw = VoiceWakeuper.getWakeuper();
		if (mIvw != null) {
			mIvw.destroy();
		} else {
			showTip("唤醒未初始化");
		}
	}

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

}

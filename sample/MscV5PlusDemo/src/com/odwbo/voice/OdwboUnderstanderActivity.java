package com.odwbo.voice;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.UnderstanderResult;

// 语音理解＋合成
public class OdwboUnderstanderActivity extends Activity implements InitListener {
	private static String TAG = OdwboUnderstanderActivity.class.getSimpleName();
	private final SpeechUnderstanderListener mUnderstanderListener = new SpeechUnderstanderListener() {
		@Override
		public void onResult(UnderstanderResult result) {
			final String text = null == result ? "" : result.getResultString();
			log("onResult:" + text);
			handleUnderstanderResult(text);
		}

		@Override
		public void onVolumeChanged(int v) { // 音量值0~30
			log("onVolumeChanged:" + v);
		}

		@Override
		public void onEndOfSpeech() { // 结束录音
			log("onEndOfSpeech");
		}

		@Override
		public void onBeginOfSpeech() {// 开始录音
			log("onBeginOfSpeech");
		}

		@Override
		public void onError(SpeechError error) {// 会话发生错误回调接口
			log("onError:" + (error == null ? "null" : error.getErrorCode()));
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {// 扩展用接口
			log("onEvent:" + eventType);
		}
	};
	private final SynthesizerListener mTtsListener = new SynthesizerListener() {
		private int mPercentForBuffering = 0;// 缓冲进度
		private int mPercentForPlaying = 0;// 播放进度

		@Override
		public void onSpeakBegin() {
			log("onSpeakBegin");
		}

		@Override
		public void onSpeakPaused() {
			log("onSpeakPaused");
		}

		@Override
		public void onSpeakResumed() {
			log("onSpeakPaused");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
			mPercentForBuffering = percent;
			log("onBufferProgress:" + mPercentForBuffering + ":" + mPercentForPlaying);
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			mPercentForPlaying = percent;
			log("onSpeakProgress:" + mPercentForBuffering + ":" + mPercentForPlaying);
		}

		@Override
		public void onCompleted(SpeechError error) {
			log("onCompleted:" + (error == null ? "null" : error.getPlainDescription(true)));
			handleTtsCommpleted();
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle arg3) {
			log("onEvent:" + eventType);
		}
	};
	private final Handler mUnderstanderHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				removeMessages(1);
				startUnderstanding();
				break;
			default:
				break;
			}
		};
	};
	private SpeechUnderstander mSpeechUnderstander;// 语义理解对象（语音到语义）。
	private SpeechSynthesizer mTts;// 语音合成对象

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupUnderstander();
		setupTts();
		handleTtsCommpleted();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUnderstanderHandler.removeMessages(1);
		destroyTts();
		destroyUnderstander();
	}

	@Override
	public void onInit(int code) {
		Log.d(TAG, "speechUnderstanderListener init() code = " + code);
		if (code != ErrorCode.SUCCESS) {
			log("onInit：" + code);
		}
	}

	private void setupUnderstander() {
		mSpeechUnderstander = SpeechUnderstander.createUnderstander(this, this);
		mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");// 设置语言
		mSpeechUnderstander.setParameter(SpeechConstant.ACCENT, "mandarin");// 设置语言区域
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS, "4000"); // 设置语音前端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS, "1000"); // 设置语音后端点
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT, "1");// 设置标点符号
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/iflytek/wavaudio.pcm");// 设置音频保存路径
	}

	private void startUnderstanding() {
		if (mSpeechUnderstander.isUnderstanding()) {// 开始前检查状态
			mSpeechUnderstander.stopUnderstanding();
		}
		int code = mSpeechUnderstander.startUnderstanding(mUnderstanderListener);
		log("onStartUnderstanding:" + code);
	}

	private void destroyUnderstander() {
		mSpeechUnderstander.cancel();
		mSpeechUnderstander.destroy();// 退出时释放连接
	}

	private void setupTts() {
		mTts = SpeechSynthesizer.createSynthesizer(this, this);
		mTts.setParameter(SpeechConstant.PARAMS, null);// 清空参数
		// 设置合成
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);// 设置使用云端引擎
		mTts.setParameter(SpeechConstant.VOICE_NAME, Constants.sVoicerCloud);// 设置发音人
		mTts.setParameter(SpeechConstant.SPEED, "50"); // 设置语速
		mTts.setParameter(SpeechConstant.PITCH, "50"); // 设置音调
		mTts.setParameter(SpeechConstant.VOLUME, "80");// 设置音量
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");// 设置播放器音频流类型
	}

	private void destroyTts() {
		mTts.stopSpeaking();
		mTts.destroy();// 退出时释放连接
	}

	private void speek(String text) {
		int code = mTts.startSpeaking(text, mTtsListener);
		log("onStartSpeaking:" + code);
	}

	private void handleUnderstanderResult(String text) {
		if (!TextUtils.isEmpty(text)) {
			speek(text);
		} else {
			handleTtsCommpleted();
		}
	}

	private void handleTtsCommpleted() {
		mUnderstanderHandler.sendEmptyMessageDelayed(1, Constants.RECONGIZE_DELAY);
	}

	private void log(final String text) {
		if (Constants.SHOW_TIP) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(OdwboUnderstanderActivity.this, text, Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			Log.i(TAG, text);
		}
	}
}

package com.odwbo.voice;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import com.odwbo.voice.speech.setting.TtsSettings;
import com.odwbo.voice.speech.setting.UnderstanderSettings;

// 语音理解＋合成
public class OdwboUnderstanderActivity extends Activity implements InitListener {
	private static String TAG = OdwboUnderstanderActivity.class.getSimpleName();
	private final Handler mUnderstanderHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
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
	private final SpeechUnderstanderListener mUnderstanderListener = new SpeechUnderstanderListener() {
		@Override
		public void onResult(UnderstanderResult result) {
			final String text = null == result ? "" : result.getResultString();
			log("onResult:" + text);
			handleUnderstanderResult(text);
		}

		@Override
		public void onVolumeChanged(int v) {
			log("onVolumeChanged:" + v);
		}

		@Override
		public void onEndOfSpeech() {
			log("onEndOfSpeech");
		}

		@Override
		public void onBeginOfSpeech() {
			log("onBeginOfSpeech");
		}

		@Override
		public void onError(SpeechError error) {
			log("onError:" + (error == null ? "null" : error.getErrorCode()));
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
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
	private SpeechUnderstander mSpeechUnderstander;// 语义理解对象（语音到语义）。
	private SpeechSynthesizer mTts;// 语音合成对象
	private Toast mToast;// 听写结果内容

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupUnderstander();
		setupTts();
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
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
		final SharedPreferences mUnderstanderSettings = getSharedPreferences(UnderstanderSettings.PREFER_NAME, Activity.MODE_PRIVATE);
		String lag = mUnderstanderSettings.getString("understander_language_preference", "mandarin");
		if (lag.equals("en_us")) {
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "en_us");// 设置语言
		} else {
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.ACCENT, lag);// 设置语言区域
		}
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS, mUnderstanderSettings.getString("understander_vadbos_preference", "4000")); // 设置语音前端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS, mUnderstanderSettings.getString("understander_vadeos_preference", "1000")); // 设置语音后端点
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT, mUnderstanderSettings.getString("understander_punc_preference", "1"));// 设置标点符号
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/iflytek/wavaudio.pcm");// 设置音频保存路径
	}

	private void destroyUnderstander() {
		mSpeechUnderstander.cancel();
		mSpeechUnderstander.destroy();// 退出时释放连接
	}

	private void setupTts() {
		mTts = SpeechSynthesizer.createSynthesizer(this, this);
		final SharedPreferences mTtsSettings = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
		mTts.setParameter(SpeechConstant.PARAMS, null);// 清空参数
		// 设置合成
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);// 设置使用云端引擎
		mTts.setParameter(SpeechConstant.VOICE_NAME, Constants.sVoicerCloud);// 设置发音人
		mTts.setParameter(SpeechConstant.SPEED, mTtsSettings.getString("speed_preference", "50")); // 设置语速
		mTts.setParameter(SpeechConstant.PITCH, mTtsSettings.getString("pitch_preference", "50")); // 设置音调
		mTts.setParameter(SpeechConstant.VOLUME, mTtsSettings.getString("volume_preference", "50"));// 设置音量
		mTts.setParameter(SpeechConstant.STREAM_TYPE, mTtsSettings.getString("stream_preference", "3"));// 设置播放器音频流类型
	}

	private void destroyTts() {
		mTts.stopSpeaking();
		mTts.destroy();// 退出时释放连接
	}

	private void startUnderstanding() {
		if (mSpeechUnderstander.isUnderstanding()) {// 开始前检查状态
			mSpeechUnderstander.stopUnderstanding();
		}
		int code = mSpeechUnderstander.startUnderstanding(mUnderstanderListener);
		log("onStartUnderstanding:" + code);
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
					mToast.setText(text);
					mToast.show();
				}
			});
		} else {
			Log.i(TAG, text);
		}
	}
}

package com.odwbo.voice;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.ImageView;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.UnderstanderResult;
import com.odwbo.voice.api.Answer;
import com.odwbo.voice.api.Result;

// 语音理解＋合成
public class UnderstanderActivity extends BaseActivity implements InitListener {
	private final SpeechUnderstanderListener mUnderstanderListener = new SpeechUnderstanderListener() {
		@Override
		public void onResult(UnderstanderResult result) {
			final String text = null == result ? "" : result.getResultString();
			log("onResult:" + text);
			final Result r;
			final Answer answer;
			if (!TextUtils.isEmpty(text)// text 有效
					&& (r = GsonUtils.fromJson(text, Result.class)) != null// result 有效
					&& (answer = r.getAnswer()) != null) { // answer 有效
				speek(answer.text);
			} else {
				startUnderstanding(); // 理解错误
			}
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
			startUnderstanding(); // 发生错误回调
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
			startUnderstanding(); // 说完话
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
				doUnderstanding();
				break;
			default:
				break;
			}
		};
	};
	private SpeechUnderstander mSpeechUnderstander;// 语义理解对象（语音到语义）。
	private SpeechSynthesizer mTts;// 语音合成对象
	private ImageView mFaceImage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_understander);
		mFaceImage = (ImageView) findViewById(R.id.face_image);
		setupUnderstander();
		setupTts();
		startUnderstanding(); // 程序启动
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUnderstanderHandler.removeMessages(1);
		destroyTts();
		destroyUnderstander();
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();
	}

	@Override
	public void onInit(int code) {
		log("onInit:" + code);
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

	// 理解错误，开始收听
	// 启动程序，开始收听
	// 说完话，开始收听
	// 发生错误回调，开始收听
	private void startUnderstanding() {
		mUnderstanderHandler.sendEmptyMessageDelayed(1, Constants.RECONGIZE_DELAY);
	}

	private void doUnderstanding() {
		mUnderstanderHandler.removeMessages(1);
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
}

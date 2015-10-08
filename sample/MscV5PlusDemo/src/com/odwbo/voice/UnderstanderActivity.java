package com.odwbo.voice;

import java.net.URLEncoder;

import org.apache.http.Header;

import android.graphics.drawable.AnimationDrawable;
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
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;
import com.odwbo.voice.api.Answer;
import com.odwbo.voice.api.Query;
import com.odwbo.voice.api.Result;

// 语音理解＋合成
public class UnderstanderActivity extends BTActivity implements InitListener {
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
			if (v > 3) {
				runOnUiThread(new Runnable() {
					public void run() {
						mFaceImage.setImageResource(R.anim.listener_anim);
						AnimationDrawable animationDrawable = (AnimationDrawable) mFaceImage.getDrawable();
						if (!animationDrawable.isRunning()) {
							animationDrawable.start();
						}
					}
				});
			}
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
	private final UnderstanderHandler mUnderstanderHandler = new UnderstanderHandler();
	private final SendHandler mSendHandler = new SendHandler();
	private SpeechUnderstander mSpeechUnderstander;// 语义理解对象（语音到语义）。
	private SpeechSynthesizer mTts;// 语音合成对象
	private ImageView mFaceImage;
	private final ResponseHandlerInterface mResponseHandler = new AsyncHttpResponseHandler() {
		@Override
		public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
			final String json = StringUtils.byteArray2String(arg2);
			Query query = GsonUtils.fromJson(json, Query.class);
			String text = query.getDst();
			if (!TextUtils.isEmpty(text)) {
				mFaceImage.setImageResource(R.anim.think_anim);
				AnimationDrawable animationDrawable = (AnimationDrawable) mFaceImage.getDrawable();
				animationDrawable.start();
			} else {
				text = "sorry";
			}
			int code = mTts.startSpeaking(text, mTtsListener);
			log("onStartSpeaking:" + code);
		}

		@Override
		public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
			log("onError:" + arg0);
			startUnderstanding(); // 发生错误回调
		}
	};

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
		super.onBackPressed();
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
		mFaceImage.setImageResource(R.drawable.hello_world);
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
		if (text.length() > 0) {
			if (text.equals("转")) { // 特别控制
				mSendHandler.mSendCount = 0;
				mSendHandler.sendEmptyMessage(1);
				return;
			} else if (text.equals("走")) {
				mSendHandler.mSendCount = 0;
				mSendHandler.sendEmptyMessage(2);
				return;
			} else if (text.equals("退")) {
				mSendHandler.mSendCount = 0;
				mSendHandler.sendEmptyMessage(3);
				return;
			}
			if (text.length() > 30) {
				text = text.substring(0, 30);
			}
			if (Constants.sChinese) {
				if (!TextUtils.isEmpty(text)) {
					mFaceImage.setImageResource(R.anim.think_anim);
					AnimationDrawable animationDrawable = (AnimationDrawable) mFaceImage.getDrawable();
					animationDrawable.start();
				} else {
					text = "没听懂啊。";
				}
				int code = mTts.startSpeaking(text, mTtsListener);
				log("onStartSpeaking:" + code);
			} else {
				String url = getUrl(URLEncoder.encode(text));
				SpeechApp.getInstance().mHttpClient.get(url, mResponseHandler);
			}
		}
	}

	private String getUrl(String text) {
		return "http://openapi.baidu.com/public/2.0/bmt/translate?client_id=4qKrb8U8CSkiCRjud0Up8ueE&q=" + text + "&from=zh&to=en";
	}

	class UnderstanderHandler extends Handler {
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
	}

	class SendHandler extends Handler {
		public int mSendCount;

		public void handleMessage(Message msg) {
			final int what = msg.what;
			switch (msg.what) {
			case 1:
				removeMessages(what);
				if (mSendCount < 9) {
					onSendTextMesage("speed:" + 250);
					sendEmptyMessageDelayed(what, 500);
				} else {
					speek("累死我了，主人！");
				}
				mSendCount++;
				break;
			case 2:
				removeMessages(what);
				if (mSendCount < 8) {
					onSendTextMesage("touch:" + 900 + ":" + 150);
					sendEmptyMessageDelayed(what, 500);
				} else {
					speek("累死我了，主人！");
				}
				mSendCount++;
				break;
			case 3:
				removeMessages(what);
				if (mSendCount < 8) {
					onSendTextMesage("touch:" + 2700 + ":" + 150);
					sendEmptyMessageDelayed(what, 500);
				} else {
					speek("累死我了，主人！");
				}
				mSendCount++;
				break;
			default:
				break;
			}
		};
	}
}

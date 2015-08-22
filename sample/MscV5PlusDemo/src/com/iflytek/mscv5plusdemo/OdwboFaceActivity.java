package com.iflytek.mscv5plusdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.speech.setting.IatSettings;
import com.iflytek.speech.util.JsonParser;

// 语音转写+合成
public class OdwboFaceActivity extends Activity {
	private static String TAG = "OdwboFaceActivity";
	// 语音听写对象
	private SpeechRecognizer mIat;
	// 语音合成对象
	private SpeechSynthesizer mTts;
	// 默认云端发音人
	public static String voicerCloud = "xiaoyan";
	// 默认本地发音人
	public static String voicerLocal = "xiaoyan";
	// 听写结果内容
	private Toast mToast;
	private SharedPreferences mSharedPreferences;
	private final Handler mRecognizeHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				removeMessages(1, Constants.RECONGIZE_DELAY);
				recogize();
				break;
			default:
				break;
			}
		};
	};
	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码：" + code);
			}
		}
	};
	/**
	 * 听写监听器。
	 */
	private RecognizerListener recognizerListener = new RecognizerListener() {
		@Override
		public void onBeginOfSpeech() {
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			showTip("结束说话");
			mRecognizeHandler.sendEmptyMessageDelayed(1, Constants.RECONGIZE_DELAY);
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			play(text);
			if (isLast) {
				// TODO 最后的结果
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip("当前正在说话，音量大小：" + volume);
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
		}
	};
	private int mRet = 0;// 函数调用返回值
	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;
	/**
	 * 初始化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码：" + code);
			}
		}
	};
	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		@Override
		public void onSpeakBegin() {
			showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
			mPercentForBuffering = percent;
			mToast.setText(String.format(getString(R.string.tts_toast_format), mPercentForBuffering, mPercentForPlaying));
			mToast.show();
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			mPercentForPlaying = percent;
			showTip(String.format(getString(R.string.tts_toast_format), mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
			// TODO Auto-generated method stub
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME, Activity.MODE_PRIVATE);
		// 初始化识别对象
		mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
		// 设置参数
		setIatParam();
//		// 初始化合成对象
//		mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
//		setTtsParam();
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		mRecognizeHandler.sendEmptyMessageDelayed(1, Constants.RECONGIZE_DELAY);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mRecognizeHandler.removeMessages(1);
		// 退出时释放连接
		mIat.cancel();
		mIat.destroy();
	}

	/**
	 * 参数设置
	 */
	private void setIatParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);
		String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
		// 设置引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT, lag);
		}
		// 设置语音前端点
		mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
		// 设置语音后端点
		mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
		// 设置标点符号
		mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));
		// 设置音频保存路径
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/iflytek/wavaudio.pcm");
	}

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	private void setTtsParam() {
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 设置合成
		// 设置使用云端引擎
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME, voicerCloud);
		// 设置语速
		mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
		// 设置音调
		mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
		// 设置音量
		mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
		// 设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
	}

	private void recogize() {
		// 不显示听写对话框
		mRet = mIat.startListening(recognizerListener);
		if (mRet != ErrorCode.SUCCESS) {
			showTip("听写失败,错误码：" + mRet);
		} else {
			showTip(getString(R.string.text_begin));
		}
	}

	private void play(String text) {
		int code = mTts.startSpeaking(text, mTtsListener);
		if (code != ErrorCode.SUCCESS) {
			showTip("语音合成失败,错误码: " + code);
		}
	}

	private void showTip(final String text) {
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

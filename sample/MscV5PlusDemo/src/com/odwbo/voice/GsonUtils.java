package com.odwbo.voice;
import java.lang.reflect.Type;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class GsonUtils {

	private static final String TAG = GsonUtils.class.getSimpleName();
	private static final Gson sGson = new Gson();

	public static String toJson(Object src, Type typeOfSrc) {
		if (src == null) {
			return null;
		}
		return sGson.toJson(src, typeOfSrc);
	}

	public static <T> T fromJson(String json, Class<T> classOfT) {
		if (TextUtils.isEmpty(json)) {
			return null;
		}
		try {
			return sGson.fromJson(json, classOfT);
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "fromJson", e);
		}
		return null;
	}
}

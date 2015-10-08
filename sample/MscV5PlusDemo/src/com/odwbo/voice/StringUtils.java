package com.odwbo.voice;
import java.io.UnsupportedEncodingException;

public class StringUtils {

	public static boolean isEmpty(String str) {
		return (str == null || str.trim().length() == 0);
	}

	public static String byteArray2String(byte[] bs) {
		String text = null;
		if (bs != null && bs.length > 0) {
			try {
				text = new String(bs, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
		}
		return text;
	}
}
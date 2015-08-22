package com.odwbo.voice;

import java.util.List;

/**
 * List Utils
 */
public class ListUtils {
	/** default join separator **/
	public static final String DEFAULT_JOIN_SEPARATOR = ",";

	public static <T> int getSize(List<T> source) {
		return source == null ? 0 : source.size();
	}
}

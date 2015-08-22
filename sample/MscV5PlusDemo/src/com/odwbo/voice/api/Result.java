package com.odwbo.voice.api;

import java.util.List;
import com.odwbo.voice.ListUtils;

public class Result {
	public int rc;// 0,
	public String operation;// ": "ANSWER",
	public String service;// ": "openQA",
	public Answer answer;
	public String text;// ": "你好"
	public List<Answer> moreResults;

	public Answer getAnswer() {
		return answer != null ? answer : ListUtils.getSize(moreResults) > 0 ? moreResults.get(0) : null;
	}
}

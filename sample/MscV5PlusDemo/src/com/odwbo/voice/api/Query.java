package com.odwbo.voice.api;

import java.util.List;

public class Query {
	// {"from":"zh","to":"en","trans_result":[{"src":"\u4f60\u597d\uff0c\u5f88\u9ad8\u5174\u9047\u89c1\u4f60\uff01","dst":"Hello, I am glad to meet you!"}]}
	public List<TR> trans_result;

	public String getDst() {
		return trans_result != null & trans_result.size() > 0 ? trans_result.get(0).dst : "sorry";
	}

	public class TR {
		public String dst;
	}
}

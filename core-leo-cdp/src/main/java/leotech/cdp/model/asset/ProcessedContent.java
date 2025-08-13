package leotech.cdp.model.asset;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

public final class ProcessedContent {

	public final String title;
	public final String content;

	public ProcessedContent(String title, String content) {
		super();
		this.title = title;
		this.content = content;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}
	
	public boolean isEmpty() {
		return StringUtil.isEmpty(content) && StringUtil.isEmpty(title);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}

package leotech.crawler.model;

import com.google.gson.Gson;

public class ArticleData {
	public final String headlineImage;
	public final String title;
	public final String content;
	public final String url;
	public final String jsonLinkedData;

	public ArticleData(String title, String headlineImage, String content, String url, String jsonLinkedData) {
		super();
		this.title = title;
		this.headlineImage = headlineImage;
		this.content = content;
		this.url = url;
		this.jsonLinkedData = jsonLinkedData;
	}

	public ArticleData(String title, String headlineImage, String content, String url) {
		super();
		this.title = title;
		this.headlineImage = headlineImage;
		this.content = content;
		this.url = url;
		this.jsonLinkedData = "";
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}

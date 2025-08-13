package leotech.cdp.model.asset;

import com.google.gson.annotations.Expose;

/**
 * @author tantrieuf31
 *
 */
public class MediaInfoUnit {

	@Expose
	String headline;

	@Expose
	String content;

	public MediaInfoUnit() {
	}

	public MediaInfoUnit(String headline, String content) {
		super();
		this.headline = headline;
		this.content = content;
	}

	public String getHeadline() {
		return headline;
	}

	public void setHeadline(String headline) {
		this.headline = headline;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}

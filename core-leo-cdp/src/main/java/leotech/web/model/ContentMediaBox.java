package leotech.web.model;

import java.util.List;

import leotech.cdp.model.asset.MeasurableItem;

public class ContentMediaBox {

	ContentNavigator navigator;
	List<MeasurableItem> posts;

	public ContentMediaBox(ContentNavigator navigator, List<MeasurableItem> posts) {
		super();
		this.navigator = navigator;
		this.posts = posts;
	}

	public ContentNavigator getNavigator() {
		return navigator;
	}

	public void setNavigator(ContentNavigator navigator) {
		this.navigator = navigator;
	}

	public List<MeasurableItem> getPosts() {
		return posts;
	}

	public void setPosts(List<MeasurableItem> posts) {
		this.posts = posts;
	}

}

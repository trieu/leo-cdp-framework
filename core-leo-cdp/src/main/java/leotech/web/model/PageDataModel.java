package leotech.web.model;

import java.util.Arrays;
import java.util.List;

import leotech.cdp.model.asset.AssetGroup;

public class PageDataModel extends WebData {

	private final List<AssetGroup> pages;

	public PageDataModel(String host, String templateFolder, String templateName, String pageTitle,
			List<AssetGroup> pages) {
		super(host, templateFolder, templateName, pageTitle);
		this.pages = pages;
	}

	public PageDataModel(String host, String templateFolder, String templateName, String pageTitle, AssetGroup page) {
		super(host, templateFolder, templateName, pageTitle);
		this.pages = Arrays.asList(page);
	}

	public List<AssetGroup> getPages() {
		return pages;
	}
}

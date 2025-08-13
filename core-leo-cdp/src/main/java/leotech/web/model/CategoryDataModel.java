package leotech.web.model;

import java.util.List;

import leotech.cdp.model.asset.AssetCategory;

public class CategoryDataModel extends WebData {

	final List<AssetCategory> categories;

	public CategoryDataModel(String host, String templateFolder, String templateName, String pageTitle,
			List<AssetCategory> categories) {
		super(host, templateFolder, templateName, pageTitle);
		this.categories = categories;
	}

	public List<AssetCategory> getCategories() {
		return categories;
	}

	// TODO
}

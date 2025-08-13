package leotech.cdp.query.filters;

import java.util.SortedSet;

import com.google.gson.annotations.Expose;

import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.MeasurableItem;

public class ContentQueryFilter extends QueryFilter {

	@Expose
	SortedSet<AssetGroup> pages;

	@Expose
	SortedSet<MeasurableItem> posts;

	public ContentQueryFilter(String name, long networkId) {
		super(name, networkId);
	}

}

package leotech.query.util;

import leotech.cdp.model.asset.AssetItem;
import leotech.system.util.RandomCollection;

/**
 * 
 * LineItemList for selection by score
 * 
 * @author trieu
 *
 */
public class ContentCollection extends RandomCollection<AssetItem> {

	public ContentCollection() {
		super();
	}

	public void addLineItem(int score, AssetItem item) {
		super.add(score, item);
	}

}

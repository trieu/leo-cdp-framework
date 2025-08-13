package test.cdp.asset;

import leotech.cdp.domain.AssetItemManagement;
import leotech.cdp.model.asset.AssetContent;
import rfx.core.util.Utils;

public class TestAssetItemManagement {

	public static void main(String[] args) {
		String contentId = "2ZkWP3ffCJ7WjpCTNDzQtf";
		AssetContent item = AssetItemManagement.getContentItemById(contentId);
		System.out.println(item);
		Utils.sleep(3800);
		item = AssetItemManagement.getContentItemById(contentId);
		System.out.println(item);
		Utils.sleep(6000);
		item = AssetItemManagement.getContentItemById(contentId);
		System.out.println(item);
		
		Utils.exitSystemAfterTimeout(1000);
	}
}

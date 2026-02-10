package test.cdp.recommendation;

import java.util.List;

import leotech.cdp.dao.graph.GraphProfile2Product;
import leotech.cdp.model.graph.Profile2Product;
import leotech.cdp.model.marketing.TargetMediaUnit;
import rfx.core.util.Utils;

public class TestGraphProfile2Product {

	public static void main(String[] args) {
		String profileId = "6IHwVnxX11yUzJLQAaHCxl";
		getRecommendedProductsForUser(profileId);
		System.out.println("-------------------------------------");
		getRecommendedProductsForAdmin(profileId);
		Utils.exitSystemAfterTimeout(3000);
	}

	static void getRecommendedProductsForUser(String profileId) {
		List<TargetMediaUnit> rs = GraphProfile2Product.getRecommendedProductItemsForProfile(profileId, 0, 10);
		System.out.println("rs.size = " + rs.size());
		for (TargetMediaUnit t : rs) {
			System.out.println(t.getLandingPageUrl());
		}
	}
	
	static void getRecommendedProductsForAdmin(String profileId) {
		List<Profile2Product> rs = GraphProfile2Product.getRecommendedProductItemsForAdmin(profileId, 0, 10);
		System.out.println("rs.size = " + rs.size());
		for (Profile2Product t : rs) {
			System.out.println(t.getProduct().getFullUrl());
		}
	}
}

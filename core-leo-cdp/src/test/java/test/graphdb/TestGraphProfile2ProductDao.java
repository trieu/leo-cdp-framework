package test.graphdb;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.graph.GraphProfile2Product;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventMetric;
import rfx.core.util.Utils;

public class TestGraphProfile2ProductDao {

public static void main(String[] args) {
		
		GraphProfile2Product.initGraph(AbstractCdpDatabaseUtil.getCdpDatabase());
		
		String fromProfileId = "7GEoeMRmNEKVHQUc1jSco2";
		String toProductId1 = "EXwEFSjmpiFiMxtjpfMSu";
		String toProductId2 = "19qgjrPXWy7Pv7WEHHvZ8L";
		
		// documents 
		Profile profile = ProfileDaoUtil.getProfileById(fromProfileId);
		ProductItem product = AssetProductItemDaoUtil.getById(toProductId2);
		
		// event metrics
		EventMetric eventMetric1 = EventMetricManagement.getEventMetricByName("product-view");
		EventMetric eventMetric2 = EventMetricManagement.getEventMetricByName("play-video");
		EventMetric eventMetric3 = EventMetricManagement.getEventMetricByName("email-click");
		EventMetric eventMetric4 = EventMetricManagement.getEventMetricByName("add-to-cart");
		
		// connection
//		batchUpdateEdgeData(profile, product, eventMetric1, 7);
//		batchUpdateEdgeData(profile, product, eventMetric2, 5);
//		batchUpdateEdgeData(profile, product, eventMetric3, 3);
//		batchUpdateEdgeData(profile, product, eventMetric4, 2);
		
//		String groupId = "4hy2C1Lim149fuag4P68zT";
//		int c = initRecomendationData(groupId);
//		System.out.println("initRecomendationData count = " + c);
		
		//setRecommendedEdgeData(profile, product, 2);
		
//		List<Profile2Product> rs = query(profile, 0, 5, eventMetric4);
//		for (Profile2Product r : rs) {
//			System.out.println(r.getTotalScore() + " " + r.getTotalEvent() + " " + r.getProduct());
//		}
		
		Utils.exitSystemAfterTimeout(3000);
	}
}

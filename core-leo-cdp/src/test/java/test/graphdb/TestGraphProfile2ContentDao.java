package test.graphdb;

import java.util.Collections;
import java.util.List;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.graph.GraphProfile2Content;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.graph.Profile2Content;
import leotech.cdp.model.journey.EventMetric;
import rfx.core.util.Utils;

public class TestGraphProfile2ContentDao {

public static void main(String[] args) {
	
		GraphProfile2Content.initGraph(AbstractCdpDatabaseUtil.getCdpDatabase());
		
		String fromProfileId = "7GEoeMRmNEKVHQUc1jSco2";
		String toCreativeId1 = "7T4TH9DX7ZggpoSkTHyfpd";
		String toCreativeId2 = "317oh7OwJxdubwQ632HAIt";
		
		// documents 
		Profile profile = ProfileDaoUtil.getProfileById(fromProfileId);
		ProfileIdentity profileIdentity = profile.toProfileIdentity();
		AssetContent creative = AssetContentDaoUtil.getById(toCreativeId2);
		
		// event metrics
		EventMetric eventMetric1 = EventMetricManagement.getEventMetricByName("content-view");
		EventMetric eventMetric2 = EventMetricManagement.getEventMetricByName("play-video");
		EventMetric eventMetric3 = EventMetricManagement.getEventMetricByName("email-click");
		
		// connection
		GraphProfile2Content.batchUpdateEdgeData(profileIdentity, creative, eventMetric1, 5, 0);
		GraphProfile2Content.batchUpdateEdgeData(profileIdentity, creative, eventMetric2, 3, 0);
		GraphProfile2Content.batchUpdateEdgeData(profileIdentity, creative, eventMetric3, 1, 0);

		List<Profile2Content> rs = GraphProfile2Content.query(profile, 0, 5, eventMetric3);
		Collections.sort(rs);
		for (Profile2Content r : rs) {
			System.out.println(r.getTotalScore() + " " + r.getContent().getTitle());
		}
		
		Utils.exitSystemAfterTimeout(3000);
	}
}

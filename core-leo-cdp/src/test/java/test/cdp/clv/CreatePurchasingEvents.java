package test.cdp.clv;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;

import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.model.analytics.ImportingEventCallback;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.model.ImportingResult;
import rfx.core.util.RandomUtil;
import rfx.core.util.Utils;

public class CreatePurchasingEvents {

	public static void main(String[] args) {
		
		String testProfileEmail= "trieu@leocdp.com";
		String importFileUrl = "./data/sample-CSV-to-import-event-data.csv";
		
		testImportFromCsvAndSaveEvents(importFileUrl);
		
//		String groupId = "780V1RSWc0Nu5GBNZDRUuQ";
//		List<ProductItem> productItems = AssetProductItemDaoUtil.list("", groupId, 1, 100);
//		for (ProductItem productItem : productItems) {
//			testImport(testProfileEmail, importFileUrl, productItem);
//		}
		
		Utils.exitSystemAfterTimeout(5000);
	}

	
	
	private static void testImportFromCsvAndSaveEvents(String importFileUrl) {
		ImportingResult rs = EventDataManagement.importFromCsvAndSaveEvents(importFileUrl);
		System.out.println(rs);
	}



	public static void testImport(String testProfileEmail, String importFileUrl, ProductItem item) {
		ProfileSingleView profile = ProfileQueryManagement.getByPrimaryEmail(testProfileEmail);
		if(profile == null) {
			profile = ProfileSingleView.newCrmProfile(testProfileEmail, "", "", FunnelMetaData.STAGE_LEAD);
			profile.setFirstName("Tester " + new Date());
			profile.setDataLabels("test; demo");
			ProfileDataManagement.saveProfile(profile);
		}
		final String profileId = profile.getId();
		
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateNew(item.getTitle(), TouchpointType.WEBSITE, item.getFullUrl());
		TouchpointHub srcTouchpointHub = TouchpointHubManagement.getByHostName(srcTouchpoint.getHostname());
		
		if(srcTouchpointHub != null && srcTouchpoint != null) {
			ImportingEventCallback callback = new ImportingEventCallback() {
				@Override
				public TrackingEvent apply() {
					
					
					event.setRefProfileId(profileId);
					
					
					event.setSrcTouchpointHub(srcTouchpointHub);
					event.setSrcTouchpoint(srcTouchpoint);
					
					event.setRefTouchpointHubId(srcTouchpointHub.getId());
					event.setRefTouchpointHost(srcTouchpointHub.getHostname());
					event.setRefTouchpointUrl(srcTouchpoint.getUrl());
					event.setRefTouchpointId(srcTouchpoint.getId());
					
					DateTime createdAt = new DateTime(item.getCreatedAt()).plusMinutes(1);
					event.setCreatedAt(createdAt.toDate());
					event.setUpdatedAt(createdAt.toDate());
					
					event.getVideoUrls().clear();
					event.getImageUrls().clear();
					event.setImageUrl(item.getHeadlineImageUrl());
					
					if(event.isConversion()) {
						Set<OrderedItem> orderedItems = new HashSet<>();
						orderedItems.add(new OrderedItem(event.getCreatedAt(), item, 1));
						event.setOrderedItems(orderedItems);
						event.setTransactionId("order-product-" + item.getProductId());
						event.setTransactionValue(item.getSalePrice());
						
					}
					if(event.isExperience()) {
						event.setMetricValue(RandomUtil.getRandomInteger(5, 1));
						event.setTransactionId("order-product-" + item.getProductId());
					}
					event.buildHashedId();
					System.out.println(event);
					return event;
				}
			};
			ImportingResult rs = EventDataManagement.importFromCsvAndSaveEvents(importFileUrl, callback);
			System.out.println(rs.importedOk);
		}
	}
}

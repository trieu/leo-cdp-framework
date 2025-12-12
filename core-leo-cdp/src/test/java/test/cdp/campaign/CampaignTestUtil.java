package test.cdp.campaign;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.customer.ProfileType;

public class CampaignTestUtil {

	public static final String PRODUCT_RECOMMMENDATION_VN = "template-vn";
	public static final String PREFIX_TITLE_DEMO_TEST = "Demo Campaign ";

	public static final String activateRuleName = ActivationRuleType.PRODUCT_RECOMMENDATION_EMAIL;

	public static Map<String, ProductItem> getEcommerceProducts(String createdCampaignId) {
		List<ProductItem> items = AssetProductItemDaoUtil.list(0, 100);
		Map<String, ProductItem> map = new HashMap<String, ProductItem>(items.size());
		for (ProductItem productItem : items) {
			productItem.addInCampaigns(createdCampaignId);
			AssetProductItemDaoUtil.save(productItem);

			String productItemId = productItem.getProductId();
			map.put(productItemId, productItem);
		}
		return map;
	}

	public static ProductItem getRandomProduct(List<ProductItem> items) {
		assertNotNull(items);
		Random random = new Random();
		return items.get(random.nextInt(items.size()));
	}

	public static ProfileSingleView buildTestProfile(long productView, ProductItem productItem) {
		// define facts
		Map<String, Long> eventStatistics = new HashMap<String, Long>();
		eventStatistics.put(BehavioralEvent.General.SUBMIT_CONTACT, 1L);
		eventStatistics.put(BehavioralEvent.General.ITEM_VIEW, productView);

		ProfileSingleView pf = new ProfileSingleView();
		pf.setType(ProfileType.LOGIN_USER_CONTACT);
		pf.setFirstName("tester");
		pf.setEventStatistics(eventStatistics);
		pf.setPrimaryEmail("tantrieuf31@gmail.com");
		pf.buildHashedId();

		if (productView > 0) {
			TrackingEvent lastProductViewEvent = new TrackingEvent();
			Map<String, Object> eventData = new HashMap<>();
			eventData.put("productId", productItem.getId());
			System.out.println(" ### buildProfileFacts eventData " + eventData);

			lastProductViewEvent.setEventData(eventData);
			pf.setLastItemViewEvent(lastProductViewEvent);
		}
		return pf;
	}

	public static ProfileSingleView buildTestProfile() {
		// define facts
		Map<String, Long> eventStatistics = new HashMap<String, Long>();
		eventStatistics.put(BehavioralEvent.General.DATA_IMPORT, 1L);

		ProfileSingleView pf = new ProfileSingleView();
		pf.setType(ProfileType.LOGIN_USER_CONTACT);
		pf.setFirstName("tester");
		pf.setEventStatistics(eventStatistics);
		pf.setPrimaryEmail("tantrieuf31@gmail.com");
		pf.buildHashedId();

		return pf;
	}
}

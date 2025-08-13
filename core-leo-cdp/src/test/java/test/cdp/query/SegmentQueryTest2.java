package test.cdp.query;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.itfsw.query.builder.ArangoDbBuilderFactory;
import com.itfsw.query.builder.exception.ParserNotFoundException;
import com.itfsw.query.builder.support.model.result.ArangoDbQueryResult;

import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.query.SegmentQuery;
import rfx.core.util.Utils;

public class SegmentQueryTest2 {

	static String jsonQueryRules = "{\"condition\":\"AND\",\"rules\":[{\"id\":\"cdp_profile__inJourneyMaps\",\"field\":\"cdp_profile__inJourneyMaps\",\"type\":\"string\",\"operator\":\"in_journey_map\",\"value\":[\"id_default_journey\",\"0\"]},{\"id\":\"type\",\"field\":\"type\",\"type\":\"integer\",\"input\":\"select\",\"operator\":\"not_equal\",\"value\":8},{\"condition\":\"OR\",\"rules\":[{\"id\":\"behavioralEvents\",\"field\":\"behavioralEvents\",\"type\":\"string\",\"input\":\"select\",\"operator\":\"contains_any\",\"value\":\"purchase\"},{\"id\":\"behavioralEvents\",\"field\":\"behavioralEvents\",\"type\":\"string\",\"input\":\"select\",\"operator\":\"contains_any\",\"value\":\"order-checkout\"}]},{\"id\":\"cdp_trackingevent__createdAt#1\",\"field\":\"cdp_trackingevent__createdAt#1\",\"type\":\"string\",\"operator\":\"before\",\"value\":[\"1\",\"years\"]}],\"valid\":true}";

	public static void main(String[] args) throws ParserNotFoundException, IOException {
		ArangoDbQueryResult result = new ArangoDbBuilderFactory().builder().build(jsonQueryRules);
		String parsedFilterAql = result.getQuery();
		System.out.println(parsedFilterAql);

		// build query
		List<String> selectedFields = Arrays.asList("_key", "primaryEmail", "createdAt", "firstName", "age");
		String beginFilterDate = "2023-01-27T00:00:00+07:00";
		String endFilterDate = "2024-09-29T00:00:00+07:00";
		int startIndex = 0;
		int numberResult = 5;
		SegmentQuery profileQuery = new SegmentQuery(beginFilterDate, endFilterDate, jsonQueryRules, "", selectedFields);

		// pagination
		profileQuery.setStartIndex(startIndex);
		profileQuery.setNumberResult(numberResult);

		long count = SegmentDaoUtil.getSegmentSizeByQuery(profileQuery);
		System.out.println(" [getSegmentSizeByQuery] " + count);
		
//		long count2 = SegmentQueryManagement.computeSegmentSize(jsonQueryRules);
//		System.out.println("getSegmentSizeByQuery " + count)		System.out.println("computeSegmentSize " + count2);
		
		List<Profile> profiles = SegmentDataManagement.previewTopProfilesSegment("test", jsonQueryRules, selectedFields);
		for (Profile profile : profiles) {
			System.out.println(profile.getId());
		}
		
		Utils.exitSystemAfterTimeout(3000);
	}
}

package test.cdp.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.itfsw.query.builder.ArangoDbBuilderFactory;
import com.itfsw.query.builder.exception.ParserNotFoundException;
import com.itfsw.query.builder.support.model.result.ArangoDbQueryResult;

import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.domain.SegmentQueryManagement;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.query.SegmentQuery;
import rfx.core.util.Utils;

public class QueryParserTest {

	public static void main(String[] args) throws ParserNotFoundException, Exception {

		String jsonQueryRules = "{\n" + 
				"  \"condition\": \"AND\",\n" + 
				"  \"rules\": [\n" + 
				"    {\n" + 
				"      \"id\": \"age\",\n" + 
				"      \"field\": \"age\",\n" + 
				"      \"type\": \"integer\",\n" + 
				"      \"input\": \"number\",\n" + 
				"      \"operator\": \"between\",\n" + 
				"      \"value\": [\n" + 
				"        18,\n" + 
				"        40\n" + 
				"      ]\n" + 
				"    }\n" + 
				"  ],\n" + 
				"  \"valid\": true\n" + 
				"}";
		jsonQueryRules = "{\"condition\":\"AND\",\"rules\":[{\"id\":\"dataContext\",\"field\":\"dataContext\",\"type\":\"integer\",\"input\":\"radio\",\"operator\":\"equal\",\"value\":1}],\"valid\":true}";
		String customQuery = "FILTER d.age > 10 AND d.age < 30";
		
		ArangoDbQueryResult result = new ArangoDbBuilderFactory().builder().build(jsonQueryRules);
		String parsedFilterAql = result.getQuery();
		System.out.println(parsedFilterAql);
		

		String beginFilterDate = "2023-01-27T00:00:00+07:00";
		String endFilterDate =  "2024-04-29T00:00:00+07:00";
		int startIndex = 0;
		int numberResult = 5;
		
		List<String> selectedFields = Arrays.asList("id","primaryEmail","createdAt","firstName","age");
		
		// build query
		SegmentQuery profileQuery = new SegmentQuery(beginFilterDate, endFilterDate, jsonQueryRules, customQuery, selectedFields);
		
		// pagination
		profileQuery.setStartIndex(startIndex);
		profileQuery.setNumberResult(numberResult);
		
		System.out.println(profileQuery.getQueryWithFiltersAndPagination());
		System.out.println(profileQuery.getCountingQueryWithDateTimeFilter());
		System.out.println(profileQuery.updateStartIndexAndGetDataQuery(20));
		
		long count  = SegmentDaoUtil.getSegmentSizeByQuery(profileQuery);
		
		long count2 = SegmentQueryManagement.computeSegmentSize(jsonQueryRules, customQuery, true);
		
		System.out.println("getSegmentSizeByQuery "+count);
		System.out.println("computeSegmentSize "+count2);
		assertEquals(count, count2);
		
		List<Profile> profiles = SegmentDataManagement.previewTopProfilesSegment("test", jsonQueryRules, selectedFields);
		for (Profile profile : profiles) {
			System.out.println("getPrimaryEmail " + profile.getPrimaryEmail() + " - age: " + profile.getAge());
		}
		
		Utils.exitSystemAfterTimeout(3000);
	}
}

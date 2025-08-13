package test.cdp.segment;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.customer.Segment;
import rfx.core.util.Utils;



public class CrudSegmentTest {
	
	static String jsonQueryRules = "{\n" + 
			"  \"condition\": \"AND\",\n" + 
			"  \"rules\": [\n" + 
			"    {\n" + 
			"      \"id\": \"age\",\n" + 
			"      \"field\": \"age\",\n" + 
			"      \"type\": \"integer\",\n" + 
			"      \"input\": \"number\",\n" + 
			"      \"operator\": \"between\",\n" + 
			"      \"value\": [\n" + 
			"        20,\n" + 
			"        40\n" + 
			"      ]\n" + 
			"    }\n" + 
			"  ],\n" + 
			"  \"valid\": true\n" + 
			"}";
	
	
    @Test
    public void testCreateNew() throws IOException {
    	Segment sm = new Segment("profiles with age between 20 to 40", jsonQueryRules);
    	System.out.println(sm.getId());
    	Segment s = SegmentDataManagement.createSegment(sm);
    	SegmentDataManagement.refreshSegmentData(s.getId());
    	assertEquals(s.getId(), sm.getId());
    	
    	Utils.sleep(6000);
    }
}

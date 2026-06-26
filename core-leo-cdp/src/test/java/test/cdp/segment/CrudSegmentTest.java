package test.cdp.segment;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.customer.Segment;
import rfx.core.util.Utils;



public class CrudSegmentTest {
	
	static String jsonQueryRules = """
			{
			  "condition": "AND",
			  "rules": [
			    {
			      "id": "age",
			      "field": "age",
			      "type": "integer",
			      "input": "number",
			      "operator": "between",
			      "value": [
			        20,
			        40
			      ]
			    }
			  ],
			  "valid": true
			}\
			""";
	
	
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

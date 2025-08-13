package test.cdp.profile;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.TrackingEventCsvData;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.system.model.ImportingResult;
import rfx.core.util.Utils;

public class TestImportEvent {

	public static void main(String[] args) {
		
		String importFileUrl = "/data/sample-CSV-to-import-event-data.csv";
		
		//testPreview(importFileUrl);
		testImport(importFileUrl);
		
		Utils.exitSystemAfterTimeout(60000);
	}

	public static void testPreview(String importFileUrl) {
		Gson create = new GsonBuilder().setPrettyPrinting().create();
		List<TrackingEvent> events = EventDataManagement.parseImportedEventData(importFileUrl);
		for (TrackingEvent event : events) {
			if(event != null) {
				System.out.println(create.toJson(event));
			}
			
		}
		List<TrackingEventCsvData> events2 = EventDataManagement.parseToPreviewTrackingEvents(importFileUrl);
		for (TrackingEventCsvData event2 : events2) {
			if(event2 != null) {
				System.out.println(create.toJson(event2));
			}
		}
	}
	
	public static void testImport(String importFileUrl) {
		ProfileSingleView p = ProfileQueryManagement.getByPrimaryEmail("trieu@leocdp.com");
		if(p == null) {
			p = ProfileSingleView.newCrmProfile("trieu@leocdp.com", "", "", FunnelMetaData.STAGE_LEAD);
			p.setFirstName("Trieu");
			p.setLastName("Nguyen");
			p.setDataLabels("test; demo");
			ProfileDataManagement.saveProfile(p);
		}
		
		ImportingResult rs = EventDataManagement.importFromCsvAndSaveEvents(importFileUrl);
		System.out.println(rs.importedOk);
		Utils.exitSystemAfterTimeout(2000);
	}
}

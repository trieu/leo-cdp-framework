package test.datapipeline;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import leotech.cdp.data.DataExtractor;
import leotech.cdp.data.DataTransformer;
import leotech.cdp.data.DataWriter;
import leotech.cdp.data.DataWriter.CallbackProcessor;
import leotech.cdp.data.service.profile.ProfileDataExtractor;
import leotech.cdp.data.service.profile.ProfileDataTransformer;
import leotech.cdp.data.service.profile.ProfileDataWriter;
import leotech.cdp.model.customer.ProfileSingleView;
import rfx.core.util.FileUtils;
import rfx.core.util.Utils;

public class TestProfileDataPipeline {

	public static void main(String[] args) throws IOException {
		String fileName = "./data/mafc/FDS.FDS.CUSTOMER.json";
		fileName = "./data/mafc/LPMS.LPMSAPP.CUSTOMER.json";
		//fileName = "./data/mafc/LPMS.LPMSAPP.APPLICATION.json";
		
		String topic = "LPMS.LPMSAPP.APPLICATION";

		String json = FileUtils.readFileAsString(fileName);
		JsonArray dataView = new Gson().fromJson(json, JsonArray.class);
		
		// 1 extract
		DataExtractor extractor = new ProfileDataExtractor();
		
		// 2 transform
		DataTransformer<ProfileSingleView> transformer = new ProfileDataTransformer(topic, extractor);
		List<ProfileSingleView> dataList = transformer.process(extractor, dataView);
		
		// 3 persistence
		DataWriter<ProfileSingleView> persistence = new ProfileDataWriter();
		
		persistence.setAutoWriteData(false);
		
		persistence.process(dataList, new CallbackProcessor<ProfileSingleView>() {
			@Override
			public void process(ProfileSingleView p) {
				System.out.println("ProfileSingleView.getIdentities " + p.getIdentities());

				System.out.println("getApplicationIDs " + p.getApplicationIDs());
				System.out.println("getGovernmentIssuedIDs " + p.getGovernmentIssuedIDs());
				System.out.println("getGender " + p.getGender());

				System.out.println("getPurchasedBrands " + p.getPurchasedBrands());
				System.out.println("getMaritalStatus " + p.getMaritalStatus());
				System.out.println(p.getLastName() + " " + p.getMiddleName() + " " + p.getFirstName());
				System.out.println("--------------- \n");
			}
		});
		

		Utils.exitSystemAfterTimeout(2000);
	}
}
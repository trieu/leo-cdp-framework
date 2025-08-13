package leotech.cdp.data.service.profile;

import java.util.List;

import com.google.gson.JsonArray;

import leotech.cdp.data.DataExtractor;
import leotech.cdp.data.DataPipeline;
import leotech.cdp.data.DataTransformer;
import leotech.cdp.data.DataWriter;
import leotech.cdp.data.DataWriter.CallbackProcessor;
import leotech.cdp.model.customer.ProfileSingleView;

public class ProfileDataPipeline implements DataPipeline {

	@Override
	public void processBatchData(String topic, JsonArray dataView) {

		// 1 extract
		DataExtractor extractor = new ProfileDataExtractor();

		// 2 transform
		DataTransformer<ProfileSingleView> transformer = new ProfileDataTransformer(topic, extractor);
		List<ProfileSingleView> dataList = transformer.process(extractor, dataView);

		// 3 write to database
		DataWriter<ProfileSingleView> writer = new ProfileDataWriter();

		// write and commit data into database
		writer.setAutoWriteData(true);

		writer.process(dataList, new CallbackProcessor<ProfileSingleView>() {
			@Override
			public void process(ProfileSingleView p) {
				System.out.println("Fullname: " + p.getLastName() + " " + p.getMiddleName() + " " + p.getFirstName());
				System.out.println("ProfileSingleView.getIdentities " + p.getIdentities());

				System.out.println("getApplicationIDs " + p.getApplicationIDs());
				System.out.println("getGovernmentIssuedIDs " + p.getGovernmentIssuedIDs());
				System.out.println("getGender " + p.getGender());

				System.out.println("getPurchasedBrands " + p.getPurchasedBrands());
				System.out.println("getMaritalStatus " + p.getMaritalStatus());
				
				System.out.println("--------------- \n");
			}
		});
	}

}

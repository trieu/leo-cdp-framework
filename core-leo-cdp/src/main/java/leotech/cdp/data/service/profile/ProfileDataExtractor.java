package leotech.cdp.data.service.profile;

import java.util.Map;

import com.google.gson.JsonArray;

import leotech.cdp.data.DataExtractor;
import leotech.cdp.data.DataFieldProcessor;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public class ProfileDataExtractor extends DataExtractor {

	private static final String PROFILE_SINGLE_VIEW = "ProfileSingleView";
	private static final String FILE_PROFILE_DATA_MAPPER_JSON = "./configs/data-mapper/profile-data-mapper.json";

	public ProfileDataExtractor() {

	}

	@Override
	public Map<String, DataFieldProcessor> loadDataProcessorMap() {
		return loadProcessor(FILE_PROFILE_DATA_MAPPER_JSON);
	}

	@Override
	public JsonArray process(JsonArray dataArray) {
		JsonArray finalArray = new JsonArray(dataArray.size());
		String singleViewKey = PROFILE_SINGLE_VIEW;

		return doExtracting(dataArray, finalArray, singleViewKey);
	}


}

package leotech.cdp.data.service.profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import leotech.cdp.data.DataExtractor;
import leotech.cdp.data.DataFieldProcessor;
import leotech.cdp.data.DataTransformer;
import leotech.cdp.model.customer.ProfileSingleView;

/**
 * @author tantrieuf31
 * @since 2022
 */
public class ProfileDataTransformer extends DataTransformer<ProfileSingleView> {

	public ProfileDataTransformer(String topic, DataExtractor extractor) {
		super(topic, extractor);
	}
	
	@Override
	protected ProfileSingleView createDataObject(JsonObject obj) {
		String email = processField(obj, "primaryEmail").toString();
		primaryKeyFields.add("primaryEmail");
		
		String phone = processField(obj, "primaryPhone").toString();
		primaryKeyFields.add("primaryPhone");
		
		String citizenId = processField(obj, "governmentIssuedIDs").toString();
		primaryKeyFields.add("governmentIssuedIDs");
		
		String applicationID = processField(obj, "applicationIDs").toString();
		primaryKeyFields.add("applicationIDs");
		
		return ProfileSingleView.newImportedCustomerProfile(email, phone, citizenId, applicationID,"");
	}

	@Override
	protected ProfileSingleView transformObject(JsonObject obj) {
		ProfileSingleView p = createDataObject(obj);

		Map<String, Object> updatingAttributes = new HashMap<String, Object>(fieldProcessors.size());
		fieldProcessors.keySet().forEach(profileFieldName -> {
			if(! primaryKeyFields.contains(profileFieldName) ) {
				Object value = processField(obj, profileFieldName);
				if(value != null) {
					String sv = String.valueOf(value);
					if(!sv.isBlank()) {
						updatingAttributes.put(profileFieldName, value);
					}
				}
			}
		});
		p.updateDataWithAttributeMap(updatingAttributes);
		p.buildHashedId();
		return p;
	}

	@Override
	protected Object processField(JsonObject obj, String profileFieldName) {
		boolean isProfileFieldName = !profileFieldName.startsWith(FIELD_NOTE);
		if(isProfileFieldName) {
			DataFieldProcessor fieldProcessor = fieldProcessors.get(profileFieldName);
			if (fieldProcessor != null) {
				List<String> jsonFieldNames = fieldProcessor.getFromFieldNames();
				Object finalValue = null;
				for (String jsonFieldName : jsonFieldNames) {
					
					// E.g: from LPMS.LPMSAPP.APPLICATION_ID to ID 
					if(!this.topicPrefix.isBlank()) {
						jsonFieldName = jsonFieldName.replace(this.topicPrefix, "");	
					}
					JsonElement e = obj.get(jsonFieldName);
					if (e != null) {
						Object v = fieldProcessor.process(jsonFieldName, e);
						if(v != null) {
							finalValue = v;
						}
					}
				}
				if(finalValue != null) {
					return finalValue;
				}
				else  {
					debug("No value is found for the profileFieldName: " + profileFieldName);
				}
			}
			else {
				debug("No FieldProcessor for profileFieldName " + profileFieldName);
			}
		}
		return "";
	}
}
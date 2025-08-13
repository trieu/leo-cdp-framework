package leotech.cdp.data;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import rfx.core.util.FileUtils;


/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public abstract class DataExtractor {
	
	Map<String, DataFieldProcessor> dataProcessor = new HashMap<>(200);
	
	protected Map<String, DataFieldProcessor> loadProcessor(String fileName) {
		if(dataProcessor.size() == 0) {
			try {
				String json = FileUtils.readFileAsString(fileName);
				Type type = new TypeToken<HashMap<String, DataFieldProcessor>>(){}.getType();
				dataProcessor.putAll(new Gson().fromJson(json,type));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dataProcessor;
	}
	
	protected final JsonArray doExtracting(JsonArray dataArray, JsonArray finalArray, String singleViewKey) {
		Map<String, DataFieldProcessor> processors = loadDataProcessorMap();
		DataFieldProcessor singleViewProcessor = processors.get(singleViewKey);
		dataArray.forEach(e -> {
			JsonObject singleViewObject = e.getAsJsonObject();
			System.out.println(singleViewObject);
			
			singleViewProcessor.setSingleViewObject(singleViewObject);
			List<String> fromFieldNames = singleViewProcessor.getFromFieldNames();
			for (String fromFieldName : fromFieldNames) {
				JsonElement subFieldData = singleViewObject.get(fromFieldName);
				singleViewObject = (JsonObject) singleViewProcessor.process(fromFieldName, subFieldData);
				finalArray.add(singleViewObject);
			}
		});
		return finalArray;
	}
	
	abstract public Map<String, DataFieldProcessor> loadDataProcessorMap();
	abstract public JsonArray process(JsonArray dataArray);
}
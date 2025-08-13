package leotech.cdp.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import leotech.system.exception.InvalidDataException;
import rfx.core.util.StringUtil;

/**
 * abstract Data Transformer class
 * @author tantrieuf31
 * @since 2022
 */
public abstract class DataTransformer<T> {
	public static final String FIELD_NOTE = "NOTE_";
	
	protected Set<String> primaryKeyFields = new HashSet<String>();
	protected Map<String, DataFieldProcessor> fieldProcessors = new HashMap<>(200);
	protected String topicPrefix = "";
	
	public DataTransformer(String topic, DataExtractor extractor) {
		if(StringUtil.isNotEmpty(topic)) {
			this.topicPrefix = topic + "_";	
		}
		// load dataProcessor from DataExtractor
		Map<String, DataFieldProcessor> fieldProcessors = extractor.loadDataProcessorMap();
		if(fieldProcessors != null) {
			this.fieldProcessors.putAll(fieldProcessors);	
		}
		else {
			throw new InvalidDataException("fieldProcessors is NULL when load class: "+extractor.getClass());
		}
	}
	
	final public List<T> process(DataExtractor dataExtractor, JsonArray dataArray) {
		// call Extract to process data
		JsonArray extractedDataArray = dataExtractor.process(dataArray);
		
		List<T> transformedList = new ArrayList<>(extractedDataArray.size());
		extractedDataArray.forEach(e-> {
			JsonObject objMap = e.getAsJsonObject();
			if(objMap.size() > 0) {
				T transformedObj = this.transformObject(objMap);
				transformedList.add(transformedObj);
			}
			else {
				debug("Empty object, skip transform");
			}
		});
		return transformedList;
	}
	
	protected void debug(String log) {
		System.err.println(log);
	}
	
	// abstract methods
	/**
	 * create new object in type T, from JsonObject
	 * 
	 * @param obj
	 * @return
	 */
	abstract protected T createDataObject(JsonObject obj);
	
	
	/**
	 * transform JsonObject into the object in type T
	 * 
	 * @param obj
	 * @return
	 */
	abstract protected T transformObject(JsonObject obj);
	
	
	/**
	 * apply DataFieldProcessor to get value for the outputFieldName, with data in JsonObject
	 * 
	 * @param obj
	 * @param profileFieldName
	 * @return
	 */
	abstract protected Object processField(JsonObject obj, String outputFieldName);
	
}

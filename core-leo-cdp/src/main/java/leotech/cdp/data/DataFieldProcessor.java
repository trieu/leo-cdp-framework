package leotech.cdp.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class DataFieldProcessor {

	private static final String DEFAULT = "default";
	List<String> fromFieldNames = null;
	Map<Object, Object> transformDataMap = null;
	JsonObject singleViewObject = null;

	/**
	 * @param fieldNames
	 */
	public DataFieldProcessor(String... fieldNames) {
		super();
		this.fromFieldNames = Arrays.asList(fieldNames);
	}

	/**
	 * @param fieldNames
	 */
	public DataFieldProcessor(List<String> fieldNames) {
		super();
		this.fromFieldNames = fieldNames;
	}

	public JsonObject getSingleViewObject() {
		return singleViewObject;
	}

	public void setSingleViewObject(JsonObject singleViewObject) {
		this.singleViewObject = singleViewObject;
	}

	public List<String> getFromFieldNames() {
		if(fromFieldNames == null) {
			fromFieldNames = new ArrayList<String>(0);
		}
		return fromFieldNames;
	}

	public Object process(String fromFieldName, JsonElement e) {
		if (e != null) {
			if (singleViewObject != null) {
				JsonObject subObject = new Gson().fromJson(e.getAsString(), JsonObject.class) ;
				//System.out.println(obj2);
				subObject.entrySet().forEach(entry->{
					singleViewObject.add(entry.getKey(), entry.getValue());
				});
				singleViewObject.remove(fromFieldName);
				return singleViewObject;
			} else {
				String obj = StringUtil.safeString(e.getAsString()).trim();
				if (this.transformDataMap == null) {
					return obj;
				} else {
					Object defaultObj = this.transformDataMap.getOrDefault(DEFAULT, "");
					this.transformDataMap.getOrDefault(obj, defaultObj);
				}
			}
		}
		if(singleViewObject != null) {
			return singleViewObject;
		}
		else {
			return "";
		}
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}

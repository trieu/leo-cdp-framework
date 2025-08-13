package leotech.system.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import rfx.core.util.FileUtils;

/**
 * data importer from JSON list, to init new database or for importing data from uploaded file
 * 
 * @author tantrieuf31
 *
 * @param <T>
 */
public final class JsonFileImporter<T> {

	String jsonFileUri;
	Class<T[]> clazz;

	public JsonFileImporter(String jsonFileUri, Class<T[]> clazz) {
		super();
		this.jsonFileUri = jsonFileUri;
		this.clazz = clazz;
	}

	public List<T> getDataAsList()  {
		try {
			String jsonListData = FileUtils.readFileAsString(jsonFileUri);
			return Arrays.asList(new Gson().fromJson(jsonListData, clazz)) ;
		} catch (JsonSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<>(0);
	}
}

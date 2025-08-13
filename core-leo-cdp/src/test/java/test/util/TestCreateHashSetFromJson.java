package test.util;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class TestCreateHashSetFromJson {

	public static void main(String[] args) {
		try {
			String json = "['a','b', 'c','a','a','b', 'c','a']";
			Type listType = new TypeToken<HashSet<String>>(){}.getType();
			Set<String> set = new Gson().fromJson(json, listType);
			if(set != null) {
				System.out.println(set);
			}
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
	}
}

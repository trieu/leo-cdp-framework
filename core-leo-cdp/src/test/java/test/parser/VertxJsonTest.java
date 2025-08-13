package test.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import rfx.core.util.StringUtil;

public class VertxJsonTest {

    public static void main(String[] args) {
	Map<String, List<String>> keywordFilters = new HashMap<>(30);
	keywordFilters.put("by Project", Arrays.asList("Retails", "Vinfast"));
	keywordFilters.put("by Location", Arrays.asList("Mien Name", "Mien Bac"));

	String json = new Gson().toJson(keywordFilters);

	System.out.println(json);

	JsonObject jsonKeywordFilters = new JsonObject(StringUtil.safeString(json, "{}"));

	jsonKeywordFilters.forEach(e -> {
	    String groupName = e.getKey();
	    JsonArray filterList = (JsonArray) e.getValue();
	    List<String> list = new ArrayList<>(filterList.size());
	    filterList.forEach(e1 -> {
		list.add(e1.toString());
	    });

	    System.out.println(groupName + " => " + list);

	});

    }
}

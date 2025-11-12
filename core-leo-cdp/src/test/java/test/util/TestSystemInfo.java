package test.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import leotech.system.domain.SystemSnapshot;

public class TestSystemInfo {

	public static void main(String[] args) {
		// Create a SystemSnapshot object
		SystemSnapshot SystemSnapshot = new SystemSnapshot("");

		// Convert to JSON
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(SystemSnapshot);

		// Print JSON to console
		System.out.println(json);

		
	}
}

package test.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import leotech.system.domain.SystemInfo;

public class TestSystemInfo {

	public static void main(String[] args) {
		// Create a SystemInfo object
		SystemInfo systemInfo = new SystemInfo();

		// Convert to JSON
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(systemInfo);

		// Print JSON to console
		System.out.println(json);

		
	}
}

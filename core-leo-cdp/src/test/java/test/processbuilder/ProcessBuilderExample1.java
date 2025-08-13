package test.processbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import rfx.core.util.Utils;

public class ProcessBuilderExample1 {

	public static void main(String[] args) {

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "/Users/mac/0-uspa/leo-cdp-free-edition/leo-observer-starter-v_0.8.9.jar","localLeoObserverWorker", "input_data_source");


			processBuilder.redirectErrorStream(true);
			File log = new File("./data/observer.log");
			processBuilder.redirectOutput(log);

			Process process = processBuilder.start();
			
			System.out.println("start process"+process.pid());
			
			Utils.foreverLoop();
			//printOutput(process);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected static void printOutput(Process process) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String result = builder.toString();
		
		System.out.println(result);
	}

}

package test.automation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public class TestSimple {
	public static void main(String args[]) throws MalformedURLException {
		// need to start chrome-driver first

		Map<String, Object> deviceMetrics = new HashMap<>();
		deviceMetrics.put("width", 800);
		deviceMetrics.put("height", 600);
		// deviceMetrics.put("pixelRatio", 3.0);

		Map<String, Object> mobileEmulation = new HashMap<>();
		mobileEmulation.put("deviceMetrics", deviceMetrics);
		mobileEmulation.put("userAgent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246");
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);

		WebDriver driver = new RemoteWebDriver(new URL("http://localhost:9515"), new ChromeOptions());
		driver.get("http://www.google.com");

	}
}

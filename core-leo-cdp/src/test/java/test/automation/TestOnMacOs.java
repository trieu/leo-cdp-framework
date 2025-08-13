package test.automation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import rfx.core.util.Utils;

public class TestOnMacOs {

	static final String path = "/Users/mac/devtools/chromedriver";

	static ChromeOptions chromeOptions;
	static {
		System.setProperty("webdriver.chrome.driver", path);

		Map<String, Object> deviceMetrics = new HashMap<>();
		deviceMetrics.put("width", 800);
		deviceMetrics.put("height", 600);

		Map<String, Object> mobileEmulation = new HashMap<>();
		mobileEmulation.put("deviceMetrics", deviceMetrics);
		mobileEmulation.put("userAgent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36");

		chromeOptions = new ChromeOptions();
		// chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
		// chromeOptions.addArguments("headless");

	}

	static AtomicLong countImp = new AtomicLong();
	static AtomicLong countClick = new AtomicLong();
	static final int NUM_THREAD = 1;

	static void test() {
		String url = "https://m.facebook.com/tantrieuf31";

		try {

			ChromeDriver driver = new ChromeDriver(chromeOptions);

			driver.get(url);

			// driver.get(url);
			Utils.sleep(1000);

			try {
				WebElement div = driver.findElement(By.id("body"));
				System.out.println(div.getText());
			} catch (NoSuchElementException e) {
				System.out.println("No ad, just Skip ..." + url);
			}

			driver.quit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		test();
	}

}

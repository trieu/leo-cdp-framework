package test.automation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import rfx.core.util.RandomUtil;
import rfx.core.util.Utils;

//https://sites.google.com/a/chromium.org/chromedriver/mobile-emulation
public class PrerollTVC {
	static final String path = "/home/platform/data/chromedriver";
	static String testUrl = "https://insight.adsrvr.org/enduser/vast/?iid=8a4f56cb-36fd-4799-822f-6e9be254863c&crid=fbll6e91&wp=1.740000&aid=1&wpc=USD&sfe=d742d44&puid=&tdid=&pid=ah401vl&ag=n5dv49i&sig=2b8p0qcT02CUjNdd24GXNUgC2oxB_MNvm7TpVdKrXWk.&cf=446387&fq=0&td_s=ngoisao.vn&rcats=7sp%2Cusw%2C5rf%2C3oc%2C2ic%2C3c6%2C2gy%2Chhr%2Cy29%2Ce7y%2Czm4&mcat=&mste=&mfld=2&mssi=&mfsi=ydy2dxh61e&uhow=90&agsa=&rgco=Vietnam&rgre=Ho%20Chi%20Minh%20City&rgme=&rgci=Ho%20Chi%20Minh%20City&rgz=&svbttd=1&dt=PC&osf=OSX&os=Other&br=Chrome&rlangs=en&mlang=&svpid=1504092369&did=gax-vvgxineykrz&rcxt=Other&lat=10.814200&lon=106.643800&tmpc=&daid=&vp=0&osi=&osv=&bv=1&bp=3&svscid=1504092835&dvps=Large&mk=Apple&mdl=Chrome%20-%20OS%20X&vpb=PreRoll&svsc=AllowEither&dc=6&vcc=CA8QHhgeMgIIAjoECAEIAkABSAFQBIgBAqABgAWoAegCyAEB0AED6AEC8AEB-AEBgAIDigICCAWaAgQIAQgCoAID&sv=ambient&pidi=1851&advi=72471&cmpi=746634&agi=3132952&cridi=6431458&svi=40&adv=ddrs7t2&cmp=ysslrx1&skip=1&vrtd=14%2C15&dur=CjsKImNoYXJnZS1hbGxJbnRlZ3JhbFZpZGVvQnJhbmRTYWZldHkiFQjm__________8BEghpbnRlZ3JhbA..&crrelr=&ipl=1530244553&atst=3";

	static AtomicLong countImp = new AtomicLong();
	static AtomicLong countClick = new AtomicLong();
	static final int NUM_THREAD = 6;

	public static void main(String[] args) {
		// Optional, if not specified, WebDriver will search your path for chromedriver.
		System.setProperty("webdriver.chrome.driver", path);

		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREAD);

		for (int i = 0; i < 1; i++) {
			Utils.sleep(100);
			executor.submit(() -> {
				for (int j = 0; j < 1; j++) {
					test();
					Utils.sleep(50);
					// break;
				}
			});
		}

	}

	static ChromeOptions chromeOptions;
	static {
		Map<String, Object> deviceMetrics = new HashMap<>();
		deviceMetrics.put("width", 800);
		deviceMetrics.put("height", 600);
		// deviceMetrics.put("pixelRatio", 3.0);

		Map<String, Object> mobileEmulation = new HashMap<>();
		// mobileEmulation.put("deviceMetrics", deviceMetrics);
		mobileEmulation.put("userAgent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246");

		// chromeOptions.addArguments("window-size=1200x600");
		chromeOptions = new ChromeOptions();
		chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
		// chromeOptions.addArguments("headless");
		chromeOptions.addArguments("window-size=1200x600");

	}

	static void test() {

		try {

			DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
			Date expiry = df.parse("Thu Sep 28 20:29:30 JST 2020");
			String path = "/";
			String domain = ".gammaplatform.com";

			ChromeDriver driver = new ChromeDriver(chromeOptions);

			String url = "http://video.monngon.tv/html/post/thuc-pham-tot-cho-nguoi-chay-bo-10000-9f63c8690d5aaa551dc392fb91067e8dddcef7f7";

			driver.get(url);
			driver.manage().addCookie(new Cookie("_aGeoIp", "VN-Hanoi", domain, path, expiry));
			driver.manage().addCookie(new Cookie("_mq_lcm", "1555411292", domain, path, expiry));
			driver.manage().addCookie(new Cookie("_mq_id", "qhpl50nkwdw", domain, path, expiry));
			driver.manage().addCookie(new Cookie("_aUID", "ugxl0veeus8z", domain, path, expiry));
			driver.manage().addCookie(new Cookie("_mq_geo", "VN", domain, path, expiry));
			// driver.manage().addCookie(new Cookie("", "", domain, path, expiry));
			Utils.sleep(8000);

			driver.get(url);
			Utils.sleep(10000);

			// driver.executeScript("window.scrollBy(0,2300)", "");

			// WebElement adURLBox = driver.findElement(By.id("ad_url"));
			// adURLBox.sendKeys(testUrl);
			// Utils.sleep(RandomUtil.randomNumber(3000, 5000));

			try {
				WebElement btn = driver.findElement(By.id("videoPlaceholder"));
				Dimension size = btn.getSize();
				long imp = countImp.incrementAndGet();
				System.out.println(imp + "==> Load OK Ad " + size.getWidth() + " " + size.getHeight());
				// if (RandomUtil.randomNumber(1, 100) < 5)
				{
					btn.click();
					long click = countClick.incrementAndGet();
					System.out.println(click + "==> Click Ad OK");
					Utils.sleep(RandomUtil.randomNumber(8000, 16000));
				}
			} catch (NoSuchElementException e) {
				System.out.println("No ad, just Skip ..." + url);
			}

			driver.quit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testPlayer() {
		try {
			Map<String, Object> deviceMetrics = new HashMap<>();
			deviceMetrics.put("width", 360);
			deviceMetrics.put("height", 640);
			deviceMetrics.put("pixelRatio", 3.0);

			Map<String, Object> mobileEmulation = new HashMap<>();
			mobileEmulation.put("deviceMetrics", deviceMetrics);
			mobileEmulation.put("userAgent",
					"Mozilla/5.0 (Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
			ChromeOptions chromeOptions = new ChromeOptions();
			chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);

			WebDriver driver = new ChromeDriver(chromeOptions);
			driver.get(
					"https://thethao.tuoitre.vn/1h-ngay-7-7-bi-manh-nhung-kho-thang-brazil-dong-deu-hon-20180706123905413.htm");

			// WebElement adURLBox = driver.findElement(By.id("ad_url"));
			// adURLBox.sendKeys(testUrl);

			// WebElement btn = driver.findElement(By.id("ad_url_play"));
			Utils.sleep(5000);
			WebElement btn = driver.findElement(By.className("hdo__box"));
			btn.click();

			Utils.sleep(5000);

			// Utils.sleep(20000);
			driver.quit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

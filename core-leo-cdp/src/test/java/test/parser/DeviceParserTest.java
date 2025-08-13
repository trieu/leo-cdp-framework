package test.parser;

import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceInfoUtil;

public class DeviceParserTest {

	public static void main(String[] args) {
		// TODO unit test all devices
		// https://deviceatlas.com/blog/list-of-user-agent-strings

		System.out.println("--- Device Info Parsing Results ---");
		System.out.println(); // Add an empty line for spacing

		printDeviceInfo(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
		printDeviceInfo(
				"Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Mobile Safari/537.36");
		printDeviceInfo(
				"Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1");
		printDeviceInfo(
				"Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Mobile Safari/537.36");
		printDeviceInfo(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246");
		printDeviceInfo(
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9");
		printDeviceInfo(
				"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
		printDeviceInfo("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:60.0) Gecko/20100101 Firefox/60.0");
		printDeviceInfo("Opera/9.80 (X11; Linux i686; Ubuntu/14.10) Presto/2.12.388 Version/12.16");
		printDeviceInfo("Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko");
		printDeviceInfo(
				"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) coc_coc_browser/42.0 CoRom/36.0.1985.144 Chrome/36.0.1985.144 Safari/537.36");
		printDeviceInfo(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 8_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12D508 [FBAN/FBIOS;FBAV/27.0.0.10.12;FBBV/8291884;FBDV/iPhone7,1;FBMD/iPhone;FBSN/iPhone OS;FBSV/8.2;FBSS/3; FBCR/vodafoneIE;FBID/phone;FBLC/en_US;FBOP/5]");
		printDeviceInfo(
				"Mozilla/5.0 (Linux; Android 7.1.1; G8231 Build/41.2.A.0.219; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/59.0.3071.125 Mobile Safari/537.36");
		printDeviceInfo(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 8_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12D508 [FBAN/FBIOS;FBAV/27.0.0.10.12;FBBV/8291884;FBDV/iPhone7,1;FBMD/iPhone;FBSN/iPhone OS;FBSV/8.2;FBSS/3; FBCR/vodafoneIE;FBID/phone;FBLC/en_US;FBOP/5]");
		printDeviceInfo(
				"Mozilla/5.0 (Linux; Android 4.1.1; X909 Build/JRO03L) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.92 Mobile Safari/537.36");
		printDeviceInfo(
				"Mozilla/5.0 (Linux; Android 7.0; SAMSUNG SM-G950F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.2 Chrome/51.0.2704.106 Mobile Safari/537.36");
		printDeviceInfo(
				"Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/W.X.Y.Z Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
		printDeviceInfo(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 13_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 [FBAN/FBIOS;FBDV/iPhone10,2;FBMD/iPhone;FBSN/iOS;FBSV/13.3.1;FBSS/3;FBID/phone;FBLC/en_US;FBOP/5;FBCR/]");
		printDeviceInfo("facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)");
		printDeviceInfo(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A356 Safari/604.1");
		printDeviceInfo("_zbot");
	}

	/**
	 * Helper method to parse a user agent string and print its DeviceInfo.
	 * @param userAgent The user agent string to parse.
	 */
	private static void printDeviceInfo(String userAgent) {
		System.out.println("User Agent: " + userAgent);
		DeviceInfo deviceInfo = DeviceInfoUtil.getDeviceInfo(userAgent);
		System.out.println("Parsed Info: " + deviceInfo);
		System.out.println("-----------------------------------"); // Separator for clarity
	}
}

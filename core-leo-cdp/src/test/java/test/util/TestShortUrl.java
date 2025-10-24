package test.util;

import leotech.system.util.URLShortener;

public class TestShortUrl {

	// test the code
		public static void main(String args[]) {
			URLShortener u = new URLShortener();
			
			String urls[] = { "www.google.com/", "www.google.com",
					"http://www.yahoo.com", "www.yahoo.com/", "www.amazon.com",
					"www.amazon.com/page1.php", "www.amazon.com/page2.php",
					"www.flipkart.in", "www.rediff.com", "www.techmeme.com",
					"www.techcrunch.com", "www.lifehacker.com", "www.icicibank.com" };

			for (int i = 0; i < urls.length; i++) {
				System.out.println("URL:" + urls[i] + "\tTiny: "
						+ u.shortenURL(urls[i]) + "\tExpanded: "
						+ u.expandURL(u.shortenURL(urls[i])));
			}
		}
}

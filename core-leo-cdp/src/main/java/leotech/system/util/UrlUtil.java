package leotech.system.util;

import java.net.URL;

import rfx.core.util.StringUtil;

/**
 * Util to extract data from URL
 * 
 * @author tantrieuf31
 * @since 2020
 */
public class UrlUtil {

	private static final String INSTAGRAM_COM = "instagram.com";
	private static final String GOOGLE_COM = "google.com";
	private static final String FACEBOOK_COM = "facebook.com";
	
	private static final String HTTP = "http";
	private static final String WWW = "www.";

	final public static String getHostName(String url) {
		if(StringUtil.isNotEmpty(url)) {
			if(url.startsWith(HTTP)) {
				try {
					URL uri = new URL(url);
					String hostname = uri.getHost();
					// to provide faultproof result, check if not null then return only
					// hostname, without www.
					if (hostname != null) {
						hostname = hostname.startsWith(WWW) ? hostname.substring(4) : hostname;
						if(hostname.contains("facebook.")) {
							return FACEBOOK_COM;
						}
						else if(hostname.contains("google.")) {
							return GOOGLE_COM;
						}
						else if(hostname.contains("instagram.")) {
							return INSTAGRAM_COM;
						}
					}
					return hostname;
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			return url;
		}
		return "";
	}
	
}

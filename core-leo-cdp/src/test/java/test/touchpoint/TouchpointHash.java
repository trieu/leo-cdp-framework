package test.touchpoint;

import com.github.slugify.Slugify;

import leotech.system.util.UrlUtil;

public class TouchpointHash {

	public static void main(String[] args) {
		String jsFileName = new Slugify().slugify(UrlUtil.getHostName("https://google.com/")).toLowerCase() + ".js";
		System.out.println(jsFileName);
	}
	

}

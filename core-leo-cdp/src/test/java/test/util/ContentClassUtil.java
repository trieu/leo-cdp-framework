package test.util;

import leotech.system.util.KeywordUtil;

public class ContentClassUtil {

	public static void main(String[] args) throws Exception {
		String s = "Chính sách bảo hành @# 123bac";
		System.out.println(KeywordUtil.convertUtf8ToAscii(s));
		System.out.println(KeywordUtil.normalizeForSEO(s));
		System.out.println(KeywordUtil.normalizeForSearchIndex(s));
	}
}

package leotech.system.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

import net.gcardone.junidecode.Junidecode;

public class KeywordUtil {
	public static String convertUtf8ToAscii(String in) {
		String nfdNormalizedString = Normalizer.normalize(in, Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		return pattern.matcher(nfdNormalizedString).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D")
				.replaceAll(" ", "_").toLowerCase();
	}

	public static String normalizeForSEO(String s) {
		return Junidecode.unidecode(s).toLowerCase().replaceAll("[^A-Za-z0-9]", "_");
	}

	public static String normalizeForSearchIndex(String s) {
		return Junidecode.unidecode(s).toLowerCase().replaceAll("[^A-Za-z0-9]", " ");
	}
}

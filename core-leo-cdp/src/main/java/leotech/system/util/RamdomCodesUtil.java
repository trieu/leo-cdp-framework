package leotech.system.util;

import java.util.Arrays;
import java.util.Random;

import rfx.core.util.Utils;

public class RamdomCodesUtil {
	private static final Random RND = new Random(System.currentTimeMillis());

	/**
	 * Generates a random code according to given config.
	 * 
	 * @param config
	 * 
	 * @return Generated code.
	 */
	public static String generate(RandomCodeConfig config) {
		StringBuilder sb = new StringBuilder();
		char[] chars = config.getCharset().toCharArray();
		char[] pattern = config.getPattern().toCharArray();

		if (config.getPrefix() != null) {
			sb.append(config.getPrefix());
		}

		for (int i = 0; i < pattern.length; i++) {
			if (pattern[i] == RandomCodeConfig.PATTERN_PLACEHOLDER) {
				sb.append(chars[RND.nextInt(chars.length)]);
			} else {
				sb.append(pattern[i]);
			}
		}

		if (config.getPostfix() != null) {
			sb.append(config.getPostfix());
		}

		return sb.toString();
	}
	
	public static class Charset {
		public static final String ALPHABETIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		public static final String ALPHANUMERIC = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		public static final String NUMBERS = "0123456789";
	}

	public static class RandomCodeConfig {
		public final static char PATTERN_PLACEHOLDER = '#';

		private final int length;
		private final String charset;
		private final String prefix;
		private final String postfix;
		private final String pattern;

		public RandomCodeConfig(Integer length, String charset, String prefix, String postfix, String pattern) {
			if (length == null) {
				length = 8;
			}

			if (charset == null) {
				charset = Charset.ALPHANUMERIC;
			}

			if (pattern == null) {
				char[] chars = new char[length];
				Arrays.fill(chars, PATTERN_PLACEHOLDER);
				pattern = new String(chars);
			}

			this.length = length;
			this.charset = charset;
			this.prefix = prefix;
			this.postfix = postfix;
			this.pattern = pattern;
		}

		public static RandomCodeConfig length(int length) {
			return new RandomCodeConfig(length, null, null, null, null);
		}

		public static RandomCodeConfig pattern(String pattern) {
			return new RandomCodeConfig(null, null, null, null, pattern);
		}

		public int getLength() {
			return length;
		}

		public String getCharset() {
			return charset;
		}

		public RandomCodeConfig withCharset(String charset) {
			return new RandomCodeConfig(length, charset, prefix, postfix, pattern);
		}

		public String getPrefix() {
			return prefix;
		}

		public RandomCodeConfig withPrefix(String prefix) {
			return new RandomCodeConfig(length, charset, prefix, postfix, pattern);
		}

		public String getPostfix() {
			return postfix;
		}

		public RandomCodeConfig withPostfix(String postfix) {
			return new RandomCodeConfig(length, charset, prefix, postfix, pattern);
		}

		public String getPattern() {
			return pattern;
		}

		@Override
		public String toString() {
			return "VoucherCodeConfig [" + "length=" + length + ", " + "charset=" + charset + ", " + "prefix="
					+ prefix + ", " + "postfix=" + postfix + ", " + "pattern=" + pattern + "]";
		}
	}

}

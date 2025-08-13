package leotech.system.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.github.jknack.handlebars.internal.text.StringEscapeUtils;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import rfx.core.util.StringUtil;

/**
 * Cross-Site Scripting (XSS) filter utility
 * 
 * @author tantrieuf31
 *
 */
public class XssFilterUtil {

	static String[] TAGS = new String[] { "html", "head", "body", "div", "span", "p", "a", "img", "br", "hr", "h1",
			"h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "table", "thead", "tbody", "tfoot", "tr", "td", "th", "b",
			"i", "u", "em", "strong", "small", "mark", "code", "pre", "blockquote", "nav", "section", "article",
			"aside", "header", "footer", "label", "source", "figure", "figcaption", "canvas", "details", "summary", "main", "time",
			"progress", "meter", "abbr", "cite", "del", "ins" };

	// Allows all elements and attributes, but removes <script> and its contents
	static final PolicyFactory POLICY = new HtmlPolicyBuilder().allowElements(TAGS) // allow all elements
			.allowTextIn("a", "span", "p", "div") // allow raw text
			.allowWithoutAttributes(TAGS).disallowElements("script") // explicitly disallow script
			.toFactory();

	public static String safeGet(JsonObject params, String name) {
		return safeGet(params, name, "");
	}

	public static String safeGet(JsonObject params, String name, String defaultVal) {
		return clean(params.getString(name, defaultVal));
	}

	public static double safeGetDouble(JsonObject params, String name, double defaultVal) {
		try {
			return params.getDouble(name, defaultVal);
		} catch (Exception e) {
		}
		return defaultVal;
	}

	public static int safeGetInteger(JsonObject params, String name, int defaultVal) {
		try {
			return params.getInteger(name, defaultVal);
		} catch (Exception e) {
		}
		return defaultVal;
	}

	public static String safeGet(MultiMap params, String name) {
		return safeGet(params, name, "");
	}

	public static String safeGet(MultiMap params, String name, String defaultVal) {
		String value = params.get(name);
		if (value != null) {
			return clean(value);
		}
		return defaultVal;
	}

	/**
	 * @param obj
	 * @param clazz
	 */
	public static void cleanAllHtmlTags(Object obj, Class<?> clazz) {
		Field[] fields = FieldUtils.getAllFields(clazz);
		for (Field field : fields) {
			if (field.getType().getSimpleName().equals("String")) {
				field.setAccessible(true);
				boolean check = !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers());
				if (check) {
					try {
						Object fieldValue = field.get(obj);
						if (fieldValue != null) {
							String value = String.valueOf(fieldValue);
							if (StringUtil.isNotEmpty(value)) {
								// System.out.println(field.getName() + " BEFORE " + value);

								// clean all HTML tags
								value = clean(value);
								field.set(obj, value);

								// System.out.println(field.getName() + " AFTER " + value);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				field.setAccessible(false);
			}
		}
	}

	/**
	 * Remove all <script> tags and their content from the input string.
	 *
	 * @param value the input value
	 * @return cleaned string with <script> tags removed
	 */
	public static String clean(Object value) {
		String s = StringUtil.safeString(value, "");
		if (s.isBlank())
			return s;
		return StringEscapeUtils.unescapeHtml4(POLICY.sanitize(s).trim());
	}

}

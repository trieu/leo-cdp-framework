package leotech.system.template;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.mustachejava.MustacheException;

import rfx.core.util.StringPool;

/**
 * the utility class for Handlebars.java template (See more at
 * http://jknack.github.io/handlebars.java/)
 * 
 * @author Trieu.nguyen
 *
 */
public final class HandlebarsTemplateUtil {

	final static Map<String, Template> mustacheMap = new ConcurrentHashMap<>();// for hot deployment & update template
	final static String BASE_TEMPLATE_PATH = "resources/app-templates/";
	final static String TEMPLATE_SUFFIX = ".html";

	final static TemplateLoader loader = new FileTemplateLoader(BASE_TEMPLATE_PATH, TEMPLATE_SUFFIX);
	final static Handlebars handlebars = new Handlebars(loader);

	final static AtomicBoolean isUsedCache = new AtomicBoolean(true);

	static {
		HandlebarsHelpers.register(handlebars);

		// when a field is evaluated as null or no value, catch the error here
		handlebars.registerHelperMissing(new Helper<Object>() {
			@Override
			public CharSequence apply(final Object context, final Options options) throws IOException {
				Template tpl = options.fn;
				String field = tpl.text();
				System.err.println(field + " is (NULL value) or (NOT existed) or (not found public getter) at " + tpl.filename());
				// TODO log error to somewhere for easily debugging
				return StringPool.BLANK;
			}
		});
	}

	public static void refreshCache() {
		mustacheMap.clear();
	}

	public static void disableUsedCache() {
		isUsedCache.set(false);
	}

	public static void enableUsedCache() {
		isUsedCache.set(true);
	}

	public static Template getCompiledTemplate(String tplPath) throws IOException {
		if (isUsedCache.get()) {
			Template tpl = mustacheMap.get(tplPath);
			if (tpl == null) {
				System.out.println("HandlebarsTemplateUtil.compile and set cache for template " + tplPath);
				tpl = handlebars.compile(tplPath);
				mustacheMap.put(tplPath, tpl);
			}
			return tpl;
		} else {
			System.out.println("HandlebarsTemplateUtil.compile template " + tplPath);
			return handlebars.compile(tplPath);
		}
	}

	public static void compileAndCache(String tplPath) throws IOException {
		mustacheMap.put(tplPath, handlebars.compile(tplPath));
		System.out.println("...HandlebarsTemplateUtil.compileAndCache template: " + tplPath);
	}

	public final static String process(String tplPath, Object model, String errorMessageIfNotFound) {
		if (tplPath == null) {
			throw new IllegalArgumentException("tplPath is NULL");
		}
		try {
			Template template = getCompiledTemplate(tplPath);
			return template.apply(model);
		} catch (MustacheException e) {
			StringBuilder s = new StringBuilder("Error:");
			s.append(e.getMessage());
			return s.toString();
		} catch (Throwable e) {
			if (e instanceof FileNotFoundException) {
				return errorMessageIfNotFound;
			} else {
				StringBuilder s = new StringBuilder("Error###");
				s.append(e.getMessage());
				s.append(" ### <br>\n StackTrace: ").append(ExceptionUtils.getStackTrace(e));
				return s.toString();
			}

		}
	}
}

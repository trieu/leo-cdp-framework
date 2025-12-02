package leotech.system.template;

import org.apache.commons.text.StringEscapeUtils;

import leotech.web.model.DefaultModel;

public class TemplateUtil {

	public static final String _500 = "500";
	public static final String _404 = "404";

	public static String process(String tplPath, Object model) {
		String s = HandlebarsTemplateUtil.process(tplPath, model, _404);
		return s;
	}

	public static String process(String tplPath, DataModel model) {
		String s = HandlebarsTemplateUtil.process(tplPath, model, _404);
		return s;
	}

	public static String render(String tplPath, DataModel model) {
		return StringEscapeUtils.unescapeHtml4(process(tplPath, model));
	}

	public static String render(String tplPath) {
		String s = process(tplPath, new DefaultModel());
		return StringEscapeUtils.unescapeHtml4(s);
	}
}

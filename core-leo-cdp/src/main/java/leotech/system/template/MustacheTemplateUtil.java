package leotech.system.template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;




import rfx.core.util.StringUtil;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;

/**
 * the utility class for Mustache template (See more at http://mustache.github.io)
 * 
 * @author Trieu.nguyen 
 *
 */
public class MustacheTemplateUtil {

	static final Map<String, String> EMPTY_MODEL = new HashMap<>(0);
	final static Map<String, Mustache> mustacheMap = new ConcurrentHashMap<>();//for hot deployment & update template 
	final static String BASE_TEMPLATE_PATH = "resources/app-templates/";
	final static String TEMPLATE_SUFFIX = ".html";
	
	//flag
	final static AtomicBoolean isUsedCache = new AtomicBoolean(true);
	volatile static MustacheFactory mustacheFactory = null;
	
	public static MustacheFactory getMustacheFactory() {
		if(mustacheFactory == null){
			mustacheFactory = new DefaultMustacheFactory();
		}
		return mustacheFactory;
	}
	
	public static void refreshCache(){
		mustacheFactory = null;
		mustacheMap.clear();		
	}
	
	public static void disableUsedCache(){
		isUsedCache.set(false);		
	}
	
	public static void enableUsedCache(){
		isUsedCache.set(true);		
	}
	
	public static Mustache getCompiledTemplate(String tplPath){		
		if(isUsedCache.get()){
			Mustache tpl = mustacheMap.get(tplPath);
			if(tpl == null){
				String fullpath = StringUtil.toString(BASE_TEMPLATE_PATH , tplPath, TEMPLATE_SUFFIX);
				tpl = getMustacheFactory().compile(fullpath);
				mustacheMap.put(tplPath, tpl);
			}		
			return tpl;
		} else {			
			String fullpath = StringUtil.toString(BASE_TEMPLATE_PATH , tplPath, TEMPLATE_SUFFIX);
			return new DefaultMustacheFactory().compile(fullpath);
		}        		
	}
	
	public static String execute(String tplPath, Object model){	
		if(tplPath == null){
			throw new IllegalArgumentException("tplPath is NULL");
		}
		try {
			StringWriter strWriter = new StringWriter();
			getCompiledTemplate(tplPath).execute(strWriter, model).flush();
			strWriter.flush();
			strWriter.close();				
			return strWriter.toString();
		} catch (MustacheException e) {
			StringBuilder s = new StringBuilder("Error:");
			s.append(e.getMessage());
			return s.toString();
		} catch (Throwable e) {
			StringBuilder s = new StringBuilder("Error###");
			s.append(e.getMessage());
			s.append(" ### <br>\n StackTrace: ").append(ExceptionUtils.getStackTrace(e));
			return s.toString();
		}
	}
	
}

package leotech.system.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * scan to convert int constants into string
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class ReflectionUtil {

	private static final String INT = "int";
	
	public static final Map<Integer, String> getConstantMap(Class<?> clazz){
		Field[] declaredFields = FieldUtils.getAllFields(clazz);
		Map<Integer, String> typeMap = new HashMap<>(declaredFields.length);
		
		for (Field field : declaredFields) {
		    int modifiers = field.getModifiers();
		    boolean isIntType = field.getType().getSimpleName().equals(INT) ;
			if (isIntType && java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) {
				try {
					int v = field.getInt(null);
					String n = field.getName();
			        typeMap.put(v, n);
				}  catch (Exception e) {
					e.printStackTrace();
				}
		    }
		}
		return typeMap;
	}
	
	public static final Map<String, Integer> getConstantIntegerMap(Class<?> clazz){
		Field[] declaredFields = FieldUtils.getAllFields(clazz);
		Map<String, Integer> typeMap = new HashMap<>(declaredFields.length);
		
		for (Field field : declaredFields) {
		    int modifiers = field.getModifiers();
		    boolean isIntType = field.getType().getSimpleName().equals(INT) ;
			if (isIntType && java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) {
				try {
					int v = field.getInt(null);
					String n = field.getName();
			        typeMap.put(n, v);
				}  catch (Exception e) {
					e.printStackTrace();
				}
		    }
		}
		return typeMap;
	}
	

	
	public static final Map<String, String> getConstantStringMap(Class<?> clazz){
		Field[] declaredFields = FieldUtils.getAllFields(clazz);
		Map<String, String> typeMap = new HashMap<>(declaredFields.length);
		
		for (Field field : declaredFields) {
		    int modifiers = field.getModifiers();
		    boolean isStringType = field.getType().getSimpleName().equals("String") ;
			if (isStringType && java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) {
				try {
					String v = String.valueOf(field.get(null));
					String n = field.getName();
			        typeMap.put(n, v);
				}  catch (Exception e) {
					e.printStackTrace();
				}
		    }
		}
		return typeMap;
	}
}

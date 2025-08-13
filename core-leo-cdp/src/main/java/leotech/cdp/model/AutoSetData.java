package leotech.cdp.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface AutoSetData {
	public boolean setDataAsString() default false;
	public boolean setDataAsInteger() default false;
	public boolean setDataAsFloat() default false;
	public boolean setDataAsDouble() default false;
	public boolean setDataAsDate() default false;
	public boolean setDataAsJsonMap() default false;
}

package leotech.cdp.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the metadata to compute data quality score of profile
 * @author tantrieuf31
 * @since 2022
 *
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ProfileMetaDataField {
	
	public boolean identityResolutionKey() default false;
	public int dataQualityScore() default 0;
	public String invalidWhenEqual() default "";
	public String label() default "";
	public boolean synchronizable() default false;
	public boolean exposedInSegment() default false;
}
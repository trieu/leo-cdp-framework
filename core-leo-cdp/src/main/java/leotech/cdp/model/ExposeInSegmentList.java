package leotech.cdp.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation to set the field in customer profile can be used for data segmentation
 * @author tantrieuf31
 * @since 2024
 *
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ExposeInSegmentList {}
package org.sokybot.webview.api.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata for a parameter in an RSocket method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface RSocketParam {
    String name();

    String type() default "string";

    boolean required() default true;

    String description() default "";
}

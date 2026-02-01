package org.sokybot.webview.api.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata for an RSocket method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RSocketMethods.class)
public @interface RSocketMethod {
    String name();

    String description() default "";

    RSocketParam[] params() default {};

    String returnType() default "object";
}

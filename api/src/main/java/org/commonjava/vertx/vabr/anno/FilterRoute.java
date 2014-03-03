package org.commonjava.vertx.vabr.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.commonjava.vertx.vabr.types.Method;

@Target( { ElementType.METHOD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface FilterRoute
{
    String apiVersion() default Handles.DEFAULT_API_VERSION;

    String value() default "";

    String path() default "";

    Method method() default Method.GET;

    int priority() default 50;
}

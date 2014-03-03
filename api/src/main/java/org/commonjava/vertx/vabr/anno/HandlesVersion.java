package org.commonjava.vertx.vabr.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( { ElementType.TYPE } )
@Retention( RetentionPolicy.RUNTIME )
public @interface HandlesVersion
{
    String DEFAULT_API_VERSION = "0.0";

    String apiVersion() default DEFAULT_API_VERSION;

    String prefix() default "";

    String value() default "";

    String key() default "";
}

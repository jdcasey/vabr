package org.commonjava.freeki.infra.route.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.commonjava.freeki.infra.route.Method;

@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface Route
{

    String path();

    Method method();

    String contentType() default "";

}

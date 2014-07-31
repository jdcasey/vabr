/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
    String value() default "";

    String path() default "";

    Method method() default Method.GET;

    int priority() default 50;

    String[] versions() default {};
}

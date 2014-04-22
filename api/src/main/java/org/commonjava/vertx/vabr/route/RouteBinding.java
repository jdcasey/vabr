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
package org.commonjava.vertx.vabr.route;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class RouteBinding
{

    public static final String RECOMMENDED_CONTENT_TYPE = "Recommended-Content-Type";

    private final String path;

    private final Method method;

    private final String contentType;

    private final int priority;

    private final String handlerKey;

    private final Class<?> handlesClass;

    private final String handlesMethodName;

    protected RouteBinding( final int priority, final String path, final Method method, final String contentType, final String handlerKey,
                            final Class<?> handlesClass, final String handlesMethodName )
    {
        this.priority = priority;
        this.path = path;
        this.method = method;
        this.handlerKey = handlerKey;
        this.handlesClass = handlesClass;
        this.handlesMethodName = handlesMethodName;
        this.contentType = contentType.length() < 1 ? null : contentType;
    }

    protected abstract void dispatch( ApplicationRouter router, HttpServerRequest req )
        throws Exception;

    public int getPriority()
    {
        return priority;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getPath()
    {
        return path;
    }

    public Method getMethod()
    {
        return method;
    }

    public String getHandlerKey()
    {
        return handlerKey;
    }

    @Override
    public String toString()
    {
        return "Route [" + method.name() + " " + path + "] => " + handlerKey;
    }

    public void handle( final ApplicationRouter router, final HttpServerRequest req )
        throws Exception
    {
        if ( contentType != null )
        {
            req.headers()
               .add( RECOMMENDED_CONTENT_TYPE, contentType );
        }

        dispatch( router, req );
    }

    public Class<?> getHandlesClass()
    {
        return handlesClass;
    }

    public String getHandlesMethodName()
    {
        return handlesMethodName;
    }

}

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
package org.commonjava.vertx.vabr.bind.route;

import java.util.List;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.bind.AbstractBinding;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class RouteBinding
    extends AbstractBinding
{

    private final String contentType;

    private final Class<?> handlesClass;

    private final String handlesMethodName;

    protected RouteBinding( final int priority, final String path, final Method method, final String contentType,
                            final String handlerKey, final Class<?> handlesClass, final String handlesMethodName,
                            final List<String> versions )
    {
        super( priority, path, method, handlerKey, versions );
        this.handlesClass = handlesClass;
        this.handlesMethodName = handlesMethodName;
        this.contentType = contentType.length() < 1 ? null : contentType;
    }

    protected abstract void dispatch( ApplicationRouter router, HttpServerRequest req )
        throws Exception;

    public String getContentType()
    {
        return contentType;
    }

    @Override
    public String toString()
    {
        return "Route [" + method.name() + " " + path + "] => " + handlerKey;
    }

    public void handle( final ApplicationRouter router, final HttpServerRequest req )
        throws Exception
    {
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

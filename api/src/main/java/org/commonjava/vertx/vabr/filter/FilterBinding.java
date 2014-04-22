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
package org.commonjava.vertx.vabr.filter;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class FilterBinding
    implements Comparable<FilterBinding>
{

    private final String path;

    private final Method method;

    private final int priority;

    private final String handlerKey;

    protected FilterBinding( final int priority, final String path, final Method method, final String handlerKey )
    {
        this.priority = priority;
        this.path = path;
        this.method = method;
        this.handlerKey = handlerKey;
    }

    public int getPriority()
    {
        return priority;
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
        return "Filter [" + method.name() + " " + path + "] => " + handlerKey;
    }

    public void handle( final ApplicationRouter router, final HttpServerRequest req, final ExecutionChain next )
        throws Exception
    {
        dispatch( router, req, next );
    }

    protected abstract void dispatch( ApplicationRouter router, HttpServerRequest req, ExecutionChain next )
        throws Exception;

    @Override
    public int compareTo( final FilterBinding other )
    {
        return Integer.valueOf( priority )
                      .compareTo( other.priority );
    }

}

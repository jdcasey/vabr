/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.vertx.vabr.filter;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.Method;
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

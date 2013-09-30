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
package org.commonjava.freeki.infra.route;

import org.vertx.java.core.http.HttpServerRequest;

public abstract class RouteBinding
{

    public static final String RECOMMENDED_CONTENT_TYPE = "Recommended-Content-Type";

    private final String path;

    private final Method method;

    private final String contentType;

    protected RouteBinding( final String path, final Method method, final String contentType )
    {
        this.path = path;
        this.method = method;
        this.contentType = contentType.length() < 1 ? null : contentType;
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

    @Override
    public String toString()
    {
        return "Route [" + method.name() + " " + path + "]";
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

    protected abstract void dispatch( ApplicationRouter router, HttpServerRequest req )
        throws Exception;

}

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
 
package ${pkg};

import org.commonjava.freeki.infra.route.ApplicationRouter;
import org.commonjava.freeki.infra.route.RouteBinding;
import org.commonjava.freeki.infra.route.Method;
import org.commonjava.freeki.infra.route.AbstractRouteCollection;
import org.vertx.java.core.http.HttpServerRequest;
import org.commonjava.util.logging.Logger;

public final class Routes
    extends AbstractRouteCollection
{

    private final Logger logger = new Logger( getClass() );

    public Routes()
    {<% routes.each { %>
        bind( new RouteBinding( "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}" )
        {
            public void dispatch( ApplicationRouter router, HttpServerRequest req )
                throws Exception
            {
                ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
                if ( handler != null )
                {
                    logger.info( "Handling via: %s", handler );
                    handler.${it.methodname}( req );
                }
                else
                {
                    throw new RuntimeException( "Cannot retrieve handler instance for: '${it.httpPath}' using method: '${it.httpMethod.name()}'" );
                } 
            }
        } );<% } %>
    }

}

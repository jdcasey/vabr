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
package org.commonjava.vertx.vabr;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.helper.NoMatchHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class MultiApplicationRouter
    implements Handler<HttpServerRequest>
{

    protected final Logger logger = new Logger( getClass() );

    private final String prefix;

    private final List<ApplicationRouter> routers;

    private NoMatchHandler noMatchHandler;

    public MultiApplicationRouter()
    {
        this.prefix = null;
        this.routers = new ArrayList<>();
    }

    public MultiApplicationRouter( final Iterable<ApplicationRouter> routers )
    {
        this.prefix = null;
        this.routers = new ArrayList<>();
        for ( final ApplicationRouter router : routers )
        {
            this.routers.add( router );
        }
    }

    public MultiApplicationRouter( final String prefix )
    {
        this.prefix = prefix;
        this.routers = new ArrayList<>();
    }

    public MultiApplicationRouter( final String prefix, final Iterable<ApplicationRouter> routers )
    {
        this.prefix = prefix;
        this.routers = new ArrayList<>();
        for ( final ApplicationRouter router : routers )
        {
            this.routers.add( router );
        }
    }

    @Override
    public void handle( final HttpServerRequest request )
    {
        try
        {
            String path = request.path();
            boolean proceed = true;
            if ( prefix != null )
            {
                if ( !path.startsWith( prefix ) )
                {
                    proceed = false;
                }
                else
                {
                    path = path.substring( prefix.length() - 1 );
                }
            }

            boolean found = false;
            if ( !proceed )
            {
                for ( final ApplicationRouter router : routers )
                {
                    if ( router.routeRequest( path, request ) )
                    {
                        found = true;
                        break;
                    }
                }
            }

            if ( !found )
            {
                if ( noMatchHandler != null )
                {
                    noMatchHandler.handle( request );
                }

                // Default 404
                request.response()
                       .setStatusCode( 404 )
                       .setStatusMessage( "Not Found" )
                       .setChunked( true )
                       .write( "No handler found" );
            }
        }
        catch ( final Throwable t )
        {
            logger.error( "ERROR: %s", t, t.getMessage() );
            request.response()
                   .setStatusCode( 500 )
                   .setStatusMessage( "Internal Server Error" )
                   .setChunked( true )
                   .write( "Error occurred during processing. See logs for more information." );
        }
        finally
        {
            request.response()
                   .end();
        }
    }

}

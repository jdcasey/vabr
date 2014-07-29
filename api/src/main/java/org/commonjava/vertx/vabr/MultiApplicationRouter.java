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
package org.commonjava.vertx.vabr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.commonjava.vertx.vabr.helper.NoMatchHandler;
import org.commonjava.vertx.vabr.helper.RedirectHandler;
import org.commonjava.vertx.vabr.util.AppPrefixComparator;
import org.commonjava.vertx.vabr.util.RouterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class MultiApplicationRouter
    implements Handler<HttpServerRequest>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private String prefix;

    private final List<ApplicationRouter> routers;

    private NoMatchHandler noMatchHandler;

    private ExecutorService executor;

    public MultiApplicationRouter()
    {
        this.prefix = null;
        this.routers = new ArrayList<>();
    }

    public MultiApplicationRouter( final String prefix )
    {
        this.prefix = prefix;
        this.routers = new ArrayList<>();
    }

    public MultiApplicationRouter( final Iterable<ApplicationRouter> routers )
    {
        this.prefix = null;
        this.routers = new ArrayList<>();
        bindRouters( routers );
    }

    public MultiApplicationRouter( final String prefix, final Iterable<ApplicationRouter> routers )
    {
        this.prefix = prefix;
        this.routers = new ArrayList<>();
        bindRouters( routers );
    }

    public MultiApplicationRouter( final ExecutorService executor )
    {
        this.executor = executor;
        this.prefix = null;
        this.routers = new ArrayList<>();
    }

    public MultiApplicationRouter( final String prefix, final ExecutorService executor )
    {
        this.prefix = prefix;
        this.executor = executor;
        this.routers = new ArrayList<>();
    }

    public MultiApplicationRouter( final Iterable<ApplicationRouter> routers, final ExecutorService executor )
    {
        this.executor = executor;
        this.prefix = null;
        this.routers = new ArrayList<>();
        bindRouters( routers );
    }

    public MultiApplicationRouter( final String prefix, final Iterable<ApplicationRouter> routers, final ExecutorService executor )
    {
        this.prefix = prefix;
        this.executor = executor;
        this.routers = new ArrayList<>();
        bindRouters( routers );
    }

    public void setHandlerExecutor( final ExecutorService executor )
    {
        this.executor = executor;
    }

    public synchronized ExecutorService getHandlerExecutor()
    {
        if ( executor == null )
        {
            executor = Executors.newCachedThreadPool();
        }

        return executor;
    }

    protected void bindRouters( final Iterable<? extends ApplicationRouter> routers )
    {
        for ( final ApplicationRouter router : routers )
        {
            this.routers.add( router );
            router.setHandlerExecutor( executor );
        }

        Collections.sort( this.routers, new AppPrefixComparator() );
    }

    @Override
    public void handle( final HttpServerRequest request )
    {
        request.pause();
        try
        {
            String path = request.path();
            boolean proceed = true;
            path = RouterUtils.trimPrefix( prefix, path );
            if ( path == null )
            {
                proceed = false;
            }

            boolean found = false;
            if ( proceed )
            {
                for ( final ApplicationRouter router : routers )
                {
                    logger.info( "attempting to route '{}' via: {}", path, router );
                    if ( router.routeRequest( path, request ) )
                    {
                        found = true;
                        break;
                    }
                }
            }

            // TODO: Determine whether to respond when proceed == false.

            if ( !found )
            {
                final NoMatchHandler handler = noMatchHandler == null ? new RedirectHandler( prefix ) : noMatchHandler;
                handler.handle( request );
                //                else
                //                {
                //                    // Default 404
                //                    request.response()
                //                           .setStatusCode( 404 )
                //                           .setStatusMessage( "Not Found" )
                //                           .setChunked( true )
                //                           .write( "No handler found" )
                //                           .end();
                //                }
            }
        }
        catch ( final Throwable t )
        {
            logger.error( "ERROR: {}", t, t.getMessage() );
            request.resume()
                   .response()
                   .setStatusCode( 500 )
                   .setStatusMessage( "Internal Server Error" )
                   .setChunked( true )
                   .end( "Error occurred during processing. See logs for more information." );
        }
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix( final String prefix )
    {
        this.prefix = prefix;
    }

}

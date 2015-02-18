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
package org.commonjava.vertx.vabr.helper;

import java.util.LinkedList;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.bind.AbstractBinding;
import org.commonjava.vertx.vabr.bind.BindingContext;
import org.commonjava.vertx.vabr.bind.filter.ExecutionChain;
import org.commonjava.vertx.vabr.bind.filter.FilterBinding;
import org.commonjava.vertx.vabr.bind.route.RouteBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public class ExecutionChainHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ApplicationRouter router;

    private LinkedList<FilterBinding> filters;

    private RouteBinding route;

    private final HttpServerRequest request;

    public ExecutionChainHandler( final ApplicationRouter router, final BindingContext ctx, final HttpServerRequest request )
    {
        this.router = router;
        this.request = request;
        if ( ctx.getPatternFilterBinding() != null )
        {
            filters = new LinkedList<>( ctx.getPatternFilterBinding()
                                           .getFilters() );
        }

        if ( ctx.getPatternRouteBinding() != null )
        {
            route = ctx.getPatternRouteBinding()
                       .getHandler();
        }
    }

    public void execute()
        throws Exception
    {
        if ( filters != null )
        {
            new ExecutionChainImpl().handle();
        }
        else
        {
            route.handle( router, request );
        }
    }

    public final class ExecutionChainImpl
        implements ExecutionChain
    {
        private AbstractBinding binding;

        private ExecutionChainImpl()
        {
        }

        public boolean isHandled()
        {
            return binding != null;
        }

        @Override
        public void handle()
            throws Exception
        {
            if ( binding != null )
            {
                logger.info( "ExecutionChain already called for: {}. Returning.", binding );
                return;
            }

            if ( filters == null || filters.isEmpty() )
            {
                logger.info( "ExecutionChain triggering route: {}", route );
                binding = route;

                route.handle( router, request );
                logger.info( "Router execution done" );
            }
            else
            {
                final FilterBinding filter = filters.removeFirst();
                binding = filter;

                logger.info( "ExecutionChain triggering filter: {}", filter );
                final ExecutionChainImpl next = new ExecutionChainImpl();
                filter.handle( router, request, next );

                if ( !next.isHandled() )
                {
                    logger.info( "Next in chain not triggered by filter. Triggering now." );
                    next.handle();
                }
                logger.info( "Filter execution done" );
            }
        }

    }

}

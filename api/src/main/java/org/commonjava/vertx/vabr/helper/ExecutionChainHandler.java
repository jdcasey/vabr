package org.commonjava.vertx.vabr.helper;

import java.util.List;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.filter.ExecutionChain;
import org.commonjava.vertx.vabr.filter.FilterBinding;
import org.commonjava.vertx.vabr.route.RouteBinding;
import org.vertx.java.core.http.HttpServerRequest;

public class ExecutionChainHandler
{

    private final ApplicationRouter router;

    private List<FilterBinding> filters;

    private RouteBinding route;

    private List<ExecutionChain> chains;

    private final HttpServerRequest request;

    public ExecutionChainHandler( final ApplicationRouter router, final BindingContext ctx, final HttpServerRequest request )
    {
        this.router = router;
        this.request = request;
        if ( ctx.getFilterBinding() != null )
        {
            filters = ctx.getFilterBinding()
                         .getFilters();
        }

        if ( ctx.getRouteBinding() != null )
        {
            route = ctx.getRouteBinding()
                       .getHandler();
        }
    }

    public void execute()
    {
        int i = 0;
        if ( filters != null )
        {
            for ( ; i < filters.size(); i++ )
            {
                // one for each filter.
                chains.add( new ExecutionChainImpl( i ) );
            }

            // for base route binding.
            chains.add( new ExecutionChainImpl( i ) );
        }
    }

    public final class ExecutionChainImpl
        implements ExecutionChain
    {
        private final int currentFilterIndex;

        private ExecutionChainImpl( final int idx )
        {
            this.currentFilterIndex = idx;
        }

        @Override
        public void handle()
            throws Exception
        {
            if ( filters == null || currentFilterIndex >= filters.size() )
            {
                route.handle( router, request );
            }
            else
            {
                final FilterBinding filter = filters.get( currentFilterIndex );
                final ExecutionChain next = chains.get( currentFilterIndex + 1 );

                filter.handle( router, request, next );
            }
        }

    }

}

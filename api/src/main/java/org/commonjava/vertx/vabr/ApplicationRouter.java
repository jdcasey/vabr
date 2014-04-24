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

import static org.commonjava.vertx.vabr.util.AnnotationUtils.getHandlerKey;
import static org.commonjava.vertx.vabr.util.RouterUtils.requestUri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.filter.FilterBinding;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.helper.BindingContext;
import org.commonjava.vertx.vabr.helper.ExecutionChainHandler;
import org.commonjava.vertx.vabr.helper.PatternFilterBinding;
import org.commonjava.vertx.vabr.helper.PatternRouteBinding;
import org.commonjava.vertx.vabr.route.RouteBinding;
import org.commonjava.vertx.vabr.route.RouteCollection;
import org.commonjava.vertx.vabr.types.BuiltInParam;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.RouterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class ApplicationRouter
    implements Handler<HttpServerRequest>
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private Map<Method, List<PatternRouteBinding>> routeBindings = new HashMap<>();

    private final Map<Method, List<PatternFilterBinding>> filterBindings = new HashMap<>();

    private final Map<String, Object> handlers = new HashMap<>();

    private Handler<HttpServerRequest> noMatchHandler;

    private String prefix;

    private ExecutorService executor;

    public ApplicationRouter()
    {
        this.prefix = null;
    }

    public ApplicationRouter( final ExecutorService executor )
    {
        this.executor = executor;
        this.prefix = null;
    }

    public ApplicationRouter( final String prefix )
    {
        this.prefix = prefix;
    }

    public ApplicationRouter( final String prefix, final ExecutorService executor )
    {
        this.prefix = prefix;
        this.executor = executor;
    }

    public ApplicationRouter( final Iterable<?> routes, final Iterable<RouteCollection> routeCollections )
    {
        this.prefix = null;
        bindRoutes( routes, routeCollections );
    }

    public ApplicationRouter( final Iterable<?> routes, final Iterable<RouteCollection> routeCollections, final ExecutorService executor )
    {
        this.executor = executor;
        this.prefix = null;
        bindRoutes( routes, routeCollections );
    }

    public ApplicationRouter( final String prefix, final Iterable<?> routes, final Iterable<RouteCollection> routeCollections )
    {
        this.prefix = prefix;
        bindRoutes( routes, routeCollections );
    }

    public ApplicationRouter( final String prefix, final Iterable<?> routes, final Iterable<RouteCollection> routeCollections,
                              final ExecutorService executor )
    {
        this.prefix = prefix;
        this.executor = executor;
        bindRoutes( routes, routeCollections );
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

    public void bindFilters( final Iterable<?> handlers, final Iterable<FilterCollection> filterCollections )
    {
        if ( handlers != null )
        {
            for ( final Object handler : handlers )
            {
                final String key = getHandlerKey( handler.getClass() );
                if ( this.handlers.containsKey( key ) )
                {
                    continue;
                }

                logger.info( "Handlers += {} ({})", key, handler.getClass()
                                                                .getName() );
                this.handlers.put( key, handler );
            }
        }

        if ( filterCollections != null )
        {
            for ( final FilterCollection fc : filterCollections )
            {
                logger.info( "Binding filters in collection: {}", fc.getClass()
                                                                    .getName() );

                for ( final FilterBinding fb : fc )
                {
                    if ( !this.handlers.containsKey( fb.getHandlerKey() ) )
                    {
                        logger.error( "Route handler '{}' not found for binding: {}", fb.getHandlerKey(), fb );
                    }

                    bind( fb );
                }
            }
        }
    }

    public void bindRoutes( final Iterable<?> handlers, final Iterable<RouteCollection> routeCollections )
    {
        if ( handlers != null )
        {
            for ( final Object handler : handlers )
            {
                final String key = getHandlerKey( handler.getClass() );
                if ( this.handlers.containsKey( key ) )
                {
                    continue;
                }

                logger.info( "Handlers += {} ({})", key, handler.getClass()
                                                                .getName() );
                this.handlers.put( key, handler );
            }
        }

        if ( routeCollections != null )
        {
            for ( final RouteCollection rc : routeCollections )
            {
                logger.info( "Binding routes in collection: {}", rc.getClass()
                                                                   .getName() );

                for ( final RouteBinding rb : rc )
                {
                    if ( !this.handlers.containsKey( rb.getHandlerKey() ) )
                    {
                        logger.error( "Route handler '{}' not found for binding: {}", rb.getHandlerKey(), rb );
                    }

                    logger.info( "Routes += {} ({})", rb.getPath(), rb.getMethod() );
                    bind( rb );
                }
            }
        }
    }

    public <T> T getResourceInstance( final Class<T> cls )
    {
        final Object handler = handlers.get( getHandlerKey( cls ) );
        return handler == null ? null : cls.cast( handler );
    }

    @Override
    public void handle( final HttpServerRequest request )
    {
        request.pause();
        try
        {
            if ( !routeRequest( request.path(), request ) )
            {
                if ( noMatchHandler != null )
                {
                    noMatchHandler.handle( request );
                }
                else
                {
                    // Default 404
                    request.response()
                           .setStatusCode( 404 )
                           .setStatusMessage( "Not Found" )
                           .setChunked( true )
                           .write( "No handler found" );
                }
            }
        }
        catch ( final Throwable t )
        {
            logger.error( "ERROR: {}", t, t.getMessage() );
            request.response()
                   .setStatusCode( 500 )
                   .setStatusMessage( "Internal Server Error" )
                   .setChunked( true )
                   .write( "Error occurred during processing. See logs for more information." );
        }
    }

    public boolean routeRequest( String path, final HttpServerRequest request )
        throws Exception
    {
        logger.info( "Originating path: {}", path );
        path = RouterUtils.trimPrefix( prefix, path );
        if ( path == null )
        {
            return false;
        }

        final Method method = Method.valueOf( request.method() );

        logger.info( "REQUEST>>> {} {}\n", method, path );

        BindingContext ctx = findBinding( method, path );

        if ( ctx == null )
        {
            ctx = findBinding( Method.ANY, path );
        }

        if ( ctx != null )
        {
            final RouteBinding handler = ctx.getRouteBinding()
                                            .getHandler();
            // FIXME Wrap this in an executor that knows about filters AND the fundamental route...
            logger.info( "MATCH: {}\n", handler );
            parseParams( ctx, request );

            new ExecutionChainHandler( this, ctx, request ).execute();
            return true;
        }

        return false;
    }

    protected void parseParams( final BindingContext ctx, final HttpServerRequest request )
    {
        final Matcher matcher = ctx.getMatcher();

        final Map<String, String> params = new HashMap<>( matcher.groupCount() );

        final String fullPath = request.path();
        final String uri = requestUri( request );

        final PatternRouteBinding routeBinding = ctx.getRouteBinding();
        final List<String> paramNames = routeBinding.getParamNames();
        final RouteBinding handler = routeBinding.getHandler();

        final Handles pp = handler.getHandlesClass()
                                  .getAnnotation( Handles.class );
        if ( pp != null )
        {
            String pathPrefix = pp.value();
            if ( pathPrefix.length() < 1 )
            {
                pathPrefix = pp.prefix();
            }

            int idx = fullPath.indexOf( pathPrefix );
            if ( idx > -1 )
            {
                idx += pathPrefix.length();
                final String prefix = fullPath.substring( 0, idx );
                params.put( BuiltInParam._classBase.key(), prefix );

                idx = uri.indexOf( pathPrefix );
                params.put( BuiltInParam._classContextUrl.key(), uri.substring( 0, idx ) );
            }
        }

        if ( paramNames != null )
        {
            // Named params
            int i = 1;

            if ( !paramNames.isEmpty() )
            {
                final int firstIdx = matcher.start( i );

                // be defensive in case the first param is optional...
                String basePath = ( firstIdx > 0 ) ? fullPath.substring( 0, firstIdx ) : fullPath;

                if ( basePath.endsWith( "/" ) )
                {
                    basePath = basePath.substring( 0, basePath.length() - 1 );
                }

                params.put( BuiltInParam._routeBase.key(), basePath );

                final int idx = uri.indexOf( basePath ) + basePath.length();
                params.put( BuiltInParam._routeContextUrl.key(), uri.substring( 0, idx ) );
            }

            for ( final String param : paramNames )
            {
                final String v = matcher.group( i );
                if ( v != null )
                {
                    logger.info( "PARAM {} = {}", param, v );
                    params.put( param, v );
                }
                i++;
            }
        }
        else
        {
            // Un-named params
            for ( int i = 0; i < matcher.groupCount(); i++ )
            {
                final String v = matcher.group( i + 1 );
                if ( v != null )
                {
                    logger.info( "PARAM param{} = {}", i, v );
                    params.put( "param" + i, v );
                }
            }
        }

        final String query = request.query();
        if ( query != null )
        {
            final String[] qe = query.split( "&" );
            for ( final String entry : qe )
            {
                final int idx = entry.indexOf( '=' );
                if ( idx > 1 )
                {
                    params.put( "q:" + entry.substring( 0, idx ), entry.substring( idx + 1 ) );
                }
                else
                {
                    params.put( "q:" + entry, "true" );
                }
            }
        }

        //        logger.info( "PARAMS: {}\n", params );
        request.params()
               .set( params );
    }

    protected BindingContext findBinding( final Method method, final String path )
    {
        final List<PatternFilterBinding> allFilterBindings = this.filterBindings.get( method );

        PatternFilterBinding filterBinding = null;
        if ( allFilterBindings != null )
        {
            for ( final PatternFilterBinding binding : allFilterBindings )
            {
                if ( binding.getPattern()
                            .matcher( path )
                            .matches() )
                {
                    filterBinding = binding;
                    break;
                }
            }
        }

        final List<PatternRouteBinding> routeBindings = this.routeBindings.get( method );
        //        logger.info( "Available bindings:\n  {}\n", join( bindings, "\n  " ) );
        if ( routeBindings != null )
        {
            for ( final PatternRouteBinding binding : routeBindings )
            {
                final Matcher m = Pattern.compile( binding.getPattern() )
                                         .matcher( path );
                if ( m.matches() )
                {
                    return new BindingContext( m, binding, filterBinding );
                }
            }
        }

        return null;
    }

    /**
     * Specify a handler that will be called for all HTTP methods
     * @param pattern The simple pattern
     * @param handler The handler to call
     */
    public void bind( final RouteBinding handler )
    {
        final Method method = handler.getMethod();
        final String path = handler.getPath();

        List<PatternRouteBinding> b = routeBindings.get( method );
        if ( b == null )
        {
            b = new ArrayList<>();
            routeBindings.put( method, b );
        }

        logger.info( "ADD Method: {}, Pattern: {}, Route: {}\n", method, path, handler );
        addPattern( path, handler, b );
    }

    /**
     * Specify a filter handler that will be used to wrap route executions
     * @param pattern The simple pattern
     * @param handler The handler to call
     */
    public void bind( final FilterBinding handler )
    {
        final Method method = handler.getMethod();
        final String path = handler.getPath();

        List<PatternFilterBinding> b = filterBindings.get( method );
        if ( b == null )
        {
            b = new ArrayList<>();
            filterBindings.put( method, b );
        }

        logger.info( "ADD Method: {}, Pattern: {}, Filter: {}\n", method, path, handler );
        List<PatternFilterBinding> allFilterBindings = this.filterBindings.get( method );
        if ( allFilterBindings == null )
        {
            allFilterBindings = new ArrayList<>();
            this.filterBindings.put( method, allFilterBindings );
        }

        boolean found = false;
        for ( final PatternFilterBinding binding : allFilterBindings )
        {
            if ( binding.getPattern()
                        .pattern()
                        .equals( handler.getPath() ) )
            {
                binding.addFilter( handler );
                found = true;
                break;
            }
        }

        if ( !found )
        {
            final PatternFilterBinding binding = new PatternFilterBinding( Pattern.compile( handler.getPath() ), handler );
            allFilterBindings.add( binding );
        }
    }

    /**
     * Specify a handler that will be called when no other handlers match.
     * If this handler is not specified default behaviour is to return a 404
     * @param handler
     */
    public void noMatch( final Handler<HttpServerRequest> handler )
    {
        noMatchHandler = handler;
    }

    protected void addPattern( final String input, final RouteBinding handler, final List<PatternRouteBinding> bindings )
    {
        //        logger.info( "BIND regex: {}, groups: {}, route: {}\n", regex, groups, handler );
        final PatternRouteBinding binding = PatternRouteBinding.parse( input, handler );
        bindings.add( binding );

        Collections.sort( bindings );
    }

    public Map<Method, List<PatternRouteBinding>> getRouteBindings()
    {
        return routeBindings;
    }

    public Map<Method, List<PatternFilterBinding>> getFilterBindings()
    {
        return filterBindings;
    }

    public Map<String, ?> getHandlers()
    {
        return handlers;
    }

    public Handler<HttpServerRequest> getNoMatchHandler()
    {
        return noMatchHandler;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setNoMatchHandler( final Handler<HttpServerRequest> noMatchHandler )
    {
        this.noMatchHandler = noMatchHandler;
    }

    public void setPrefix( final String prefix )
    {
        this.prefix = prefix;
    }

    protected void setBindings( final Map<Method, List<PatternRouteBinding>> bindings )
    {
        this.routeBindings = bindings;
    }

    protected void setHandlers( final Map<String, Object> handlers )
    {
        this.handlers.clear();
        this.handlers.putAll( handlers );
    }

}

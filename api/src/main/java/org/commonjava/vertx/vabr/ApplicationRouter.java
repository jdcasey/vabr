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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.bind.BindingContext;
import org.commonjava.vertx.vabr.bind.BindingKey;
import org.commonjava.vertx.vabr.bind.PatternFilterBinding;
import org.commonjava.vertx.vabr.bind.PatternRouteBinding;
import org.commonjava.vertx.vabr.bind.filter.FilterBinding;
import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteBinding;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.AcceptInfo;
import org.commonjava.vertx.vabr.helper.ExecutionChainHandler;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.helper.RoutingCriteria;
import org.commonjava.vertx.vabr.types.ApplicationHeader;
import org.commonjava.vertx.vabr.types.BuiltInParam;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Query;
import org.commonjava.vertx.vabr.util.RouteHeader;
import org.commonjava.vertx.vabr.util.RouterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.CaseInsensitiveMultiMap;

public class ApplicationRouter
    implements Handler<HttpServerRequest>
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private Map<BindingKey, List<PatternRouteBinding>> routeBindings = new HashMap<>();

    private Map<BindingKey, List<PatternFilterBinding>> filterBindings = new HashMap<>();

    private final Map<String, Object> handlers = new HashMap<>();

    private Handler<HttpServerRequest> noMatchHandler;

    private String prefix;

    private String appAcceptId = "app";

    private String defaultVersion = "v1";

    private ExecutorService handlerExecutor;

    private final Map<String, String> routeAliases;

    public ApplicationRouter()
    {
        this( new ApplicationRouterConfig() );
    }

    public ApplicationRouter( final ApplicationRouterConfig config )
    {
        this.prefix = config.getPrefix();
        this.noMatchHandler = config.getNoMatchHandler();
        this.routeAliases = config.getRouteAliases();

        if ( config.getAppAcceptId() != null )
        {
            this.appAcceptId = config.getAppAcceptId();
        }
        if ( config.getDefaultVersion() != null )
        {
            this.defaultVersion = config.getDefaultVersion();
        }

        this.handlerExecutor = config.getHandlerExecutor();

        final Set<RequestHandler> h = config.getHandlers();
        final List<RouteCollection> routeCollections = config.getRouteCollections();
        final List<FilterCollection> filterCollections = config.getFilterCollections();

        bindHandlers( h );
        bindRouteCollections( routeCollections );
        bindFilterCollections( filterCollections );
    }

    public void setHandlerExecutor( final ExecutorService executor )
    {
        this.handlerExecutor = executor;
    }

    public synchronized ExecutorService getHandlerExecutor()
    {
        if ( handlerExecutor == null )
        {
            handlerExecutor = Executors.newCachedThreadPool();
        }

        return handlerExecutor;
    }

    public void bindFilters( final Iterable<?> handlers, final Iterable<FilterCollection> filterCollections )
    {
        bindHandlers( handlers );
        bindFilterCollections( filterCollections );
    }

    public void bindRoutes( final Iterable<?> handlers, final Iterable<RouteCollection> routeCollections )
    {
        bindHandlers( handlers );
        bindRouteCollections( routeCollections );
    }

    public void bindHandlers( final Iterable<?> handlers )
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
    }

    public void bindFilterCollections( final Iterable<FilterCollection> filterCollections )
    {
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

    public void bindRouteCollections( final Iterable<RouteCollection> routeCollections )
    {
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
                    request.resume()
                           .response()
                           .setStatusCode( 404 )
                           .setStatusMessage( "Not Found" )
                           .setChunked( true )
                           .end( "No handler found" );
                }
            }
        }
        catch ( final Throwable t )
        {
            logger.error( String.format( "ERROR: %s", t.getMessage() ), t );
            request.resume()
                   .response()
                   .setStatusCode( 500 )
                   .setStatusMessage( "Internal Server Error" )
                   .setChunked( true )
                   .end( "Error occurred during processing. See logs for more information." );
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

        for ( final Map.Entry<String, String> entry : routeAliases.entrySet() )
        {
            final String alias = entry.getKey();
            if ( path.startsWith( alias ) )
            {
                final String oldPath = path;
                path = Paths.get( entry.getValue(), path.substring( alias.length() ) )
                            .toString();

                logger.info( "ALIAS:\n{}\n\nwill be forwarded to:\n\n{}\n\n", oldPath, path );

                request.response()
                       .putHeader( ApplicationHeader.deprecated.key(), path );
                break;
            }
        }

        final Method method = Method.valueOf( request.method() );
        final RoutingCriteria routingCriteria = RoutingCriteria.parse( request, this.appAcceptId, this.defaultVersion );

        for ( final AcceptInfo info : routingCriteria )
        {
            final String version = info.getVersion();
            final BindingKey key = new BindingKey( method, version );

            logger.info( "REQUEST>>> {} {}\n", key, path );

            final BindingContext ctx = findBinding( key, path, routingCriteria );

            if ( ctx != null )
            {
                final RouteBinding handler = ctx.getPatternRouteBinding()
                                                .getHandler();

                if ( !info.getRawAccept()
                          .equals( RoutingCriteria.ACCEPT_ANY ) )
                {
                    request.headers()
                           .add( RouteHeader.recommended_content_type.header(), info.getRawAccept() );

                    request.headers()
                           .add( RouteHeader.base_accept.header(), info.getBaseAccept() );
                }

                request.headers()
                       .add( RouteHeader.recommended_content_version.header(), version );

                logger.info( "MATCH: {}\n", handler );
                parseParams( ctx, request );

                new ExecutionChainHandler( this, ctx, request ).execute();
                return true;
            }
        }

        return false;
    }

    protected void parseParams( final BindingContext ctx, final HttpServerRequest request )
    {
        final Matcher matcher = ctx.getMatcher();

        final MultiMap params = new CaseInsensitiveMultiMap();

        final String fullPath = request.path();
        final String uri = requestUri( request );

        final PatternRouteBinding routeBinding = ctx.getPatternRouteBinding();
        final List<String> paramNames = routeBinding.getParamNames();
        final RouteBinding handler = routeBinding.getHandler();

        final Handles pp = handler.getHandlesClass()
                                  .getAnnotation( Handles.class );

        String pathPrefix = pp.value();
        if ( pathPrefix.length() < 1 )
        {
            pathPrefix = pp.prefix();
        }

        int i = 1;

        if ( matcher.groupCount() > 0 )
        {
            final int firstIdx = matcher.start( i ) + prefix.length();
            // be defensive in case the first param is optional...
            String routeBase = ( firstIdx > 0 ) ? fullPath.substring( 0, firstIdx ) : fullPath;
            if ( routeBase.endsWith( "/" ) )
            {
                routeBase = routeBase.substring( 0, routeBase.length() - 1 );
            }

            params.add( BuiltInParam._routeBase.key(), routeBase );

            int idx = uri.indexOf( routeBase ) + routeBase.length();
            final String routeContextUrl = uri.substring( 0, idx );
            params.add( BuiltInParam._routeContextUrl.key(), routeContextUrl );

            idx = fullPath.indexOf( routeBase );

            String classBase = null;
            String classContext = null;
            if ( idx > -1 )
            {
                idx += routeBase.length();
                classBase = fullPath.substring( 0, idx );
                params.add( BuiltInParam._classBase.key(), classBase );

                idx = uri.indexOf( routeBase ) + routeBase.length();
                classContext = uri.substring( 0, idx );
                params.add( BuiltInParam._classContextUrl.key(), classContext );
            }
        }
        else
        {
            final String find = pathPrefix.length() > 0 ? pathPrefix : prefix;
            int idx = fullPath.indexOf( find );

            String classBase = null;
            String classContext = null;
            if ( idx > -1 )
            {
                idx += find.length();
                classBase = fullPath.substring( 0, idx );
                params.add( BuiltInParam._classBase.key(), classBase );

                idx = uri.indexOf( find ) + find.length();
                classContext = uri.substring( 0, idx );
                params.add( BuiltInParam._classContextUrl.key(), classContext );
            }
        }


        if ( paramNames != null )
        {
            // Named params
            for ( final String param : paramNames )
            {
                final String v = matcher.group( i );
                if ( v != null )
                {
                    logger.info( "PARAM {} = {}", param, v );
                    params.add( param, v );
                }
                i++;
            }
        }

        // Un-named params
        for ( ; i < matcher.groupCount(); i++ )
        {
            final String v = matcher.group( i );
            if ( v != null )
            {
                logger.info( "PARAM param{} = {}", i, v );
                params.add( "param" + i, v );
            }
        }

        final Query query = Query.from( request );
        for ( final String name : paramNames )
        {
            params.add( "q:" + name, query.getAll( name ) );
        }

        //        logger.info( "PARAMS: {}\n", params );
        request.params()
               .add( params );
    }

    protected BindingContext findBinding( final BindingKey key, final String path, final RoutingCriteria routingCriteria )
    {
        final List<PatternFilterBinding> allFilterBindings = this.filterBindings.get( key );

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

        logger.debug( "Searching for bindings matching key: {}", key );
        final List<PatternRouteBinding> routeBindings = this.routeBindings.get( key );
        logger.debug( "Available bindings:\n  {}\n", new Object()
        {
            @Override
            public String toString()
            {
                return StringUtils.join( routeBindings, "\n  " );
            }
        } );

        if ( routeBindings != null )
        {
            for ( final PatternRouteBinding binding : routeBindings )
            {
                final Matcher m = Pattern.compile( binding.getPattern() )
                                         .matcher( path );
                if ( m.matches() )
                {
                    final String produces = binding.getHandler()
                                                   .getContentType();

                    if ( produces != null )
                    {
                        for ( final AcceptInfo info : routingCriteria )
                        {
                            if ( info.getBaseAccept()
                                     .equals( RoutingCriteria.ACCEPT_ANY ) || info.getBaseAccept()
                                                                                  .equals( produces.toLowerCase() )
                                || info.getRawAccept()
                                       .equals( produces.toLowerCase() ) )
                            {
                                return new BindingContext( m, binding, filterBinding );
                            }
                            else
                            {
                                logger.warn( "Accept content-type: '{}' DID NOT MATCH produced content-type: '{}' for: {}. NOT A MATCH.",
                                             info.getBaseAccept(), produces, binding );
                            }
                        }
                    }
                    else
                    {
                        return new BindingContext( m, binding, filterBinding );
                    }
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

        logger.info( "Using appId: {} and default version: {}", appAcceptId, defaultVersion );
        List<String> versions = handler.getVersions();
        if ( versions == null || versions.isEmpty() )
        {
            versions = Collections.singletonList( defaultVersion );
        }

        for ( final String version : versions )
        {
            final Set<Method> methods = new HashSet<>();
            if ( method == Method.ANY )
            {
                for ( final Method m : Method.values() )
                {
                    methods.add( m );
                }
            }
            else
            {
                methods.add( method );
            }

            for ( final Method m : methods )
            {
                final BindingKey key = new BindingKey( m, version );
                List<PatternRouteBinding> b = routeBindings.get( key );
                if ( b == null )
                {
                    b = new ArrayList<>();
                    routeBindings.put( key, b );
                }

                logger.info( "ADD: {}, Pattern: {}, Route: {}\n", key, path, handler );
                addPattern( path, handler, b );
            }
        }
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

        logger.info( "Using appId: {} and default version: {}", appAcceptId, defaultVersion );
        List<String> versions = handler.getVersions();
        if ( versions == null || versions.isEmpty() )
        {
            versions = Collections.singletonList( defaultVersion );
        }

        for ( final String version : versions )
        {
            final Set<Method> methods = new HashSet<>();
            if ( method == Method.ANY )
            {
                for ( final Method m : Method.values() )
                {
                    methods.add( m );
                }
            }
            else
            {
                methods.add( method );
            }

            for ( final Method m : methods )
            {
                final BindingKey key = new BindingKey( m, version );

                logger.info( "ADD: {}, Pattern: {}, Filter: {}\n", key, path, handler );
                List<PatternFilterBinding> allFilterBindings = this.filterBindings.get( key );
                if ( allFilterBindings == null )
                {
                    allFilterBindings = new ArrayList<>();
                    this.filterBindings.put( key, allFilterBindings );
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
                    final PatternFilterBinding binding = new PatternFilterBinding( handler.getPath(), handler );
                    allFilterBindings.add( binding );
                }
            }
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

    public Map<BindingKey, List<PatternRouteBinding>> getRouteBindings()
    {
        return routeBindings;
    }

    public Map<BindingKey, List<PatternFilterBinding>> getFilterBindings()
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

    public void setRouteBindings( final Map<BindingKey, List<PatternRouteBinding>> bindings )
    {
        this.routeBindings = bindings;
    }

    public void setFilterBindings( final Map<BindingKey, List<PatternFilterBinding>> bindings )
    {
        this.filterBindings = bindings;
    }

    public void setHandlers( final Map<String, Object> handlers )
    {
        this.handlers.clear();
        this.handlers.putAll( handlers );
    }

    public String getAppAcceptId()
    {
        return appAcceptId;
    }

    public void setAppAcceptId( final String appAcceptId )
    {
        this.appAcceptId = appAcceptId;
    }

    public String getDefaultVersion()
    {
        return defaultVersion;
    }

    public void setDefaultVersion( final String defaultVersion )
    {
        this.defaultVersion = defaultVersion;
    }

    protected Logger getLogger()
    {
        return logger;
    }

}

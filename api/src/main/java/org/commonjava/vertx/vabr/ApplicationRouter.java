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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.PathPrefix;
import org.commonjava.vertx.vabr.filter.FilterBinding;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.filter.FilterHandler;
import org.commonjava.vertx.vabr.helper.BindingContext;
import org.commonjava.vertx.vabr.helper.ExecutionChainHandler;
import org.commonjava.vertx.vabr.helper.PatternFilterBinding;
import org.commonjava.vertx.vabr.helper.PatternRouteBinding;
import org.commonjava.vertx.vabr.route.RouteBinding;
import org.commonjava.vertx.vabr.route.RouteCollection;
import org.commonjava.vertx.vabr.route.RouteHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class ApplicationRouter
    implements Handler<HttpServerRequest>
{

    private static final String PATH_SEG_PATTERN = "([^\\/]+)";

    protected final Logger logger = new Logger( getClass() );

    private Map<Method, List<PatternRouteBinding>> routeBindings = new HashMap<>();

    private final Map<Method, List<PatternFilterBinding>> filterBindings = new HashMap<>();

    private Map<String, RouteHandler> routes = new HashMap<>();

    private final Map<String, FilterHandler> filters = new HashMap<>();

    private Handler<HttpServerRequest> noMatchHandler;

    private String prefix;

    public ApplicationRouter()
    {
        this.prefix = null;
    }

    public ApplicationRouter( final Iterable<RouteHandler> routes, final Iterable<RouteCollection> routeCollections )
    {
        this.prefix = null;
        bindRoutes( routes, routeCollections );
    }

    public ApplicationRouter( final String prefix )
    {
        this.prefix = prefix;
    }

    public ApplicationRouter( final String prefix, final Iterable<RouteHandler> routes, final Iterable<RouteCollection> routeCollections )
    {
        this.prefix = prefix;
        bindRoutes( routes, routeCollections );
    }

    public void bindFilters( final Iterable<FilterHandler> filters, final Iterable<FilterCollection> filterCollections )
    {
        for ( final FilterHandler filter : filters )
        {
            this.filters.put( filter.getClass()
                                    .getName(), filter );
        }

        for ( final FilterCollection fc : filterCollections )
        {
            for ( final FilterBinding fb : fc )
            {
                bind( fb );
            }
        }
    }

    public void bindRoutes( final Iterable<RouteHandler> routes, final Iterable<RouteCollection> routeCollections )
    {
        for ( final RouteHandler route : routes )
        {
            this.routes.put( route.getClass()
                                  .getName(), route );
        }

        for ( final RouteCollection rc : routeCollections )
        {
            for ( final RouteBinding rb : rc )
            {
                bind( rb );
            }
        }
    }

    public <T> T getResourceInstance( final Class<T> cls )
    {
        final RouteHandler handler = routes.get( cls.getName() );
        return handler == null ? null : cls.cast( handler );
    }

    @Override
    public void handle( final HttpServerRequest request )
    {
        try
        {
            final Method method = Method.valueOf( request.method() );

            //            logger.info( "REQUEST>>> %s %s\n", method, request.path() );

            String path = request.path();
            if ( prefix == null || path.startsWith( prefix ) )
            {
                if ( prefix != null )
                {
                    path = path.substring( prefix.length() - 1 );
                }
            }

            BindingContext ctx = findBinding( method, request.path() );

            if ( ctx == null )
            {
                ctx = findBinding( Method.ANY, request.path() );
            }

            if ( ctx != null )
            {
                final RouteBinding handler = ctx.getRouteBinding()
                                                .getHandler();
                // FIXME Wrap this in an executor that knows about filters AND the fundamental route...
                logger.info( "MATCH: %s\n", handler );
                parseParams( ctx, request );

                new ExecutionChainHandler( this, ctx, request ).execute();
                return;
            }

            if ( noMatchHandler != null )
            {
                noMatchHandler.handle( request );
            }
            else
            {
                // Default 404
                request.response()
                       .setStatusCode( 404 )
                       .setStatusMessage( "No handler found" )
                       .end();
            }
        }
        catch ( final Throwable t )
        {
            logger.info( "ERROR: %s", t.getMessage() );
            t.printStackTrace();
            request.response()
                   .setStatusCode( 500 )
                   .setStatusMessage( "Error occurred during processing. See logs for more information." )
                   .end();
        }
    }

    protected void parseParams( final BindingContext ctx, final HttpServerRequest request )
    {
        final Matcher matcher = ctx.getMatcher();

        final Map<String, String> params = new HashMap<>( matcher.groupCount() );

        final String fullPath = request.path();

        final PatternRouteBinding routeBinding = ctx.getRouteBinding();
        final List<String> paramNames = routeBinding.getParamNames();
        final RouteBinding handler = routeBinding.getHandler();

        final PathPrefix pp = handler.getMethod()
                                     .getDeclaringClass()
                                     .getAnnotation( PathPrefix.class );
        if ( pp != null )
        {
            final String pathPrefix = pp.value();
            int idx = fullPath.indexOf( pathPrefix );
            if ( idx > -1 )
            {
                idx += pathPrefix.length();
                final String prefix = fullPath.substring( 0, idx );
                params.put( BuiltInParam._classBase.key(), prefix );
            }
        }

        if ( paramNames != null )
        {
            // Named params
            int i = 1;

            if ( !paramNames.isEmpty() )
            {
                // We should absolutely be able to figure this out without being defensive.
                final int firstIdx = matcher.start( i );
                String basePath = fullPath.substring( 0, firstIdx );

                if ( basePath.endsWith( "/" ) )
                {
                    basePath = basePath.substring( 0, basePath.length() - 1 );
                }

                params.put( BuiltInParam._routeBase.key(), basePath );
            }

            for ( final String param : paramNames )
            {
                final String v = matcher.group( i );
                if ( v != null )
                {
                    logger.info( "PARAM %s = %s", param, v );
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
                    logger.info( "PARAM param%s = %s", i, v );
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

        //        logger.info( "PARAMS: %s\n", params );
        request.params()
               .set( params );
    }

    protected BindingContext findBinding( final Method method, final String path )
    {
        final List<PatternFilterBinding> allFilterBindings = this.filterBindings.get( method );

        PatternFilterBinding filterBinding = null;
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

        final List<PatternRouteBinding> routeBindings = this.routeBindings.get( method );
        //        logger.info( "Available bindings:\n  %s\n", join( bindings, "\n  " ) );
        if ( routeBindings != null )
        {
            for ( final PatternRouteBinding binding : routeBindings )
            {
                final Matcher m = binding.getPattern()
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

        logger.info( "ADD Method: %s, Pattern: %s, Route: %s\n", method, path, handler );
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

        logger.info( "ADD Method: %s, Pattern: %s, Filter: %s\n", method, path, handler );
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
        // input is /:name/:path=(.+)/:page
        // route pattern is: /([^\\/]+)/(.+)/([^\\/]+)
        // group list is: [name, path, page], where index+1 == regex-group-number

        // We need to search for any :<token name> tokens in the String and replace them with named capture groups
        final Matcher m = Pattern.compile( ":(\\??[A-Za-z][A-Za-z0-9_]*)(=\\([^)]+\\))?" )
                                 .matcher( input );
        final StringBuffer sb = new StringBuffer();
        final List<String> groups = new ArrayList<>();
        while ( m.find() )
        {
            String group = m.group( 1 );
            boolean optional = false;
            if ( group.startsWith( "?" ) )
            {
                group = group.substring( 1 );
                optional = true;
            }

            String pattern = m.group( 2 );
            if ( pattern == null )
            {
                pattern = PATH_SEG_PATTERN;
            }
            else
            {
                pattern = pattern.substring( 1 );
            }

            if ( optional )
            {
                pattern += "?";
            }

            if ( groups.contains( group ) )
            {
                throw new IllegalArgumentException( "Cannot use identifier " + group + " more than once in pattern string" );
            }

            m.appendReplacement( sb, pattern );

            groups.add( group );
        }
        m.appendTail( sb );
        final String regex = sb.toString();

        //        logger.info( "BIND regex: %s, groups: %s, route: %s\n", regex, groups, handler );

        final PatternRouteBinding binding = new PatternRouteBinding( Pattern.compile( regex ), groups, handler );
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

    public Map<String, RouteHandler> getRoutes()
    {
        return routes;
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

    protected void setRoutes( final Map<String, RouteHandler> routes )
    {
        this.routes = routes;
    }

}

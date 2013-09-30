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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.util.logging.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class ApplicationRouter
    implements Handler<HttpServerRequest>
{

    static class BindingContext
    {
        private final Matcher matcher;

        private final PatternBinding binding;

        private BindingContext( final Matcher matcher, final PatternBinding binding )
        {
            this.matcher = matcher;
            this.binding = binding;
        }
    }

    private static final String PATH_SEG_PATTERN = "([^\\/]+)";

    private final Logger logger = new Logger( getClass() );

    private final Map<Method, List<PatternBinding>> bindings = new HashMap<>();

    private final Map<String, RouteHandler> routes = new HashMap<>();

    private Handler<HttpServerRequest> noMatchHandler;

    public ApplicationRouter()
    {
    }

    public ApplicationRouter( final Collection<RouteHandler> routes, final Iterable<RouteCollection> routeCollections )
    {
        bind( routes, routeCollections );
    }

    private void bind( final Iterable<RouteHandler> routes, final Iterable<RouteCollection> routeCollections )
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

            final BindingContext ctx = findBinding( method, request.path() );
            if ( ctx != null )
            {
                logger.info( "MATCH: %s\n", ctx.binding.handler );
                parseParams( ctx, request );

                ctx.binding.handler.handle( this, request );
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
        final Map<String, String> params = new HashMap<>( ctx.matcher.groupCount() );
        if ( ctx.binding.paramNames != null )
        {
            // Named params
            int i = 1;
            for ( final String param : ctx.binding.paramNames )
            {
                final String v = ctx.matcher.group( i );
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
            for ( int i = 0; i < ctx.matcher.groupCount(); i++ )
            {
                final String v = ctx.matcher.group( i + 1 );
                if ( v != null )
                {
                    logger.info( "PARAM param%s = %s", i, v );
                    params.put( "param" + i, v );
                }
            }
        }

        //        logger.info( "PARAMS: %s\n", params );
        request.params()
               .set( params );
    }

    protected BindingContext findBinding( final Method method, final String path )
    {
        final List<PatternBinding> bindings = this.bindings.get( method );
        //        logger.info( "Available bindings:\n  %s\n", join( bindings, "\n  " ) );
        if ( bindings != null )
        {
            for ( final PatternBinding binding : bindings )
            {
                final Matcher m = binding.pattern.matcher( path );
                if ( m.matches() )
                {
                    return new BindingContext( m, binding );
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

        // FIXME: Sort these to push the most specific paths to the top.
        List<PatternBinding> b = bindings.get( method );
        if ( b == null )
        {
            b = new ArrayList<>();
            bindings.put( method, b );
        }

        logger.info( "ADD Method: %s, Pattern: %s, Route: %s\n", method, path, handler );
        addPattern( path, handler, b );
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

    private void addPattern( final String input, final RouteBinding handler, final List<PatternBinding> bindings )
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

        final PatternBinding binding = new PatternBinding( Pattern.compile( regex ), groups, handler );
        bindings.add( binding );
    }

    private static class PatternBinding
    {
        final Pattern pattern;

        final RouteBinding handler;

        final List<String> paramNames;

        private PatternBinding( final Pattern pattern, final List<String> paramNames, final RouteBinding handler )
        {
            this.pattern = pattern;
            this.paramNames = paramNames;
            this.handler = handler;
        }

        @Override
        public String toString()
        {
            return String.format( "Binding [pattern: %s, params: %s, handler: %s]", pattern, paramNames, handler );
        }
    }
}

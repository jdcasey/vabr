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
package org.commonjava.vertx.vabr.bind;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.vertx.vabr.bind.route.RouteBinding;

public class PatternRouteBinding
    implements Comparable<PatternRouteBinding>
{
    private static final String PATH_SEG_PATTERN = "([^\\/]+)";

    private final String pattern;

    private final RouteBinding handler;

    private final List<String> paramNames;

    public PatternRouteBinding( final String pattern, final List<String> paramNames, final RouteBinding handler )
    {
        this.pattern = pattern;
        this.paramNames = paramNames;
        this.handler = handler;
    }

    public static PatternRouteBinding parse( String route, RouteBinding handler )
    {
        // input is /:name/:path=(.+)/:page
        // route pattern is: /([^\\/]+)/(.+)/([^\\/]+)
        // group list is: [name, path, page], where index+1 == regex-group-number

        // We need to search for any :<token name> tokens in the String and replace them with named capture groups
        final Matcher m = Pattern.compile( ":(\\??[A-Za-z][A-Za-z0-9_]*)(=\\([^)]+\\))?" )
                                 .matcher( route );

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
                throw new IllegalArgumentException( "Cannot use identifier " + group
                    + " more than once in pattern string" );
            }

            m.appendReplacement( sb, pattern );

            groups.add( group );
        }
        m.appendTail( sb );
        final String regex = sb.toString();

        return new PatternRouteBinding( regex, groups, handler );
    }

    @Override
    public String toString()
    {
        return String.format( "Route Binding [pattern: %s, params: %s, handler: %s]", pattern, paramNames, handler );
    }

    @Override
    public int compareTo( final PatternRouteBinding other )
    {
        // this is intentionally backward, since higher priority should sort FIRST.
        return new Integer( other.handler.getPriority() ).compareTo( handler.getPriority() );
    }

    public String getPattern()
    {
        return pattern;
    }

    public RouteBinding getHandler()
    {
        return handler;
    }

    public List<String> getParamNames()
    {
        return paramNames;
    }
}

package org.commonjava.vertx.vabr.helper;

import java.util.List;
import java.util.regex.Pattern;

import org.commonjava.vertx.vabr.route.RouteBinding;

public class PatternRouteBinding
    implements Comparable<PatternRouteBinding>
{
    private final Pattern pattern;

    private final RouteBinding handler;

    private final List<String> paramNames;

    public PatternRouteBinding( final Pattern pattern, final List<String> paramNames, final RouteBinding handler )
    {
        this.pattern = pattern;
        this.paramNames = paramNames;
        this.handler = handler;
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

    public Pattern getPattern()
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

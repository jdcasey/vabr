package org.commonjava.vertx.vabr.helper;

import java.util.regex.Matcher;

public class BindingContext
{
    private final Matcher matcher;

    private final PatternRouteBinding routeBinding;

    private final PatternFilterBinding filterBinding;

    public BindingContext( final Matcher matcher, final PatternRouteBinding routeBinding, final PatternFilterBinding filterBinding )
    {
        this.matcher = matcher;
        this.routeBinding = routeBinding;
        this.filterBinding = filterBinding;
    }

    public Matcher getMatcher()
    {
        return matcher;
    }

    public PatternRouteBinding getRouteBinding()
    {
        return routeBinding;
    }

    public PatternFilterBinding getFilterBinding()
    {
        return filterBinding;
    }
}

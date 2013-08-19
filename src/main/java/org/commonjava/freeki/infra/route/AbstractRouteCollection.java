package org.commonjava.freeki.infra.route;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractRouteCollection
    implements RouteCollection
{

    private final Set<RouteBinding> routes = new HashSet<>();

    protected void bind( final RouteBinding route )
    {
        routes.add( route );
    }

    @Override
    public final Set<RouteBinding> getRoutes()
    {
        return routes;
    }

    @Override
    public Iterator<RouteBinding> iterator()
    {
        return routes.iterator();
    }

}

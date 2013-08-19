package org.commonjava.freeki.infra.route;

import java.util.Set;


public interface RouteCollection
    extends Iterable<RouteBinding>
{

    Set<RouteBinding> getRoutes();

}

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
package org.commonjava.vertx.vabr.route;

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

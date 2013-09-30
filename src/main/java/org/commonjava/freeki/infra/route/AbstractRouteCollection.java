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

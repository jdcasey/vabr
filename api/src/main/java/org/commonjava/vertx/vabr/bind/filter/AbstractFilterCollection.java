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
package org.commonjava.vertx.vabr.bind.filter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractFilterCollection
    implements FilterCollection
{

    private final Set<FilterBinding> filters = new HashSet<>();

    protected void bind( final FilterBinding filter )
    {
        filters.add( filter );
    }

    @Override
    public final Set<FilterBinding> getFilters()
    {
        return filters;
    }

    @Override
    public Iterator<FilterBinding> iterator()
    {
        return filters.iterator();
    }

}

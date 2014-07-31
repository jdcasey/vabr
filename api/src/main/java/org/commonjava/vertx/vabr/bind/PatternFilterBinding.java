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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.commonjava.vertx.vabr.bind.filter.FilterBinding;

public class PatternFilterBinding
{
    private final String regex;

    private final List<FilterBinding> filters = new ArrayList<>();

    public PatternFilterBinding( final String regex, final FilterBinding filter )
    {
        this.regex = regex;
        filters.add( filter );
    }

    public void addFilter( final FilterBinding filter )
    {
        if ( !filters.contains( filter ) )
        {
            filters.add( filter );
            Collections.sort( filters );
        }
    }

    public Pattern getPattern()
    {
        return Pattern.compile( regex );
    }

    public List<FilterBinding> getFilters()
    {
        return filters;
    }

    @Override
    public String toString()
    {
        return String.format( "Filter Binding [pattern: %s, filters: %s]", regex, filters );
    }
}

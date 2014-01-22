package org.commonjava.vertx.vabr.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.commonjava.vertx.vabr.filter.FilterBinding;

public class PatternFilterBinding
{
    private final Pattern pattern;

    private final List<FilterBinding> filters = new ArrayList<>();

    public PatternFilterBinding( final Pattern pattern, final FilterBinding filter )
    {
        this.pattern = pattern;
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
        return pattern;
    }

    public List<FilterBinding> getFilters()
    {
        return filters;
    }

    @Override
    public String toString()
    {
        return String.format( "Filter Binding [pattern: %s, filters: %s]", pattern, filters );
    }
}

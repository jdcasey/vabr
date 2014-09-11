package org.commonjava.vertx.vabr.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.CaseInsensitiveMultiMap;

public final class Query
{

    private static final String DATE_FORMAT = "yyyy-MM-dd+hh-mm-ss";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final MultiMap map;

    private Query( final MultiMap map )
    {
        this.map = map;
    }

    public static Query from( final HttpServerRequest request )
    {
        final MultiMap map = new CaseInsensitiveMultiMap();

        final String query = request.query();
        if ( query != null )
        {
            final String[] qe = query.split( "&" );
            for ( final String entry : qe )
            {
                final int idx = entry.indexOf( '=' );
                if ( idx > 1 )
                {
                    map.add( entry.substring( 0, idx ), entry.substring( idx + 1 ) );
                }
                else
                {
                    map.add( entry, "true" );
                }
            }
        }

        return new Query( map );
    }

    public String get( final String name )
    {
        return map.get( name );
    }

    public List<String> getAll( final String name )
    {
        return map.getAll( name );
    }

    public List<Entry<String, String>> entries()
    {
        return map.entries();
    }

    public boolean contains( final String name )
    {
        return map.contains( name );
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public Iterator<Entry<String, String>> iterator()
    {
        return map.iterator();
    }

    public Set<String> names()
    {
        return map.names();
    }

    public int size()
    {
        return map.size();
    }

    public Integer getInt( final String name )
    {
        final String value = get( name );
        return value == null ? null : Integer.valueOf( value );
    }

    public Integer getInt( final String name, final int defaultValue )
    {
        final String value = get( name );
        try
        {
            return value == null ? defaultValue : Integer.valueOf( value );
        }
        catch ( final NumberFormatException e )
        {
            logger.debug( String.format( "Failed to convert query parameter: %s to integer. Reason: %s", value,
                                         e.getMessage() ), e );
            return defaultValue;
        }
    }

    public Long getLong( final String name )
    {
        final String value = get( name );
        return value == null ? null : Long.valueOf( value );
    }

    public Long getLong( final String name, final long defaultValue )
    {
        final String value = get( name );
        try
        {
            return value == null ? defaultValue : Long.valueOf( value );
        }
        catch ( final NumberFormatException e )
        {
            logger.debug( String.format( "Failed to convert query parameter: %s to long. Reason: %s", value,
                                         e.getMessage() ), e );
            return defaultValue;
        }
    }

    public Boolean getBoolean( final String name )
    {
        final String value = get( name );
        return value == null ? null : Boolean.valueOf( value );
    }

    public Boolean getBoolean( final String name, final boolean defaultValue )
    {
        final String value = get( name );
        try
        {
            return value == null ? defaultValue : Boolean.valueOf( value );
        }
        catch ( final NumberFormatException e )
        {
            logger.debug( String.format( "Failed to convert query parameter: %s to integer. Reason: %s", value,
                                         e.getMessage() ), e );
            return defaultValue;
        }
    }

    public Date getDate( final String name )
        throws ParseException
    {
        return getDate( name, DATE_FORMAT );
    }

    public Date getDate( final String name, final String format )
        throws ParseException
    {
        final String value = get( name );
        return value == null ? null : new SimpleDateFormat( format ).parse( value );
    }

    public Date getDate( final String name, final Date defaultValue )
    {
        return getDate( name, DATE_FORMAT, defaultValue );
    }

    public Date getDate( final String name, final String format, final Date defaultValue )
    {
        final String value = get( name );
        try
        {
            return value == null ? defaultValue : new SimpleDateFormat( format ).parse( value );
        }
        catch ( final ParseException e )
        {
            logger.debug( String.format( "Failed to convert query parameter: %s to integer. Reason: %s", value,
                                         e.getMessage() ), e );
            return defaultValue;
        }
    }

    public <T extends Enum<T>> T getEnum( final String name, final Class<T> enumType )
    {
        final String value = get( name );
        return value == null ? null : Enum.valueOf( enumType, value );
    }

    public <T extends Enum<T>> T getEnum( final String name, final Class<T> enumType, final T defaultValue )
    {
        final String value = get( name );
        return value == null ? defaultValue : Enum.valueOf( enumType, value );
    }

}

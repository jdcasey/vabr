package org.commonjava.vertx.vabr.util;

import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.Handles;

public final class AnnotationUtils
{

    private AnnotationUtils()
    {
    }

    public static String getHandlerKey( final Class<?> cls )
    {
        final Logger logger = new Logger( AnnotationUtils.class );

        Class<?> c = cls;
        Handles anno = null;
        do
        {
            logger.info( "Searching %s for @Handles annotation...", c.getName() );
            anno = c.getAnnotation( Handles.class );
            if ( anno == null )
            {
                c = cls.getSuperclass();
            }
        }
        while ( anno == null && c != null );

        if ( anno == null )
        {
            throw new IllegalArgumentException( "Handler classes MUST declare @" + Handles.class.getSimpleName() + " with a unique key!" );
        }

        final String key = getHandlerKey( anno, c.getName() );
        logger.info( "Got: %s", key );
        return key;
    }

    public static String getHandlerKey( final Handles handles, final String className )
    {
        String key = handles.key();
        if ( key == null || key.length() < 1 )
        {
            key = handles.value();
        }

        if ( key == null || key.length() < 1 )
        {
            key = handles.prefix();
        }

        if ( key == null || key.length() < 1 )
        {
            throw new IllegalArgumentException( "You must either specify key or default value string for @Handles (at the class level) in "
                + className );
        }

        return key;
    }

    public static String pathOf( final Handles handles, final String path, final String value )
    {
        final StringBuilder sb = new StringBuilder();
        if ( handles != null )
        {
            String pfx = handles.value();
            if ( pfx.length() < 1 )
            {
                pfx = handles.prefix();
            }

            if ( pfx.endsWith( "/" ) )
            {
                pfx = pfx.substring( 0, pfx.length() - 1 );
            }

            sb.append( pfx );
        }

        String suff = null;
        if ( path != null && path.trim()
                                 .length() > 0 )
        {
            suff = path;
        }
        else if ( value != null && value.trim()
                                        .length() > 0 )
        {
            suff = value;
        }

        if ( suff != null )
        {
            sb.append( suff );
        }

        if ( sb.length() < 1 )
        {
            sb.append( "/" );
        }

        return sb.toString();
    }

}

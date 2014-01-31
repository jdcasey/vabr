package org.commonjava.vertx.vabr.util;

import org.commonjava.util.logging.Logger;

public final class RouterUtils
{

    private RouterUtils()
    {
    }

    public static String trimPrefix( final String prefix, String path )
    {
        if ( prefix != null )
        {
            if ( !path.startsWith( prefix ) )
            {
                return null;
            }
            else
            {
                new Logger( RouterUtils.class ).info( "Trimming off: '%s'", path.substring( 0, prefix.length() ) );
                path = path.substring( prefix.length() );
            }
        }

        if ( path.length() < 1 )
        {
            path = "/";
        }

        return path;
    }

}

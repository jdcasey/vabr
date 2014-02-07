package org.commonjava.vertx.vabr.util;

import org.commonjava.util.logging.Logger;
import org.vertx.java.core.http.HttpServerRequest;

public final class RouterUtils
{

    private RouterUtils()
    {
    }

    public static String requestUri( final HttpServerRequest request )
    {
        final String hostHeader = request.headers()
                                         .get( "Host" );

        String hostAndPort = request.absoluteURI()
                                    .getHost();

        final int port = request.absoluteURI()
                                .getPort();
        if ( port != 80 && port != 443 )
        {
            hostAndPort += ":" + port;
        }

        final String uri = request.absoluteURI()
                                  .toString();

        final int idx = uri.indexOf( hostAndPort );

        final StringBuilder sb = new StringBuilder();
        sb.append( uri.substring( 0, idx ) );
        sb.append( hostHeader );
        sb.append( uri.substring( idx + hostAndPort.length() ) );

        return sb.toString();
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

        //        if ( path.length() < 1 )
        //        {
        //            path = "/";
        //        }

        return path;
    }

}

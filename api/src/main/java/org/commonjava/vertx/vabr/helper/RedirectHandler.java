package org.commonjava.vertx.vabr.helper;

import org.commonjava.vertx.vabr.util.RouterUtils;
import org.vertx.java.core.http.HttpServerRequest;

public class RedirectHandler
    implements NoMatchHandler
{

    private final String to;

    public RedirectHandler( final String to )
    {
        if ( to == null )
        {
            this.to = "/";
        }
        else if ( to.endsWith( "/" ) )
        {
            this.to = to;
        }
        else
        {
            this.to = to + "/";
        }
    }

    @Override
    public void handle( final HttpServerRequest request )
    {
        if ( request.path()
                    .endsWith( to ) )
        {
            request.resume()
                   .response()
                   .setStatusCode( 404 )
                   .setStatusMessage( "Not Found" )
                   .end();
            return;
        }

        final int code = 301;
        final String message = "Moved Permanently";

        String uri = RouterUtils.requestUri( request );
        if ( uri.endsWith( "/" ) && to.startsWith( "/" ) )
        {
            if ( to.length() > 1 )
            {
                uri += to.substring( 1 );
            }
        }
        else
        {
            uri += to;
        }

        request.resume()
               .response()
               .setStatusCode( code )
               .setStatusMessage( message )
               .putHeader( "URI", uri )
               .putHeader( "Location", uri )
               .end();
    }

}

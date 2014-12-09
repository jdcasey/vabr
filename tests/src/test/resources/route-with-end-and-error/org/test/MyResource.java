package org.test;

import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.http.HttpServerRequest;

@Handles("/my")
public class MyResource
    implements RequestHandler
{
    
    @Route( "/name" )
    public void getName( final HttpServerRequest request )
    {
        try
        {
            throw new NullPointerException( "This is the error" );
        }
        finally
        {
            request.response().setStatusCode( 500 ).setStatusMessage( "Server Error" ).end();
        }
    }

}
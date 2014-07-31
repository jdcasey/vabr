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
        request.response().setStatusCode( 200 ).setStatusMessage( "Ok" ).end("Hector");
    }

}
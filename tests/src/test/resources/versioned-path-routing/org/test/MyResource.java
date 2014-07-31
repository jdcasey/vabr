package org.test;

import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.http.HttpServerRequest;

@Handles("/my")
public class MyResource
    implements RequestHandler
{
    
    @Route( path="/name", versions="v1" )
    public void getV1Name( final HttpServerRequest request )
    {
        System.out.println( "Executing version 1");
        request.response().putHeader( "Content-Type", "application/app-v1+plain" ).setStatusCode( 200 ).setStatusMessage( "Ok" ).end("Hector");
    }

    @Route( path="/name", versions="v2" )
    public void getV2Name( final HttpServerRequest request )
    {
        System.out.println( "Executing version 2");
        request.response().putHeader( "Content-Type", "application/app-v2+plain" ).setStatusCode( 200 ).setStatusMessage( "Ok" ).end("Ralph");
    }

}
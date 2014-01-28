package org.commonjava.vertx.vabr.util;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerResponse;

public class TrackingResponse
    implements HttpServerResponse
{

    private HttpServerResponse response;

    private boolean ended;

    public TrackingResponse( final HttpServerResponse response )
    {
        this.response = response;
    }

    @Override
    public HttpServerResponse exceptionHandler( final Handler<Throwable> handler )
    {
        response = response.exceptionHandler( handler );
        return this;
    }

    @Override
    public HttpServerResponse setWriteQueueMaxSize( final int maxSize )
    {
        response = response.setWriteQueueMaxSize( maxSize );
        return this;
    }

    @Override
    public boolean writeQueueFull()
    {
        return response.writeQueueFull();
    }

    @Override
    public int getStatusCode()
    {
        return response.getStatusCode();
    }

    @Override
    public HttpServerResponse drainHandler( final Handler<Void> handler )
    {
        response = response.drainHandler( handler );
        return this;
    }

    @Override
    public HttpServerResponse setStatusCode( final int statusCode )
    {
        response = response.setStatusCode( statusCode );
        return this;
    }

    @Override
    public String getStatusMessage()
    {
        return response.getStatusMessage();
    }

    @Override
    public HttpServerResponse setStatusMessage( final String statusMessage )
    {
        response = response.setStatusMessage( statusMessage );
        return this;
    }

    @Override
    public HttpServerResponse setChunked( final boolean chunked )
    {
        response = response.setChunked( chunked );
        return this;
    }

    @Override
    public boolean isChunked()
    {
        return response.isChunked();
    }

    @Override
    public MultiMap headers()
    {
        return response.headers();
    }

    @Override
    public HttpServerResponse putHeader( final String name, final String value )
    {
        response = response.putHeader( name, value );
        return this;
    }

    @Override
    public HttpServerResponse putHeader( final String name, final Iterable<String> values )
    {
        response = response.putHeader( name, values );
        return this;
    }

    @Override
    public MultiMap trailers()
    {
        return response.trailers();
    }

    @Override
    public HttpServerResponse putTrailer( final String name, final String value )
    {
        response = response.putTrailer( name, value );
        return this;
    }

    @Override
    public HttpServerResponse putTrailer( final String name, final Iterable<String> values )
    {
        response = response.putTrailer( name, values );
        return this;
    }

    @Override
    public HttpServerResponse closeHandler( final Handler<Void> handler )
    {
        response = response.closeHandler( handler );
        return this;
    }

    @Override
    public HttpServerResponse write( final Buffer chunk )
    {
        response = response.write( chunk );
        ended = true;
        return this;
    }

    @Override
    public HttpServerResponse write( final String chunk, final String enc )
    {
        response = response.write( chunk, enc );
        ended = true;
        return this;
    }

    @Override
    public HttpServerResponse write( final String chunk )
    {
        response = response.write( chunk );
        ended = true;
        return this;
    }

    @Override
    public void end( final String chunk )
    {
        response.end( chunk );
        ended = true;
    }

    @Override
    public void end( final String chunk, final String enc )
    {
        response.end( chunk, enc );
        ended = true;
    }

    @Override
    public void end( final Buffer chunk )
    {
        response.end( chunk );
        ended = true;
    }

    @Override
    public void end()
    {
        response.end();
        ended = true;
    }

    @Override
    public HttpServerResponse sendFile( final String filename )
    {
        response = response.sendFile( filename );
        ended = true;
        return this;
    }

    @Override
    public HttpServerResponse sendFile( final String filename, final String notFoundFile )
    {
        response = response.sendFile( filename, notFoundFile );
        ended = true;
        return this;
    }

    @Override
    public void close()
    {
        response.close();
    }

    public boolean isEnded()
    {
        return ended;
    }

}

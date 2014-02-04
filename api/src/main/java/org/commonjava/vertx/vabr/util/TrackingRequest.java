package org.commonjava.vertx.vabr.util;

import java.net.InetSocketAddress;
import java.net.URI;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.net.NetSocket;

public class TrackingRequest
    implements HttpServerRequest
{

    private HttpServerRequest request;

    private TrackingResponse response;

    public TrackingRequest( final HttpServerRequest request )
    {
        this.request = request;
    }

    @Override
    public HttpServerRequest exceptionHandler( final Handler<Throwable> handler )
    {
        request = request.exceptionHandler( handler );
        return this;
    }

    @Override
    public HttpServerRequest dataHandler( final Handler<Buffer> handler )
    {
        request = request.dataHandler( handler );
        return this;
    }

    @Override
    public HttpServerRequest pause()
    {
        request = request.pause();
        return this;
    }

    @Override
    public HttpServerRequest resume()
    {
        request = request.resume();
        return this;
    }

    @Override
    public HttpServerRequest endHandler( final Handler<Void> endHandler )
    {
        request = request.endHandler( endHandler );
        return this;
    }

    @Override
    public HttpVersion version()
    {
        return request.version();
    }

    @Override
    public String method()
    {
        return request.method();
    }

    @Override
    public String uri()
    {
        return request.uri();
    }

    @Override
    public String path()
    {
        return request.path();
    }

    @Override
    public String query()
    {
        return request.query();
    }

    @Override
    public synchronized HttpServerResponse response()
    {
        if ( response == null )
        {
            response = new TrackingResponse( request.response() );
        }

        return response;
    }

    @Override
    public MultiMap headers()
    {
        return request.headers();
    }

    @Override
    public MultiMap params()
    {
        return request.params();
    }

    @Override
    public InetSocketAddress remoteAddress()
    {
        return request.remoteAddress();
    }

    @Override
    public X509Certificate[] peerCertificateChain()
        throws SSLPeerUnverifiedException
    {
        return request.peerCertificateChain();
    }

    @Override
    public URI absoluteURI()
    {
        return request.absoluteURI();
    }

    @Override
    public HttpServerRequest bodyHandler( final Handler<Buffer> bodyHandler )
    {
        request = request.bodyHandler( bodyHandler );
        return this;
    }

    @Override
    public NetSocket netSocket()
    {
        return request.netSocket();
    }

    @Override
    public HttpServerRequest expectMultiPart( final boolean expect )
    {
        request = request.expectMultiPart( expect );
        return this;
    }

    @Override
    public HttpServerRequest uploadHandler( final Handler<HttpServerFileUpload> uploadHandler )
    {
        request = request.uploadHandler( uploadHandler );
        return this;
    }

    @Override
    public MultiMap formAttributes()
    {
        return request.formAttributes();
    }

    public TrackingResponse trackingResponse()
    {
        return (TrackingResponse) response();
    }

}

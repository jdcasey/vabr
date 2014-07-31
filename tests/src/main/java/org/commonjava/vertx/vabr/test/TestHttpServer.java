/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.vertx.vabr.test;

import static org.commonjava.vertx.vabr.test.PathUtils.normalize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

public class TestHttpServer
    extends ExternalResource
    implements Handler<HttpServerRequest>
{

    public class TestWrapperHandler
        implements Handler<HttpServerRequest>
    {

        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private final Handler<HttpServerRequest> handler;

        public TestWrapperHandler( final Handler<HttpServerRequest> handler )
        {
            this.handler = handler;
        }

        @Override
        public void handle( final HttpServerRequest event )
        {
            event.pause();
            logger.debug( "Handing off: " + event.uri() + " to: " + handler );
            event.resume();
            handler.handle( event );
            logger.debug( "Finished: " + event.uri() );
        }

    }

    private static final int TRIES = 4;

    private static Random rand = new Random();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final int port;

    private Vertx vertx;

    private final String baseResource;

    private final Map<String, Expectation> expectations = new HashMap<String, Expectation>();

    private final Map<String, Integer> accessesByPath = new HashMap<String, Integer>();

    private final Map<String, String> errors = new HashMap<String, String>();

    private Handler<HttpServerRequest> handler;

    private boolean started;

    public TestHttpServer()
    {
        this.baseResource = null;
        this.port = findPort();
    }

    public TestHttpServer( final String baseResource )
    {
        this.baseResource = baseResource;
        this.handler = this;
        this.port = findPort();
    }

    private int findPort()
    {
        int port = -1;
        ServerSocket ss = null;
        for ( int i = 0; i < TRIES; i++ )
        {
            final int p = Math.abs( rand.nextInt() ) % 2000 + 8000;
            logger.info( "Trying port: {}", p );
            try
            {
                ss = new ServerSocket( p );
                port = p;
                break;
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Port %s failed. Reason: %s", p, e.getMessage() ), e );
            }
            finally
            {
                IOUtils.closeQuietly( ss );
            }
        }

        if ( port < 8000 )
        {
            throw new RuntimeException( "Failed to start test HTTP server. Cannot find open port in " + TRIES
                + " tries." );
        }
        return port;
    }

    public void setHandlerAndStart( final Handler<HttpServerRequest> handler )
    {
        this.handler = handler;
        stop();
        start();
    }

    public void clearHandler()
    {
        this.handler = this;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public void after()
    {
        logger.debug( "Shutdown!" );
        stop();
    }

    public void stop()
    {
        logger.debug( "Trying to stop vertx: " + vertx + " on: " + this );
        if ( vertx != null )
        {
            vertx.stop();
            vertx = null;
        }

        this.started = false;
    }

    @Override
    public void before()
        throws Exception
    {
        start();
    }

    public void start()
    {
        if ( handler != null )
        {
            this.vertx = new DefaultVertx();
            final HttpServer httpserver = vertx.createHttpServer()
                                               .requestHandler( new TestWrapperHandler( handler ) );

            logger.debug( "Starting vertx http server using: " + vertx + " with handler: " + handler + " on: "
                + port + " (test-server: " + this + ")" );

            final Handler<AsyncResult<HttpServer>> listener = new Handler<AsyncResult<HttpServer>>()
            {
                @Override
                public void handle( final AsyncResult<HttpServer> event )
                {
                    if ( event.succeeded() )
                    {
                        final HttpServer server = event.result();
                        logger.debug( "Server: " + server + " ready." );
                        synchronized ( this )
                        {
                            notifyAll();
                        }
                    }
                }
            };

            new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    httpserver.listen( port, "localhost", listener );
                }

            } ).start();

            synchronized ( listener )
            {
                try
                {
                    listener.wait();
                }
                catch ( final InterruptedException e )
                {
                    e.printStackTrace();
                    Thread.currentThread()
                          .interrupt();
                }
            }

            logger.info( "Listening on: " + port );

            this.started = true;
        }
    }

    public boolean isStarted()
    {
        return started;
    }

    public Vertx getVertx()
    {
        return vertx;
    }

    public String formatUrl( final String... subpath )
    {
        if ( baseResource != null )
        {
            return String.format( "http://localhost:%s/%s/%s", port, baseResource, normalize( subpath ) );
        }

        return String.format( "http://localhost:%s%s", port, normalize( subpath ) );
    }

    public String getBaseUri()
    {
        return String.format( "http://localhost:%s/%s", port, baseResource );
    }

    public String getUrlPath( final String url )
        throws MalformedURLException
    {
        return new URL( url ).getPath();
    }

    public Map<String, Integer> getAccessesByPath()
    {
        return accessesByPath;
    }

    public Map<String, String> getRegisteredErrors()
    {
        return errors;
    }

    @Override
    public void handle( final HttpServerRequest req )
    {
        final boolean ended = false;
        try
        {
            final String wholePath = req.path();
            String path = wholePath;
            if ( path.length() > 1 )
            {
                path = path.substring( 1 );
            }

            final Integer i = accessesByPath.get( wholePath );
            if ( i == null )
            {
                accessesByPath.put( wholePath, 1 );
            }
            else
            {
                accessesByPath.put( wholePath, i + 1 );
            }

            if ( errors.containsKey( wholePath ) )
            {
                final String error = errors.get( wholePath );
                logger.error( "Returning registered error: {}", error );
                req.response()
                   .setStatusCode( 500 )
                   .setStatusMessage( error );

                return;
            }

            logger.info( "Looking for expectation: '{}'", wholePath );
            final Expectation expectation = expectations.get( wholePath );
            if ( expectation != null )
            {
                logger.info( "Responding via registered expectation: {}", expectation );

                req.response()
                   .setStatusCode( expectation.code() )
                   .setChunked( true )
                   .write( expectation.body() );

                return;
            }

            req.response()
               .setStatusCode( 404 )
               .setStatusMessage( "Not Found" );

            //            logger.info( "Looking for classpath resource: '{}'", path );
            //
            //            final URL url = Thread.currentThread()
            //                                  .getContextClassLoader()
            //                                  .getResource( path );
            //
            //            logger.info( "Classpath URL is: '{}'", url );
            //
            //            if ( url == null )
            //            {
            //                req.response()
            //                   .setStatusCode( 404 )
            //                   .setStatusMessage( "Not found" );
            //
            //                return;
            //            }
            //            else
            //            {
            //                final String method = req.method()
            //                                         .toUpperCase();
            //
            //                logger.info( "Method: '{}'", method );
            //                if ( "GET".equals( method ) )
            //                {
            //                    doGet( req, url );
            //                }
            //                else if ( "HEAD".equals( method ) )
            //                {
            //                    req.response()
            //                       .setStatusCode( 200 );
            //                }
            //                else
            //                {
            //                    req.response()
            //                       .setStatusCode( 400 )
            //                       .setStatusMessage( "Method: " + method + " not supported by test fixture." );
            //                }
            //            }
        }
        finally
        {
            if ( !ended )
            {
                req.response()
                   .end();
            }
        }
    }

    @SuppressWarnings( "unused" )
    private void doGet( final HttpServerRequest req, final URL url )
    {
        InputStream stream = null;
        try
        {
            stream = url.openStream();

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy( stream, baos );

            final int len = baos.toByteArray().length;
            final Buffer buf = new Buffer( baos.toByteArray() );
            logger.info( "Send: {} bytes", len );
            req.response()
               .putHeader( "Content-Length", Integer.toString( len ) )
               .end( buf );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to stream content for: %s. Reason: %s", url, e.getMessage() ), e );
            req.response()
               .setStatusCode( 500 )
               .setStatusMessage( "FAIL: " + e.getMessage() )
               .end();
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

    public void registerException( final String url, final String error )
    {
        this.errors.put( url, error );
    }

    public void expect( final String testUrl, final int responseCode, final String body )
        throws Exception
    {
        final URL url = new URL( testUrl );
        final String path = url.getPath();

        logger.info( "Registering expection: '{}', code: {}, body:\n{}", path, responseCode, body );
        expectations.put( path, new Expectation( path, responseCode, body ) );
    }

    private static final class Expectation
    {
        private final int code;

        private final String body;

        private final String url;

        Expectation( final String url, final int code, final String body )
        {
            this.url = url;
            this.code = code;
            this.body = body;
        }

        int code()
        {
            return code;
        }

        String body()
        {
            return body == null || body.length() < 1 ? "OK" : body;
        }

        @Override
        public String toString()
        {
            return "Expect (" + url + "), and respond with code:" + code() + ", body:\n" + body();
        }
    }

}

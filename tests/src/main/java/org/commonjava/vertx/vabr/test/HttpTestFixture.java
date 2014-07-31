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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientResponse;

public class HttpTestFixture
    extends ExternalResource
{
    public class TestResponseHandler
        implements Handler<HttpClientResponse>
    {
        int statusCode = -1;

        String statusMessage;

        final StringBuilder body = new StringBuilder();

        @Override
        public void handle( final HttpClientResponse response )
        {
            response.resume()
                    .endHandler( new Handler<Void>()
                    {
                        @Override
                        public void handle( final Void event )
                        {
                            synchronized ( TestResponseHandler.this )
                            {
                                TestResponseHandler.this.notifyAll();
                            }
                        }
                    } )
                    .bodyHandler( new Handler<Buffer>()
                    {

                        @Override
                        public void handle( final Buffer event )
                        {
                            final String string = event.getString( 0, event.length() );
                            body.append( string );
                        }
                    } );

            statusCode = response.statusCode();
            statusMessage = response.statusMessage();

        }

        @Override
        public String toString()
        {
            return String.format( "Response:\n  statusCode=%s\n  statusMessage=%s\n  body:\n\n%s\n\n", statusCode,
                                  statusMessage, body );
        }

        public synchronized void waitForResponse()
        {
            if ( statusCode == -1 )
            {
                try
                {
                    wait();
                }
                catch ( final InterruptedException e )
                {
                    e.printStackTrace();
                    Thread.currentThread()
                          .interrupt();
                }
            }
        }
    }

    private final TemporaryFolder folder;

    private final TestHttpServer server;

    public HttpTestFixture( final TemporaryFolder folder )
    {
        this.folder = folder;
        server = new TestHttpServer();
    }

    public HttpTestFixture( final TemporaryFolder folder, final String baseResource )
    {
        this.folder = folder;
        server = new TestHttpServer( baseResource );
    }

    @Override
    protected void after()
    {
        server.after();
        folder.delete();
        super.after();
    }

    @Override
    protected void before()
        throws Throwable
    {
        super.before();
        server.before();
    }

    public TemporaryFolder folder()
    {
        return folder;
    }

    public TestHttpServer server()
    {
        return server;
    }

    public String formatUrl( final String... subpath )
    {
        return server.formatUrl( subpath );
    }

    public String urlPath( final String url )
        throws MalformedURLException
    {
        return server.getUrlPath( url );
    }

    public String get( final String testUrl, final int expectedResponse )
        throws Exception
    {
        return get( testUrl, null, expectedResponse );
    }

    public String get( final String testUrl, final Map<String, String> headers, final int expectedResponse )
        throws Exception
    {
        final HttpClient http = new DefaultHttpClient();
        final HttpGet get = new HttpGet( formatUrl( testUrl ) );

        if ( headers != null )
        {
            for ( final String key : headers.keySet() )
            {
                get.setHeader( key, headers.get( key ) );
            }
        }

        final HttpResponse response = http.execute( get );
        assertThat( response.getStatusLine()
                            .getStatusCode(), equalTo( expectedResponse ) );

        InputStream stream = null;
        try
        {
            stream = response.getEntity()
                             .getContent();
            return IOUtils.toString( stream );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }

        //        
        //        final URL u = new URL( formatUrl( testUrl ) );
        //        HttpURLConnection conn = null;
        //        InputStream stream = null;
        //        try
        //        {
        //            conn = (HttpURLConnection) u.openConnection();
        //
        //            if ( headers != null )
        //            {
        //                for ( final String key : headers.keySet() )
        //                {
        //                    conn.setRequestProperty( key, headers.get( key ) );
        //                }
        //            }
        //
        //            stream = conn.getInputStream();
        //
        //            final int code = conn.getResponseCode();
        //            assertThat( code, equalTo( expectedResponse ) );
        //
        //            return IOUtils.toString( stream );
        //        }
        //        finally
        //        {
        //            IOUtils.closeQuietly( stream );
        //            IOUtils.close( conn );
        //        }
    }

}

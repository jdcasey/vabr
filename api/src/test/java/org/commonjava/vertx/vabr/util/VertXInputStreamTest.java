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
package org.commonjava.vertx.vabr.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

public class VertXInputStreamTest
{

    private static final String BASE = VertXInputStream.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private int port;

    @Before
    public void setup()
    {
        port = 18080;
    }

    @Test
    //    @Ignore
    public void readViaHttpHandler()
        throws InterruptedException
    {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();

        final Vertx v = new DefaultVertx();
        final HttpServer server = v.createHttpServer()
                                   .requestHandler( new Handler<HttpServerRequest>()
                                   {
                                       @Override
                                       public void handle( final HttpServerRequest request )
                                       {
                                           request.pause();

                                           new Thread( new Runnable()
                                           {
                                               @Override
                                               public void run()
                                               {
                                                   logger.info( "GOT IT" );
                                                   final VertXInputStream stream = new VertXInputStream( request );
                                                   try
                                                   {
                                                       IOUtils.copy( stream, result );

                                                       logger.info( "READ DONE" );
                                                       synchronized ( result )
                                                       {
                                                           result.notifyAll();
                                                       }
                                                   }
                                                   catch ( final IOException e )
                                                   {
                                                       throw new RuntimeException( "Failed to read stream: " + e.getMessage(), e );
                                                   }
                                               }
                                           }, "server-request" ).start();
                                       }
                                   } )
                                   .listen( port, "localhost" );

        final HttpClient client = v.createHttpClient()
                                   .setHost( "localhost" )
                                   .setPort( port );
        final HttpClientRequest put = client.put( "/put", new Handler<HttpClientResponse>()
        {
            @Override
            public void handle( final HttpClientResponse response )
            {
                logger.info( "Response: {} {}", response.statusCode(), response.statusMessage() );
            }
        } );

        final ByteArrayOutputStream check = new ByteArrayOutputStream();
        final Random rand = new Random();

        // 4Mb file...
        final byte[] txfr = new byte[4];
        for ( int i = 0; i < 1048576; i++ )
        {
            rand.nextBytes( txfr );
            check.write( txfr, 0, txfr.length );
        }

        put.setChunked( true )
           .write( new Buffer( check.toByteArray() ) )
           .end();

        logger.info( "SENT: {}", check.toByteArray().length );

        synchronized ( result )
        {
            result.wait();
        }

        final byte[] checkedArry = check.toByteArray();
        final byte[] resultArry = result.toByteArray();
        assertThat( checkedArry.length, equalTo( resultArry.length ) );

        boolean match = true;
        for ( int i = 0; i < checkedArry.length; i++ )
        {
            if ( resultArry[i] != checkedArry[i] )
            {
                logger.error( "Index {} mismatch! Was: {}, expected: {}", i, resultArry[i], checkedArry[i] );
                match = false;
            }
        }

        assertThat( "Byte arrays do not match.", match, equalTo( true ) );

        server.close();
        client.close();
    }

    @Test
    public void readSimpleFileViaAsyncFile()
        throws IOException
    {
        final String path = getResource( BASE, "test-read.txt" );

        final String check = FileUtils.readFileToString( new File( path ) );

        final FileHandler fh = new FileHandler();

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( path, fh );

        synchronized ( fh )
        {
            try
            {
                fh.wait();
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }

        InputStream stream = null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            stream = new VertXInputStream( fh.af );
            IOUtils.copy( stream, baos );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }

        assertThat( new String( baos.toByteArray() ), equalTo( check ) );
    }

    @Test
    public void closeFileBeforeReadingAll()
        throws IOException
    {
        final FileHandler fh = new FileHandler();

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( getResource( BASE, "test-early-close.txt" ), fh );

        synchronized ( fh )
        {
            try
            {
                fh.wait();
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }

        InputStream stream = null;
        try
        {
            stream = new VertXInputStream( fh.af );

            final byte[] buf = new byte[15];
            stream.read( buf );

            assertThat( new String( buf ), equalTo( "This is a test!" ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

    @Test
    public void closeFileBeforeReadingAny()
        throws IOException
    {
        final FileHandler fh = new FileHandler();

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( getResource( BASE, "test-early-close.txt" ), fh );

        synchronized ( fh )
        {
            try
            {
                fh.wait();
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }

        InputStream stream = null;
        try
        {
            stream = new VertXInputStream( fh.af );
            //
            //            final byte[] buf = new byte[15];
            //            stream.read( buf );
            //
            //            assertThat( new String( buf ), equalTo( "This is a test!" ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

    private String getResource( final String base, final String... parts )
    {
        final String path = Paths.get( base, parts )
                                 .toString();

        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( path );

        if ( resource == null )
        {
            fail( "Cannot find classpath resource: " + path );
        }

        return resource.getPath();
    }

    private static final class FileHandler
        implements Handler<AsyncResult<AsyncFile>>
    {
        private AsyncFile af;

        @Override
        public synchronized void handle( final AsyncResult<AsyncFile> event )
        {
            af = event.result();
            notifyAll();
        }
    }

}

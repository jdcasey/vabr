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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class VertXInputStream
    extends InputStream
{

    private final class DataHandler
        implements Handler<Buffer>
    {
        @Override
        public void handle( final Buffer buffer )
        {
            synchronized ( VertXInputStream.this )
            {
                pending.add( buffer );
                VertXInputStream.this.notifyAll();
            }
        }
    };

    private final class EndHandler
        implements Handler<Void>
    {
        @Override
        public void handle( final Void event )
        {
            synchronized ( VertXInputStream.this )
            {
                pending.add( null );
                VertXInputStream.this.notifyAll();

                VertXInputStream.this.awaitDrain();
            }
        }
    };

    private final DataHandler dataHandler;

    private final EndHandler endHandler;

    private final List<Buffer> pending = new ArrayList<>();

    private byte[] current;

    private int index = 0;

    private final long contentLength;

    private long total = 0;

    private boolean done;

    public VertXInputStream( final ReadStream<?> stream )
    {
        this( stream, -1 );
    }

    public VertXInputStream( final ReadStream<?> stream, final long contentLength )
    {
        this.contentLength = contentLength;
        if ( stream == null )
        {
            throw new NullPointerException( "Cannot read from null stream!" );
        }

        dataHandler = new DataHandler();
        endHandler = new EndHandler();

        stream.resume();
        stream.dataHandler( dataHandler );
        stream.endHandler( endHandler );
    }

    @Override
    public int read()
        throws IOException
    {
        if ( contentLength > 0 && total == contentLength )
        {
            done = true;
            // notify the end handler...
            synchronized ( this )
            {
                notifyAll();
            }

            return -1;
        }

        if ( done )
        {
            // notify the end handler...
            synchronized ( this )
            {
                notifyAll();
            }

            return -1;
        }

        boolean refresh = false;
        // first condition is for bootstrap
        // second condition is when last page is done
        if ( current == null )
        {
            refresh = true;
        }
        else if ( index >= current.length )
        {
            // in case we've read past the last page, set it to null before waiting.
            current = null;
            refresh = true;
        }

        if ( refresh )
        {
            synchronized ( this )
            {
                // as long as there are no new pages, wait.
                while ( pending.isEmpty() )
                {
                    try
                    {
                        wait( 500 );
                    }
                    catch ( final InterruptedException e )
                    {
                        Thread.currentThread()
                              .interrupt();

                        //System.out.println( "INTERRUPT-END READ" );
                        return -1;
                    }
                }

                // pull off the next page
                final Buffer buffer = pending.remove( 0 );
                if ( buffer != null )
                {
                    current = buffer.getBytes();
                }

                index = 0;

                // notify to wake up the end handler in case it's waiting.
                notifyAll();
            }

            if ( current == null )
            {
                // we're done.
                done = true;
                return -1;
            }
        }

        final int b = current[index] & 0xff;

        index++;
        total++;

        return b;
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();
    }

    public synchronized void awaitDrain()
    {
        while ( !done )
        {
            try
            {
                wait( 500 );
            }
            catch ( final InterruptedException e )
            {
                Thread.currentThread()
                      .interrupt();

                return;
            }
        }
    }

}

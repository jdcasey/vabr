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
import java.io.OutputStream;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.streams.WriteStream;

public class VertXOutputStream
    extends OutputStream
{

    private final WriteStream<?> response;

    private final byte[] buffer;

    private int counter = 0;

    public VertXOutputStream( final WriteStream<?> response )
    {
        this.response = response;

        // TODO is there a better size? Common MTU on TCP/IP is 1500, with about 1380 payload...is that better?
        buffer = new byte[16384];
    }

    @Override
    public synchronized void write( final int b )
        throws IOException
    {
        buffer[counter++] = (byte) b;
        if ( counter >= buffer.length )
        {
            flush();
        }
    }

    @Override
    public synchronized void flush()
        throws IOException
    {
        super.flush();
        if ( counter > 0 )
        {
            byte[] remaining = buffer;
            if ( counter < buffer.length )
            {
                remaining = new byte[counter];
                System.arraycopy( buffer, 0, remaining, 0, counter );
            }
            response.write( new Buffer( remaining ) );
            counter = 0;
        }
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        flush();
        super.close();
        if ( response instanceof HttpServerResponse )
        {
            try
            {
                ( (HttpServerResponse) response ).end();
            }
            catch ( final IllegalStateException e )
            {

            }
        }
        else if ( response instanceof AsyncFile )
        {
            ( (AsyncFile) response ).close();
        }
    }

}

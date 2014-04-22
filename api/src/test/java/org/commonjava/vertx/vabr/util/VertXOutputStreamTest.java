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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.impl.DefaultVertx;

public class VertXOutputStreamTest
{

    private static final String BASE = VertXOutputStream.class.getSimpleName();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File tempFolder;

    @Before
    public void tempFolder()
        throws IOException
    {
        tempFolder = temp.newFolder();
    }

    @Test
    public void writeSimpleFileViaAsyncFile()
        throws IOException
    {
        final FileHandler fh = new FileHandler();

        final File f = getTempResource( BASE, "test-write.txt" );

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( f.getAbsolutePath(), fh );

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

        final ByteArrayInputStream bain = new ByteArrayInputStream( "This is a test!".getBytes() );
        VertXOutputStream stream = null;
        try
        {
            stream = new VertXOutputStream( fh.af );
            IOUtils.copy( bain, stream );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }

        final String result = FileUtils.readFileToString( f );
        assertThat( result, equalTo( "This is a test!" ) );
    }

    private File getTempResource( final String base, final String... parts )
    {
        final String path = Paths.get( base, parts )
                                 .toString();

        final File f = new File( tempFolder, path );
        f.getParentFile()
         .mkdirs();

        return f;
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

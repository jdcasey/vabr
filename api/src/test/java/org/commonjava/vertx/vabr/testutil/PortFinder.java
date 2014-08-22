package org.commonjava.vertx.vabr.testutil;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public final class PortFinder
{
    private static final int TRIES = 4;

    private static final Random RANDOM = new Random();

    private PortFinder()
    {
    }

    public static int findOpenPort()
    {
        for ( int i = 0; i < TRIES; i++ )
        {
            final int port = 1024 + ( Math.abs( RANDOM.nextInt() ) % 30000 );
            ServerSocket sock = null;
            try
            {
                sock = new ServerSocket( port );
                return port;
            }
            catch ( final IOException e )
            {
            }
            finally
            {
                IOUtils.closeQuietly( sock );
            }
        }

        Assert.fail( "Cannot find open port after " + TRIES + " attempts." );
        return -1;
    }

}

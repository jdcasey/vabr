package org.commonjava.vertx.vabr.testutil;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClientResponse;

public class WaitHandler
    implements Handler<HttpClientResponse>
{
    @Override
    public synchronized void handle( final HttpClientResponse event )
    {
        notifyAll();
    }
}
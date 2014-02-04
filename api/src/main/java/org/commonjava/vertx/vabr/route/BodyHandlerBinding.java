package org.commonjava.vertx.vabr.route;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.Method;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class BodyHandlerBinding
    extends RouteBinding
    implements Handler<Buffer>
{

    protected HttpServerRequest request;

    public BodyHandlerBinding( final int priority, final String path, final Method method, final String contentType, final String handlerKey )
    {
        super( priority, path, method, contentType, handlerKey );
    }

    @Override
    protected void dispatch( final ApplicationRouter router, final HttpServerRequest request )
        throws Exception
    {
        this.request = request;
        request.bodyHandler( this );
    }

}

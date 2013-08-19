package org.commonjava.freeki.infra.route;

import org.vertx.java.core.http.HttpServerRequest;

public abstract class RouteBinding
{

    public static final String RECOMMENDED_CONTENT_TYPE = "Recommended-Content-Type";

    private final String path;

    private final Method method;

    private final String contentType;

    protected RouteBinding( final String path, final Method method, final String contentType )
    {
        this.path = path;
        this.method = method;
        this.contentType = contentType.length() < 1 ? null : contentType;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getPath()
    {
        return path;
    }

    public Method getMethod()
    {
        return method;
    }

    @Override
    public String toString()
    {
        return "Route [" + method.name() + " " + path + "]";
    }

    public void handle( final ApplicationRouter router, final HttpServerRequest req )
        throws Exception
    {
        if ( contentType != null )
        {
            req.headers()
               .add( RECOMMENDED_CONTENT_TYPE, contentType );
        }

        dispatch( router, req );
    }

    protected abstract void dispatch( ApplicationRouter router, HttpServerRequest req )
        throws Exception;

}

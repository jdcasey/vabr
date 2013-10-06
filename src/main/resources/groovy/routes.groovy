package ${pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.RouteBinding;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.AbstractRouteCollection;

import org.vertx.java.core.http.HttpServerRequest;

import org.commonjava.util.logging.Logger;

public final class Routes
    extends AbstractRouteCollection
{

    private final Logger logger = new Logger( getClass() );

    public Routes()
    {<% routes.each { %>
        bind( new RouteBinding( "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}" )
        {
            public void dispatch( ApplicationRouter router, HttpServerRequest req )
                throws Exception
            {
                ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
                if ( handler != null )
                {
                    logger.debug( "Handling via: %s", handler );
                    handler.${it.methodname}( req );
                }
                else
                {
                    throw new RuntimeException( "Cannot retrieve handler instance for: '${it.httpPath}' using method: '${it.httpMethod.name()}'" );
                } 
            }
        } );
        <% } %>
    }

}

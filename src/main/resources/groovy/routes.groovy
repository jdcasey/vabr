package ${pkg};

import org.commonjava.freeki.infra.route.ApplicationRouter;
import org.commonjava.freeki.infra.route.RouteBinding;
import org.commonjava.freeki.infra.route.Method;
import org.commonjava.freeki.infra.route.AbstractRouteCollection;
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
                    logger.info( "Handling via: %s", handler );
                    handler.${it.methodname}( req );
                }
                else
                {
                    throw new RuntimeException( "Cannot retrieve handler instance for: '${it.httpPath}' using method: '${it.httpMethod.name()}'" );
                } 
            }
        } );<% } %>
    }

}

package ${pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.route.RouteBinding;
import org.commonjava.vertx.vabr.route.AbstractRouteCollection;

import org.vertx.java.core.http.HttpServerRequest;

import org.commonjava.util.logging.Logger;
<%if( qualifier ){ %>
import ${qualifier.fullName};

@${qualifier.simpleName}<% } %>
public final class ${className}
    extends AbstractRouteCollection
{

    private final Logger logger = new Logger( getClass() );

    public ${className}()
    {<% templates.each { %>
        bind( new RouteBinding( ${it.priority}, "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}" )
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
                    throw new RuntimeException( "Cannot retrieve handler instance for: " + toString() );
                } 
            }
        } );
        <% } %>
    }

}

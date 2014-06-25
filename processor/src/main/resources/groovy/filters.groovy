package ${pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.filter.ExecutionChain;
import org.commonjava.vertx.vabr.filter.FilterBinding;
import org.commonjava.vertx.vabr.filter.AbstractFilterCollection;

import org.vertx.java.core.http.HttpServerRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

<%if( qualifier ){ %>
import ${qualifier.fullName};

@${qualifier.simpleName}<% } %>
public final class ${className}
    extends AbstractFilterCollection
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public ${className}()
    {<% templates.each { %>
        bind( new FilterBinding( ${it.priority}, "${it.httpPath}", Method.${it.httpMethod}, "${it.handlerKey}" )
        {
            public void dispatch( ApplicationRouter router, HttpServerRequest req, ExecutionChain chain )
                throws Exception
            {
                ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
                if ( handler != null )
                {
                    logger.debug( "Filtering via: " + handler );
                    try
                    {
                        handler.${it.methodname}( req, chain );
                    }
                    catch ( Throwable error )
                    {
                        if ( error instanceof InterruptedException ){ Thread.currentThread().interrupt(); }
        
                        String message = String.format( "Error executing %s. Reason: %s", this, error.getMessage() );
                        logger.error( message );
                        request.response().setStatusCode( 500 )
                                          .setStatusMessage( message )
                                          .end();
                    }
                }
                else
                {
                    String message = "[VABR] Cannot retrieve handler instance for: " + toString();
                    logger.error( message );
                    request.response().setStatusCode( 500 )
                                      .setStatusMessage( message )
                                      .end();
                } 
            }
        } );
        <% } %>
    }

}

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
            public void dispatch( ApplicationRouter router, HttpServerRequest request, ExecutionChain chain )
                throws Exception
            {
                request.pause();
                
                ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
                if ( handler != null )
                {
                    logger.debug( "Filtering via: " + handler );
                    try
                    {
                        handler.${it.methodname}( request, chain );
                    }
                    catch ( Throwable error )
                    {
                        if ( error instanceof InterruptedException ){ Thread.currentThread().interrupt(); }
        
                        long marker = System.currentTimeMillis();
                        String message = String.format( "(%s) Error executing %s. Reason: %s", marker, this, error.getMessage() );
                        logger.error( message, error );
                        request.resume().response().setStatusCode( 500 )
                                          .setStatusMessage( "Internal Server Error (" + marker + ")" )
                                          .end();
                    }
                }
                else
                {
                    String message = "[VABR] Cannot retrieve handler instance for: " + toString();
                    logger.error( message );
                    request.resume().response().setStatusCode( 500 )
                                      .setStatusMessage( message )
                                      .end();
                } 
            }
        } );
        <% } %>
    }

}

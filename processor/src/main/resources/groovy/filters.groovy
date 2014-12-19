package ${pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.bind.filter.ExecutionChain;
import org.commonjava.vertx.vabr.bind.filter.FilterBinding;
import org.commonjava.vertx.vabr.bind.filter.AbstractFilterCollection;

import org.vertx.java.core.http.HttpServerRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

<%if( qualifier ){ %>
import ${qualifier.fullName};

@${qualifier.simpleName}<% } %>
public final class ${className}
    extends AbstractFilterCollection
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public ${className}()
    {<% templates.each {
          def versions = "new ArrayList<String>(Arrays.<String>asList(" + it.getVersions().collect({v -> "\"" + v + "\""}).join(", ") + "))"
     %>
        bind( new FilterBinding( ${it.priority}, "${it.httpPath}", "${it.routePathFragment}", "${it.handlerPathFragment}", Method.${it.httpMethod}, "${it.handlerKey}", ${versions} )
        {
            private final Logger logger = LoggerFactory.getLogger( getClass() );

            public void dispatch( ApplicationRouter router, HttpServerRequest request, ExecutionChain chain )
                throws Exception
            {
                try
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
                catch( Throwable e )
                {
                    logger.error( e.getMessage(), e );
                }            
            }
        } );
        <% } %>
    }

}

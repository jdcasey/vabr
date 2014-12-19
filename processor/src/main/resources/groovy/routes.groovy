package ${pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.bind.route.RouteBinding;
import org.commonjava.vertx.vabr.bind.route.AbstractRouteCollection;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

<%if( qualifier ){ %>
import ${qualifier.fullName};

@${qualifier.simpleName}<% } %>
public final class ${className}
    extends AbstractRouteCollection
{

    public ${className}()
    {<% templates.each
        {
          if ( it.getBinding().name() == "raw" )
          { 
    %>
        bind( new RawBinding_${it.classname}_${it.methodname}_${it.routeKey}() );
    <%    } 
          else if ( it.getBinding().name() == "body_handler" )
          {
    %>
        bind( new BodyBinding_${it.classname}_${it.methodname}_${it.routeKey}() );
    <%    }
        }
    %>
    }
    
    <% templates.each {
          def versions = "new ArrayList<String>(Arrays.<String>asList(" + it.getVersions().collect({v -> "\"" + v + "\""}).join(", ") + "))"
          if ( it.getBinding().name() == "raw" )
          {
    %>
    public static final class RawBinding_${it.classname}_${it.methodname}_${it.routeKey}
        extends RouteBinding
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        public RawBinding_${it.classname}_${it.methodname}_${it.routeKey}()
        {
            super( ${it.priority}, "${it.httpPath}", "${it.routePathFragment}", "${it.handlerPathFragment}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}", ${it.qualifiedClassname}.class, "${it.methodname}", ${versions} );
        }
        
        public void dispatch( ApplicationRouter router, HttpServerRequest request )
        {
            try
            {
                request.pause();
                
                ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
                if ( handler != null )
                {
                    <% if( it.isFork()){ %>
                    router.getHandlerExecutor().execute( new RawRunnable_${it.classname}_${it.methodname}_${it.routeKey}( handler, request ) );
                    <% }else{ %>
                    new RawRunnable_${it.classname}_${it.methodname}_${it.routeKey}( handler, request ).run();
                    <%}%>
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
    }
    
    public static final class RawRunnable_${it.classname}_${it.methodname}_${it.routeKey}
        implements Runnable
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );
        
        private final ${it.qualifiedClassname} handler;
        
        private HttpServerRequest request;

        public RawRunnable_${it.classname}_${it.methodname}_${it.routeKey}( ${it.qualifiedClassname} handler, HttpServerRequest request )
        {
            this.handler = handler;
            this.request = request;
        }
        
        public void run()
        {
            try
            {
                logger.debug( "Handling via: " + handler );
                
                handler.${it.methodname}( ${it.callParams.join(', ')} );
            }
            catch ( Throwable error )
            {
                if ( error instanceof InterruptedException ){ Thread.currentThread().interrupt(); }
        
                long marker = System.currentTimeMillis();
                String message = String.format( "(%s) Error executing %s. Reason: %s", marker, this, error.getMessage() );
                logger.error( message, error );
                try
                {
                    request.resume().response().setStatusCode( 500 )
                                      .setStatusMessage( "Internal Server Error (" + marker + ")" )
                                      .end("An unhandled exception has occurred.");
                }
                catch ( Throwable errorError )
                {
                    logger.error( errorError.getMessage(), errorError );
                }
            }
        }
    }
    <%
         }
         else if ( it.getBinding().name() == "body_handler" )
         {
    %>
    public static final class BodyBinding_${it.classname}_${it.methodname}_${it.routeKey}
        extends RouteBinding
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );
    
        public BodyBinding_${it.classname}_${it.methodname}_${it.routeKey}()
        {
            super( ${it.priority}, "${it.httpPath}", "${it.routePathFragment}", "${it.handlerPathFragment}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}", ${it.qualifiedClassname}.class, "${it.methodname}", ${versions} );
        }
        
        public synchronized void dispatch( ApplicationRouter router, HttpServerRequest request )
        {
            try
            {
                request.pause();
                ${it.qualifiedClassname} target = router.getResourceInstance( ${it.qualifiedClassname}.class );
                
                if ( target == null )
                {
                    String message = "[VABR] Cannot retrieve handler instance for: " + toString();
                    logger.error( message );
                    request.resume().response().setStatusCode( 500 )
                                      .setStatusMessage( message )
                                      .end();
                } 
                
                <% if( it.isFork()){ %>
                router.getHandlerExecutor().execute( new BodyHandler_${it.classname}_${it.methodname}_${it.routeKey}( target, request ) );
                <% }else{ %>
                new BodyHandler_${it.classname}_${it.methodname}_${it.routeKey}( target, request ).run();
                <%}%>
            }
            catch( Throwable e )
            {
                logger.error( e.getMessage(), e );
            }            
        }
    }
    
    public static final class BodyHandler_${it.classname}_${it.methodname}_${it.routeKey}
        implements Handler<Buffer>, Runnable
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );
    
        private final ${it.qualifiedClassname} handler;
        
        private final HttpServerRequest request;
        
        private Buffer body;
        
        public BodyHandler_${it.classname}_${it.methodname}_${it.routeKey}( ${it.qualifiedClassname} handler, final HttpServerRequest request )
        {
            this.handler = handler;
            this.request = request;
            logger.info( "Attaching this as body handler.");
            request.bodyHandler( this );
            request.resume();
        }
        
        public synchronized void handle( Buffer body )
        {
            request.pause();
            logger.debug( "Got request body.");
            this.body = body;
        }
        
        public void run()
        {
            try
            {
                synchronized( this )
                {
                    while( body == null )
                    {
                        try
                        {
                            wait(100);
                        }
                        catch( InterruptedException e )
                        {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                
                request.pause();
                logger.debug( "Handling via: " + handler );
                handler.${it.methodname}( ${it.callParams.join(', ')} );
            }
            catch ( Throwable error )
            {
                if ( error instanceof InterruptedException ){ Thread.currentThread().interrupt(); }
        
                long marker = System.currentTimeMillis();
                String message = String.format( "(%s) Error executing %s. Reason: %s", marker, this, error.getMessage() );
                logger.error( message, error );
                try
                {
                    request.resume().response().setStatusCode( 500 )
                                      .setStatusMessage( "Internal Server Error (" + marker + ")" )
                                      .end("An unhandled exception has occurred.");
                }
                catch ( Throwable errorError )
                {
                    logger.error( errorError.getMessage(), errorError );
                }
            }
        }
    }
    <%    }
       } %>
}

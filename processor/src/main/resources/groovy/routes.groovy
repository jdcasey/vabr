package ${import com.hazelcast.logging.LoggerFactory;

pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.route.RouteBinding;
import org.commonjava.vertx.vabr.route.AbstractRouteCollection;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
          if ( it.getBinding().name() == "raw" )
          {
    %>
    public static final class RawBinding_${it.classname}_${it.methodname}_${it.routeKey}
        extends RouteBinding
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        public RawBinding_${it.classname}_${it.methodname}_${it.routeKey}()
        {
            super( ${it.priority}, "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}", ${it.qualifiedClassname}.class, "${it.methodname}" );
        }
        
        public void dispatch( ApplicationRouter router, HttpServerRequest request )
        {
            ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
            if ( handler != null )
            {
                router.getHandlerExecutor().execute( new RawRunnable_${it.classname}_${it.methodname}_${it.routeKey}( handler, request ) );
            }
            else
            {
                throw new RuntimeException( "Cannot retrieve handler instance for: " + toString() );
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
            logger.debug( "Handling via: {}", handler );
            
            handler.${it.methodname}( ${it.callParams.join(', ')} );
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
            super( ${it.priority}, "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}", ${it.qualifiedClassname}.class, "${it.methodname}" );
        }
        
        public synchronized void dispatch( ApplicationRouter router, HttpServerRequest request )
        {
            request.pause();
            ${it.qualifiedClassname} target = router.getResourceInstance( ${it.qualifiedClassname}.class );
            
            if ( target == null )
            {
                throw new RuntimeException( "Cannot retrieve handler instance for: " + toString() );
            } 
            
            router.getHandlerExecutor().execute( new BodyHandler_${it.classname}_${it.methodname}_${it.routeKey}( target, request ) );
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
            logger.info( "Got request body.");
            this.body = body;
        }
        
        public void run()
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
            logger.info( "Handling via: {}", handler );
            handler.${it.methodname}( ${it.callParams.join(', ')} );
        }
    }
    <%    }
       } %>
}

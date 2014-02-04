package ${pkg};

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.route.RouteBinding;
import org.commonjava.vertx.vabr.route.AbstractRouteCollection;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import org.commonjava.util.logging.Logger;
<%if( qualifier ){ %>
import ${qualifier.fullName};

@${qualifier.simpleName}<% } %>
public final class ${className}
    extends AbstractRouteCollection
{

    public ${className}()
    {<% templates.each { %>
        bind( new Binding_${it.classname}_${it.methodname}_${it.routeKey}() );
        <% } %>
    }
    
    <% templates.each {
          if ( it.getBinding().name() == "raw" )
          {
    %>
    public static final class Binding_${it.classname}_${it.methodname}_${it.routeKey}
        extends RouteBinding
    {
        private final Logger logger = new Logger( getClass() );

        public Binding_${it.classname}_${it.methodname}_${it.routeKey}()
        {
            super( ${it.priority}, "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}" );
        }
        
        public void dispatch( ApplicationRouter router, HttpServerRequest request )
            throws Exception
        {
            request.pause();
            
            ${it.qualifiedClassname} handler = router.getResourceInstance( ${it.qualifiedClassname}.class );
            if ( handler != null )
            {
                logger.debug( "Handling via: %s", handler );
                
                request.resume();
                handler.${it.methodname}( ${it.callParams.join(', ')} );
            }
            else
            {
                throw new RuntimeException( "Cannot retrieve handler instance for: " + toString() );
            } 
        }
    }
    <%
         }
         else if ( it.getBinding().name() == "body_handler" )
         {
    %>
    public static final class Binding_${it.classname}_${it.methodname}_${it.routeKey}
        extends RouteBinding
    {
        private final Logger logger = new Logger( getClass() );
    
        public Binding_${it.classname}_${it.methodname}_${it.routeKey}()
        {
            super( ${it.priority}, "${it.httpPath}", Method.${it.httpMethod}, "${it.httpContentType}", "${it.handlerKey}" );
        }
        
        public synchronized void dispatch( ApplicationRouter router, HttpServerRequest request )
            throws Exception
        {
            request.pause();
            ${it.qualifiedClassname} target = router.getResourceInstance( ${it.qualifiedClassname}.class );
            
            if ( target == null )
            {
                throw new RuntimeException( "Cannot retrieve handler instance for: " + toString() );
            } 
            
            new Handler_${it.classname}_${it.methodname}_${it.routeKey}( target, request );
        }
    }
    
    public static final class Handler_${it.classname}_${it.methodname}_${it.routeKey}
        implements Handler<Buffer>
    {
        private final Logger logger = new Logger( getClass() );
    
        private final ${it.qualifiedClassname} handler;
        
        private final HttpServerRequest request;
        
        public Handler_${it.classname}_${it.methodname}_${it.routeKey}( ${it.qualifiedClassname} handler, final HttpServerRequest request )
        {
            this.handler = handler;
            this.request = request;
            request.bodyHandler( this );
            request.resume();
        }
        
        public void handle( Buffer body )
        {
            logger.debug( "Handling via: %s", handler );
            handler.${it.methodname}( ${it.callParams.join(', ')} );
        }
    }
    <%    }
       } %>
}

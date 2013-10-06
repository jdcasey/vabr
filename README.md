# Vert.x Annotation-Based Routing (for the Java API)

This is a relatively simple API and Java annotation processor that generates a Java source file containing routing object instances. Each instance corresponds to a different route, expressed using a path pattern (which may contain regular expressions) and a couple of other options. These instances basically delegate the call to the method that declared them.

## Step 1: Annotate

So, if you have a Java class that looks like this:

{{{
  
    package org.foo.myapp;
    
    import org.commonjava.freeki.data.TemplateController;
    import org.commonjava.freeki.data.TemplateException;
    import org.commonjava.freeki.infra.route.Method;
    import org.commonjava.freeki.infra.route.RouteHandler;
    import org.commonjava.freeki.infra.route.anno.Route;
    import org.commonjava.freeki.infra.route.anno.Routes;
    
    public class MyContentHandler
        implements RouteHandler
    {
    
        /* @formatter:off */
        @Routes( {
            @Route( path="/templates/:template", method=Method.GET )
        } )
        /* @formatter:on */
        public void get( final HttpServerRequest req )
            throws Exception
        {
            final String template = req.params().get( "template" );
            
            final File html = controller.getTemplateHtml( template );
            if ( !html.exists() )
            {
                req.response()
                   .setStatusCode( 404 )
                   .setStatusMessage( "Not Found" )
                   .end();
            }
            else
            {
                req.response()
                   .setStatusCode( 200 )
                   .sendFile( html.getAbsolutePath() );
            }
        }
    
        private final TemplateController controller;
        
        private final JsonSerializer serializer;
        
        public TemplateContentHandler( final TemplateController controller, final JsonSerializer serializer )
        {
            this.controller = controller;
            this.serializer = serializer;
        }
    }
}}}

## Step 2: Generate

The Apache Maven POM configuration will look similar to this:

{{{xml
    
    <properties>
      <vabrVersion>1.0-SNAPSHOT</vabrVersion>
    </properties>
    
    <dependencies>
      [...]
    
      <dependency>
        <groupId>org.commonjava.vertx</groupId>
        <artifactId>vabr</artifactId>
        <version>${vabrVersion}</version>
      </dependency>
    </dependencies>
    
    <build>
      [...]
    
      <pluginManagement>
        <plugins>
          <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <dependencies>
              <dependency>
                <groupId>org.commonjava.vertx</groupId>
                <artifactId>vabr</artifactId>
                <version>${vabrVersion}</version>
              </dependency>
            </dependencies>
            <configuration>
              <source>1.7</source>
              <target>1.7</target>
              <annotationProcessors>
                <annotationProcessor>org.commonjava.freeki.infra.route.anno.proc.RoutingAnnotationProcessor</annotationProcessor>
              </annotationProcessors>
            </configuration>
          </plugin>
        
          [...]
        </plugins>
      </pluginManagement>
      [...]
    </build>
}}}

The annotation processor will generate the following Java:

{{{
    
    package org.foo.myapp;
    
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
        {
            bind( new RouteBinding( "/templates/:template", Method.GET, "" )
            {
                public void dispatch( ApplicationRouter router, HttpServerRequest req )
                    throws Exception
                {
                    org.foo.myapp.TemplateContentHandler handler = router.getResourceInstance( org.foo.myapp.TemplateContentHandler.class );
                    if ( handler != null )
                    {
                        logger.info( "Handling via: %s", handler );
                        handler.get( req );
                    }
                    else
                    {
                        throw new RuntimeException( "Cannot retrieve handler instance for: '/templates/:template' using method: 'GET'" );
                    } 
                }
            } );
        }
    }
}}}

## Step 3: Launch!

To initialize the controller that knows how to handle these generated route bindings, use something like the following:

{{{
    
    final Set<RouteHandler> handlers = Collections.singleton( new TemplateContentHandler( new TemplateController( store, config ), serializer ) );
    
    final ServiceLoader<RouteCollection> collections = ServiceLoader.load( RouteCollection.class );
    
    final ApplicationRouter router = new ApplicationRouter( handlers, collections );
    
    vertx.createHttpServer()
         .requestHandler( router )
         .listen( 8080, "127.0.0.1" );
}}}

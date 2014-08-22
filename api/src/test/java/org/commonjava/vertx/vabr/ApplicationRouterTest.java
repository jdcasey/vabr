package org.commonjava.vertx.vabr;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.bind.route.RouteBinding;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.testutil.PortFinder;
import org.commonjava.vertx.vabr.testutil.WaitHandler;
import org.commonjava.vertx.vabr.types.BuiltInParam;
import org.commonjava.vertx.vabr.types.Method;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

public class ApplicationRouterTest
{

    private HttpServer server;

    private HttpClient client;

    private Vertx vertx;

    private ParamAccess access;

    private Handler<HttpClientResponse> clientHandler;

    private int port;

    private String host;

    @Before
    public void setup()
    {
        access = new ParamAccess();
        vertx = new DefaultVertx();

        port = PortFinder.findOpenPort();
        host = "127.0.0.1";

        client = vertx.createHttpClient()
                      .setHost( host )
                      .setPort( port );

        clientHandler = new WaitHandler();
    }

    @After
    public void shutdown()
    {
        if ( client != null )
        {
            client.close();
        }

        if ( server != null )
        {
            server.close();
        }
    }

    @Test
    public void builtInParams_PrefixedPathWithParamsRightAfterPrefix()
        throws Exception
    {
        final MultiMap params = getParams( "/api", "/:type/:name:path=(/.*)", "/api/remote/foo/" );

        final String basePath = "/api";
        final String baseUrl = "http://" + host + ":" + port + basePath;
        assertThat( params.get( BuiltInParam._classBase.key() ), equalTo( basePath ) );
        assertThat( params.get( BuiltInParam._classContextUrl.key() ), equalTo( baseUrl ) );
        assertThat( params.get( BuiltInParam._routeBase.key() ), equalTo( basePath ) );
        assertThat( params.get( BuiltInParam._routeContextUrl.key() ), equalTo( baseUrl ) );
        assertThat( params.get( "type" ), equalTo( "remote" ) );
        assertThat( params.get( "name" ), equalTo( "foo" ) );
        assertThat( params.get( "path" ), equalTo( "/" ) );
    }

    private MultiMap getParams( final String prefix, final String bindingPath, final String requestPath )
        throws Exception
    {
        access = new ParamAccess();

        server =
            vertx.createHttpServer()
                 .requestHandler( new ApplicationRouter(
                                                         new ApplicationRouterConfig().withPrefix( prefix )
                                                                                      .withHandler( access )
                                                                                      .withRouteCollection( new ParamCollection(
                                                                                                                                 bindingPath,
                                                                                                                                 access ) ) ) )
                 .listen( port, host );

        final HttpClientRequest req = client.get( requestPath, clientHandler );

        req.end();

        synchronized ( clientHandler )
        {
            clientHandler.wait();
        }

        return access.params();
    }

    public static final class ParamCollection
        implements RouteCollection
    {

        private final ParamRouteBinding binding;

        public ParamCollection( final String path, final ParamAccess access )
        {
            binding = new ParamRouteBinding( path, access );
        }

        @Override
        public Iterator<RouteBinding> iterator()
        {
            return Collections.<RouteBinding> singleton( binding )
                              .iterator();
        }

        @Override
        public Set<RouteBinding> getRoutes()
        {
            return Collections.<RouteBinding> singleton( binding );
        }

    }

    public static final class ParamRouteBinding
        extends RouteBinding
    {
        private final ParamAccess access;

        public ParamRouteBinding( final String path, final ParamAccess access )
        {
            super( 1, path, Method.GET, "application/foo", "key", ParamAccess.class, "handle",
                   Collections.singletonList( "v1" ) );
            this.access = access;
        }

        @Override
        protected void dispatch( final ApplicationRouter router, final HttpServerRequest req )
            throws Exception
        {
            access.handle( req );
        }
    }

    @Handles( key = "content" )
    public static final class ParamAccess
        implements RequestHandler
    {

        private MultiMap params;

        public void handle( final HttpServerRequest event )
        {
            params = event.params();
            event.response()
                 .setStatusCode( 200 )
                 .setStatusMessage( "OK" )
                 .end();
        }

        public void clear()
        {
            params = null;
        }

        public MultiMap params()
        {
            return params;
        }

    }

}

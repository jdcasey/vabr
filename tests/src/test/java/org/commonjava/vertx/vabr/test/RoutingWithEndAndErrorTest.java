package org.commonjava.vertx.vabr.test;

import java.net.URLClassLoader;

import org.commonjava.test.compile.CompilerFixture;
import org.commonjava.test.compile.CompilerFixtureConfig;
import org.commonjava.test.compile.CompilerResult;
import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.ApplicationRouterConfig;
import org.commonjava.vertx.vabr.anno.proc.RoutingAnnotationProcessor;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RoutingWithEndAndErrorTest
{

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public HttpTestFixture fixture = new HttpTestFixture( folder );

    @Rule
    public CompilerFixture compiler = new CompilerFixture( folder );

    @Test
    public void routeUsingEndAndThrowingException()
        throws Exception
    {
        final CompilerResult result =
            compiler.compileSourceDirWithThisClass( "route-with-end-and-error",
                                                    "org.test.MyResource",
                                                    new CompilerFixtureConfig().withAnnotationProcessor( RoutingAnnotationProcessor.class ) );

        final URLClassLoader classLoader = result.getClassLoader();
        final Class<?> routesCls = classLoader.loadClass( "org.test.Routes" );
        final RouteCollection collection = (RouteCollection) routesCls.newInstance();

        final Class<?> handlerCls = classLoader.loadClass( "org.test.MyResource" );
        final RequestHandler handler = (RequestHandler) handlerCls.newInstance();

        final ApplicationRouter router =
            new ApplicationRouter( new ApplicationRouterConfig().withHandler( handler )
                                                                .withRouteCollection( collection ) );

        fixture.server()
               .setHandlerAndStart( router );

        fixture.get( "/my/name", 500 );
        fixture.get( "/my/name", 500 );
    }

}

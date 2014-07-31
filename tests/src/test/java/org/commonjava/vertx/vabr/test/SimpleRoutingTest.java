package org.commonjava.vertx.vabr.test;

import java.net.URLClassLoader;

import org.commonjava.test.compile.CompilerFixture;
import org.commonjava.test.compile.CompilerFixtureConfig;
import org.commonjava.test.compile.CompilerResult;
import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.ApplicationRouterConfig;
import org.commonjava.vertx.vabr.anno.proc.RoutingAnnotationProcessor;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SimpleRoutingTest
{

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public HttpTestFixture fixture = new HttpTestFixture( folder );

    @Rule
    public CompilerFixture compiler = new CompilerFixture( folder );

    @Test
    public void singleSimpleAnnotatedRoute()
        throws Exception
    {
        final CompilerResult result =
            compiler.compileSourceDirWithThisClass( "simple-single-route",
                                                    "org.test.MyResource",
                                                    new CompilerFixtureConfig().withAnnotationProcessor( RoutingAnnotationProcessor.class ) );

        final URLClassLoader classLoader = result.getClassLoader();
        final Class<?> routesCls = classLoader.loadClass( "org.test.Routes" );
        final RouteCollection collection = (RouteCollection) routesCls.newInstance();

        final Class<?> handlerCls = classLoader.loadClass( "org.test.MyResource" );
        final Object handler = handlerCls.newInstance();

        final ApplicationRouter router =
            new ApplicationRouter( new ApplicationRouterConfig().withHandler( handler )
                                                                .withRouteCollection( collection ) );

        fixture.server()
               .setHandlerAndStart( router );

        fixture.get( "/my/name", 200 );
    }

}

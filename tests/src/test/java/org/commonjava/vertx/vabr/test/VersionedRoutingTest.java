package org.commonjava.vertx.vabr.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URLClassLoader;
import java.util.Collections;

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

public class VersionedRoutingTest
{

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public HttpTestFixture fixture = new HttpTestFixture( folder );

    @Rule
    public CompilerFixture compiler = new CompilerFixture( folder );

    @Test
    public void singlePathTwoVersions()
        throws Exception
    {
        final CompilerResult result =
            compiler.compileSourceDirWithThisClass( "versioned-path-routing",
                                                    "org.test.MyResource",
                                                    new CompilerFixtureConfig().withAnnotationProcessor( RoutingAnnotationProcessor.class ) );

        final URLClassLoader classLoader = result.getClassLoader();
        final Class<?> routesCls = classLoader.loadClass( "org.test.Routes" );
        final RouteCollection collection = (RouteCollection) routesCls.newInstance();

        final Class<?> handlerCls = classLoader.loadClass( "org.test.MyResource" );
        final RequestHandler handler = (RequestHandler) handlerCls.newInstance();

        final ApplicationRouter router =
            new ApplicationRouter( new ApplicationRouterConfig().withAppAcceptId( "app" )
                                                                .withDefaultVersion( "v1" )
                                                                .withHandler( handler )
                                                                .withRouteCollection( collection ) );

        fixture.server()
               .setHandlerAndStart( router );

        String name = fixture.get( "/my/name", 200 );

        System.out.println( name );
        assertThat( name, equalTo( "Hector" ) );

        name = fixture.get( "/my/name", Collections.singletonMap( "Accept", "application/app-v2+plain" ), 200 );

        System.out.println( name );
        assertThat( name, equalTo( "Ralph" ) );
    }

}

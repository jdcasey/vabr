package org.commonjava.vertx.vabr.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.vertx.vabr.testutil.PortFinder;
import org.commonjava.vertx.vabr.testutil.WaitHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

public class RoutingCriteriaTest
{

    private static final String ACCEPT = "Accept";

    private HttpServer server;

    private HttpClient client;

    private Vertx vertx;

    private RoutingCriteriaParser parser;

    private Handler<HttpClientResponse> clientHandler;

    @Before
    public void setup()
    {
        parser = new RoutingCriteriaParser();
        vertx = new DefaultVertx();

        final int port = PortFinder.findOpenPort();
        final String host = "127.0.0.1";
        server = vertx.createHttpServer()
                      .requestHandler( parser )
                      .listen( port, host );

        client = vertx.createHttpClient()
                      .setHost( host )
                      .setPort( port );

        clientHandler = new WaitHandler();
    }

    @After
    public void shutdown()
    {
        client.close();
        server.close();
    }

    @Test
    public void singleAcceptHeader()
        throws Exception
    {
        final RoutingCriteria criteria = parse( "text/html" );
        System.out.println( "Got criteria: " + criteria );
        final List<AcceptInfo> accepts = criteria.getAccepts();
        assertThat( accepts.size(), equalTo( 1 ) );

        final AcceptInfo accept = accepts.get( 0 );
        assertThat( accept.getBaseAccept(), equalTo( "text/html" ) );
    }

    @Test
    public void multiAcceptHeader()
        throws Exception
    {
        final RoutingCriteria criteria =
            parse( "text/html", "application/xhtml+xml", "application/xml;q=0.9", "*/*;q=0.8" );
        System.out.println( criteria );

        final List<AcceptInfo> infos = criteria.getAccepts();
        assertThat( infos.size(), equalTo( 4 ) );
    }

    @Test
    public void multiAcceptHeader_SingleHeaderString()
        throws Exception
    {
        final RoutingCriteria criteria = parse( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );
        System.out.println( criteria );

        final List<AcceptInfo> infos = criteria.getAccepts();
        assertThat( infos.size(), equalTo( 4 ) );
    }

    private RoutingCriteria parse( final String... types )
        throws Exception
    {
        final HttpClientRequest req = client.get( "/", clientHandler );
        for ( final String type : types )
        {
            req.headers()
               .add( ACCEPT, type );
        }

        req.end();

        synchronized ( clientHandler )
        {
            clientHandler.wait();
        }

        return parser.getParsed();
    }

    public static final class RoutingCriteriaParser
        implements Handler<HttpServerRequest>
    {

        private RoutingCriteria criteria;

        @Override
        public void handle( final HttpServerRequest event )
        {
            criteria = RoutingCriteria.parse( event, "api", "v1" );
            event.response()
                 .setStatusCode( 200 )
                 .setStatusMessage( "OK" )
                 .end();
        }

        public void clear()
        {
            criteria = null;
        }

        public RoutingCriteria getParsed()
        {
            return criteria;
        }
    }

}

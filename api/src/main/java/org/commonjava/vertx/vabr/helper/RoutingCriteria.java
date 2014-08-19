package org.commonjava.vertx.vabr.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.vertx.vabr.util.RouteHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public class RoutingCriteria
    implements Iterable<AcceptInfo>
{
    private static final Logger logger = LoggerFactory.getLogger( RoutingCriteria.class );

    public static final String ACCEPT_ANY = "*/*";

    public static final Set<String> ANY = Collections.unmodifiableSet( Collections.singleton( ACCEPT_ANY ) );

    private final Set<AcceptInfo> accepts;

    public static RoutingCriteria parse( final HttpServerRequest request, final String appId,
                                         final String defaultVersion )
    {
        final String header = request.headers()
                                        .get( RouteHeader.accept.header() );

        if ( header == null || header.trim()
                                     .length() < 1 )
        {
            return new RoutingCriteria(
                                        Collections.singleton( new AcceptInfo( ACCEPT_ANY, ACCEPT_ANY, defaultVersion ) ) );
        }

        final Set<String> raw = new HashSet<String>( Arrays.asList( header.split( "\\s*,\\s*" ) ) );
        final Set<String> rawAccept = new HashSet<String>( raw );
        for ( final String r : raw )
        {
            rawAccept.add( r.toLowerCase() );
        }

        final Set<AcceptInfo> acceptInfos = new HashSet<>();
        if ( !rawAccept.isEmpty() )
        {
            final String appPrefix = "application/" + appId + "-";
            for ( final String r : rawAccept )
            {
                logger.info( "Checking for ACCEPT header starting with: '{}' (header value is: '{}')", appPrefix,
                             rawAccept );
                if ( r.startsWith( appPrefix ) )
                {
                    final String[] parts = r.substring( appPrefix.length() )
                                            .split( "\\+" );

                    if ( parts.length > 1 )
                    {
                        acceptInfos.add( new AcceptInfo( r, "application/" + parts[1], parts[0] ) );
                    }
                }
                else
                {
                    acceptInfos.add( new AcceptInfo( r, r, defaultVersion ) );
                }
            }
        }

        return new RoutingCriteria( acceptInfos );
    }

    private RoutingCriteria( final Set<AcceptInfo> acceptInfos )
    {
        this.accepts = acceptInfos;
    }

    public Set<AcceptInfo> getAccepts()
    {
        return accepts;
    }

    @Override
    public Iterator<AcceptInfo> iterator()
    {
        return accepts.iterator();
    }

}

package org.commonjava.vertx.vabr.helper;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

    private final List<AcceptInfo> accepts;

    public static RoutingCriteria parse( final HttpServerRequest request, final String appId,
                                         final String defaultVersion )
    {
        final List<String> rawList = request.headers()
                                        .getAll( RouteHeader.accept.header() );

        final List<String> raw = new ArrayList<String>();
        for ( final String listed : rawList )
        {
            final String[] parts = listed.split( "\\s*,\\s*" );
            if ( parts.length == 1 )
            {
                logger.info( "adding atomic accept header: '{}'", listed );
                raw.add( listed );
            }
            else
            {
                logger.info( "Adding split header values: '{}'", join( parts, "', '" ) );
                raw.addAll( Arrays.asList( parts ) );
            }
        }

        logger.info( "Got raw ACCEPT header values:\n  {}", join( raw, "\n  " ) );

        if ( raw == null || raw.isEmpty() )
        {
            return new RoutingCriteria( Collections.singletonList( new AcceptInfo( ACCEPT_ANY, ACCEPT_ANY,
                                                                                   defaultVersion ) ) );
        }

        final List<AcceptInfo> acceptInfos = new ArrayList<AcceptInfo>();
        for ( final String r : raw )
        {
            String cleaned = r.toLowerCase();
            final int qIdx = cleaned.indexOf( ';' );
            if ( qIdx > -1 )
            {
                // FIXME: We shouldn't discard quality suffix...
                cleaned = cleaned.substring( 0, qIdx );
            }

            logger.info( "Cleaned up: {} to: {}", r, cleaned );

            final String appPrefix = "application/" + appId + "-";

            logger.info( "Checking for ACCEPT header starting with: '{}' and containing: '+' (header value is: '{}')",
                         appPrefix, cleaned );
            if ( cleaned.startsWith( appPrefix ) && cleaned.contains( "+" ) )
            {
                final String[] parts = cleaned.substring( appPrefix.length() )
                                              .split( "\\+" );

                acceptInfos.add( new AcceptInfo( cleaned, "application/" + parts[1], parts[0] ) );
            }
            else
            {
                acceptInfos.add( new AcceptInfo( cleaned, cleaned, defaultVersion ) );
            }
        }

        return new RoutingCriteria( acceptInfos );
    }

    private RoutingCriteria( final List<AcceptInfo> acceptInfos )
    {
        this.accepts = acceptInfos;
    }

    public List<AcceptInfo> getAccepts()
    {
        return accepts;
    }

    @Override
    public Iterator<AcceptInfo> iterator()
    {
        return accepts.iterator();
    }

    @Override
    public String toString()
    {
        return String.format( "RoutingCriteria: [\n  %s\n]", join( accepts, "\n  " ) );
    }

}

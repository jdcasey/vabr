package org.commonjava.vertx.vabr.helper;

import org.commonjava.vertx.vabr.util.RouteHeader;
import org.vertx.java.core.http.HttpServerRequest;

public class RoutingCriteria
{
    public static final String ACCEPT_ANY = "*/*";

    private final String rawAccept;

    private final String modifiedAccept;

    private final String version;

    public static RoutingCriteria parse( final HttpServerRequest request, final String appId,
                                         final String defaultVersion )
    {
        final String rawAccept = request.headers()
                                        .get( RouteHeader.accept.header() );

        if ( rawAccept == null )
        {
            return new RoutingCriteria( null, null, defaultVersion );
        }
        else if ( rawAccept.equals( ACCEPT_ANY ) )
        {
            return new RoutingCriteria( rawAccept, rawAccept, defaultVersion );
        }

        String modifiedAccept = rawAccept;
        String version = defaultVersion;
        if ( rawAccept != null )
        {
            final String appPrefix = "application/" + appId + "-";
            if ( rawAccept.startsWith( appPrefix ) )
            {
                final String[] parts = rawAccept.substring( appPrefix.length() )
                                                .split( "\\+" );
                if ( parts.length > 1 )
                {
                    version = parts[0];
                    modifiedAccept = "application/" + parts[1];
                }
            }
        }

        return new RoutingCriteria( rawAccept, modifiedAccept, version );
    }

    private RoutingCriteria( final String rawAccept, final String modifiedAccept, final String version )
    {
        this.rawAccept = rawAccept;
        this.modifiedAccept = modifiedAccept;
        this.version = version;
    }

    public String getRawAccept()
    {
        return rawAccept;
    }

    public String getModifiedAccept()
    {
        return modifiedAccept;
    }

    public String getVersion()
    {
        return version;
    }

}

package org.commonjava.vertx.vabr.util;

public enum RouteHeader
{
    recommended_content_type( "VABR-Recommended-Content-Type" ), accept( "Accept" ), recommended_content_version(
        "VABR-Recommended-Content-Version" );

    private final String header;

    private RouteHeader( final String header )
    {
        this.header = header;
    }

    public String header()
    {
        return header;
    }

}

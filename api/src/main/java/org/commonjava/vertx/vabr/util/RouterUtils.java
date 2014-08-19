/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.vertx.vabr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public final class RouterUtils
{

    private RouterUtils()
    {
    }

    public static String requestUri( final HttpServerRequest request )
    {
        final String hostHeader = request.headers()
                                         .get( "Host" );

        String hostAndPort = request.absoluteURI()
                                    .getHost();

        final int port = request.absoluteURI()
                                .getPort();
        if ( port != 80 && port != 443 )
        {
            hostAndPort += ":" + port;
        }

        final String uri = request.absoluteURI()
                                  .toString();

        final int idx = uri.indexOf( hostAndPort );

        final StringBuilder sb = new StringBuilder();
        sb.append( uri.substring( 0, idx ) );
        sb.append( hostHeader );
        sb.append( uri.substring( idx + hostAndPort.length() ) );

        return sb.toString();
    }

    public static String trimPrefix( final String prefix, String path )
    {
        if ( prefix != null && prefix.length() > 0 )
        {
            String p = prefix;
            if ( !p.startsWith( "/" ) )
            {
                p = "/" + p;
            }

            if ( !path.startsWith( p ) )
            {
                return null;
            }
            else
            {
                final Logger logger = LoggerFactory.getLogger( RouterUtils.class );
                logger.info( "Trimming off: '{}'", path.substring( 0, p.length() ) );
                path = path.substring( p.length() );
            }
        }

        //        if ( path.length() < 1 )
        //        {
        //            path = "/";
        //        }

        return path;
    }

}

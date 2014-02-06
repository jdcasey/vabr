/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.vertx.vabr.anno.proc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.types.BindingType;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

public final class RoutingTemplateInfo
    extends AbstractTemplateInfo
{

    private final String httpContentType;

    private final BindingType binding;

    private final String routeKey;

    private final List<String> callParams;

    public RoutingTemplateInfo( final Element elem, final Route route, final Handles handles )
    {
        super( elem, handles, route.priority(), route.method(), route.path(), route.value() );
        this.httpContentType = route.contentType();
        this.binding = route.binding();
        this.routeKey = route.routeKey();

        if ( binding == BindingType.raw )
        {
            callParams = Collections.singletonList( "request" );
        }
        else
        {
            final ExecutableElement ee = (ExecutableElement) elem;
            final List<? extends VariableElement> parameters = ee.getParameters();
            callParams = new ArrayList<>();
            System.out.println( "Found " + parameters.size() + " parameters" );
            for ( final VariableElement param : parameters )
            {
                final String typeStr = param.asType()
                                            .toString();
                System.out.println( "Found parameter of type: " + typeStr );
                if ( typeStr.equals( Buffer.class.getName() ) )
                {
                    callParams.add( "body" );
                }
                else if ( typeStr.equals( HttpServerRequest.class.getName() ) )
                {
                    callParams.add( "request" );
                }
            }
        }
    }

    public List<String> getCallParams()
    {
        return callParams;
    }

    public String getHttpContentType()
    {
        return httpContentType;
    }

    public BindingType getBinding()
    {
        return binding;
    }

    public String getRouteKey()
    {
        return routeKey;
    }
}

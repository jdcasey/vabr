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
package org.commonjava.vertx.vabr.bind.filter;

import java.util.List;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.bind.AbstractBinding;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class FilterBinding
    extends AbstractBinding
    implements Comparable<FilterBinding>
{

    protected FilterBinding( final int priority, final String path, final Method method, final String handlerKey,
                             final List<String> versions )
    {
        super( priority, path, method, handlerKey, versions );
    }

    @Override
    public String toString()
    {
        return "Filter [" + method.name() + " " + path + "] => " + handlerKey;
    }

    public void handle( final ApplicationRouter router, final HttpServerRequest req, final ExecutionChain next )
        throws Exception
    {
        dispatch( router, req, next );
    }

    protected abstract void dispatch( ApplicationRouter router, HttpServerRequest req, ExecutionChain next )
        throws Exception;

    @Override
    public int compareTo( final FilterBinding other )
    {
        return Integer.valueOf( priority )
                      .compareTo( other.priority );
    }

}

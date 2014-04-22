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
package org.commonjava.vertx.vabr.helper;

import java.util.regex.Matcher;

public class BindingContext
{
    private final Matcher matcher;

    private final PatternRouteBinding routeBinding;

    private final PatternFilterBinding filterBinding;

    public BindingContext( final Matcher matcher, final PatternRouteBinding routeBinding, final PatternFilterBinding filterBinding )
    {
        this.matcher = matcher;
        this.routeBinding = routeBinding;
        this.filterBinding = filterBinding;
    }

    public Matcher getMatcher()
    {
        return matcher;
    }

    public PatternRouteBinding getRouteBinding()
    {
        return routeBinding;
    }

    public PatternFilterBinding getFilterBinding()
    {
        return filterBinding;
    }
}

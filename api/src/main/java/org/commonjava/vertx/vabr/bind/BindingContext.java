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
package org.commonjava.vertx.vabr.bind;

import java.util.regex.Matcher;

public class BindingContext
{
    private final Matcher matcher;

    private final PatternRouteBinding patternRouteBinding;

    private final PatternFilterBinding patternFilterBinding;

    public BindingContext( final Matcher matcher, final PatternRouteBinding routeBinding, final PatternFilterBinding filterBinding )
    {
        this.matcher = matcher;
        this.patternRouteBinding = routeBinding;
        this.patternFilterBinding = filterBinding;
    }

    public Matcher getMatcher()
    {
        return matcher;
    }

    public PatternRouteBinding getPatternRouteBinding()
    {
        return patternRouteBinding;
    }

    public PatternFilterBinding getPatternFilterBinding()
    {
        return patternFilterBinding;
    }
}

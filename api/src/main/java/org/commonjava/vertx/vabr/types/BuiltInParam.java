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
package org.commonjava.vertx.vabr.types;

public enum BuiltInParam
{

    _classContextUrl, _classBase, _routeContextUrl, _routeBase;

    private String key;

    private BuiltInParam( final String key )
    {
        this.key = key;
    }

    private BuiltInParam()
    {
        this.key = null;
    }

    public String key()
    {
        return key == null ? name() : key;
    }

}

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
package org.commonjava.vertx.vabr.anno.proc;

import javax.lang.model.element.Element;

public class QualifierInfo
{

    public static final QualifierInfo EMPTY_QUALIFIER = new QualifierInfo( "", "" );

    private final String simpleName;

    private final String fullName;

    public QualifierInfo( final String simpleName, final String fullName )
    {
        this.simpleName = simpleName.trim();
        this.fullName = fullName.trim();
    }

    public QualifierInfo( final Element elem )
    {
        this.simpleName = elem.getSimpleName()
                              .toString()
                              .trim();

        this.fullName = elem.asType()
                            .toString()
                            .trim();
    }

    public String getSimpleName()
    {
        return simpleName;
    }

    public String getFullName()
    {
        return fullName;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( fullName == null ) ? 0 : fullName.hashCode() );
        result = prime * result + ( ( simpleName == null ) ? 0 : simpleName.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final QualifierInfo other = (QualifierInfo) obj;
        if ( fullName == null )
        {
            if ( other.fullName != null )
            {
                return false;
            }
        }
        else if ( !fullName.equals( other.fullName ) )
        {
            return false;
        }
        if ( simpleName == null )
        {
            if ( other.simpleName != null )
            {
                return false;
            }
        }
        else if ( !simpleName.equals( other.simpleName ) )
        {
            return false;
        }
        return true;
    }

}

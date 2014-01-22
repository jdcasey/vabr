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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.PathPrefix;
import org.commonjava.vertx.vabr.anno.Route;

public final class RoutingTemplateInfo
{

    private final String packagename;

    private final String qualifiedClassname;

    private final String classname;

    private final String methodname;

    private final Method httpMethod;

    private final String httpPath;

    private final String httpContentType;

    //    public RoutingTemplateInfo( final String packagename, final String qualifiedClassname, final String classname, final String methodname,
    //                                final Route route )
    //    {
    //        this.packagename = packagename;
    //        this.qualifiedClassname = qualifiedClassname;
    //        this.classname = classname;
    //        this.methodname = methodname;
    //        this.httpMethod = route.method();
    //        this.httpPath = route.path();
    //        this.httpContentType = route.contentType();
    //    }
    //
    public RoutingTemplateInfo( final Element elem, final Route route, final PathPrefix prefix )
    {
        this.httpMethod = route.method();
        this.httpPath = pathOf( prefix, route );
        this.httpContentType = route.contentType();
        // it only applies to methods...
        final ExecutableElement eelem = (ExecutableElement) elem;

        methodname = eelem.getSimpleName()
                          .toString();

        final TypeElement cls = (TypeElement) eelem.getEnclosingElement();
        final PackageElement pkg = (PackageElement) cls.getEnclosingElement();

        qualifiedClassname = cls.getQualifiedName()
                                .toString();
        classname = cls.getSimpleName()
                       .toString();
        packagename = pkg.getQualifiedName()
                         .toString();
    }

    private String pathOf( final PathPrefix prefix, final Route route )
    {
        final StringBuilder sb = new StringBuilder();
        if ( prefix != null )
        {
            String pfx = prefix.value();
            if ( pfx.endsWith( "/" ) )
            {
                pfx = pfx.substring( 0, pfx.length() - 1 );
            }

            sb.append( pfx );
        }

        String suff = null;
        if ( route.path() != null && route.path()
                                          .trim()
                                          .length() > 0 )
        {
            suff = route.path();
        }
        else if ( route.value() != null && route.value()
                                                .trim()
                                                .length() > 0 )
        {
            suff = route.value();
        }

        if ( suff != null )
        {
            if ( !suff.startsWith( "/" ) && suff.length() > 0 )
            {
                sb.append( "/" );
            }

            sb.append( suff );
        }

        return sb.toString();
    }

    public String getPackagename()
    {
        return packagename;
    }

    public String getQualifiedClassname()
    {
        return qualifiedClassname;
    }

    public String getClassname()
    {
        return classname;
    }

    public String getMethodname()
    {
        return methodname;
    }

    public Method getHttpMethod()
    {
        return httpMethod;
    }

    public String getHttpPath()
    {
        return httpPath;
    }

    public String getHttpContentType()
    {
        return httpContentType;
    }

}

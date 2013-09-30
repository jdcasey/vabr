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
package org.commonjava.freeki.infra.route.anno.proc;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.commonjava.freeki.infra.route.Method;
import org.commonjava.freeki.infra.route.anno.Route;

public final class RoutingTemplateInfo
{

    private final String packagename;

    private final String qualifiedClassname;

    private final String classname;

    private final String methodname;

    private final Method httpMethod;

    private final String httpPath;

    private final String httpContentType;

    public RoutingTemplateInfo( final String packagename, final String qualifiedClassname, final String classname,
                                final String methodname, final Route route )
    {
        this.packagename = packagename;
        this.qualifiedClassname = qualifiedClassname;
        this.classname = classname;
        this.methodname = methodname;
        this.httpMethod = route.method();
        this.httpPath = route.path();
        this.httpContentType = route.contentType();
    }

    public RoutingTemplateInfo( final Element elem, final Route route )
    {
        this.httpMethod = route.method();
        this.httpPath = route.path();
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

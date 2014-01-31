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
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.util.AnnotationUtils;

public abstract class AbstractTemplateInfo
{

    private final int priority;

    private final String packagename;

    private final String qualifiedClassname;

    private final String classname;

    private final String methodname;

    private final Method httpMethod;

    private final String httpPath;

    private final String handlerKey;

    protected AbstractTemplateInfo( final Element elem, final Handles handles, final int priority, final Method method, final String path,
                                    final String defPath )
    {
        this.priority = priority;
        this.httpMethod = method;
        this.httpPath = AnnotationUtils.pathOf( handles, path, defPath );
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

        this.handlerKey = AnnotationUtils.getHandlerKey( handles, qualifiedClassname );
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

    public int getPriority()
    {
        return priority;
    }

    public String getHandlerKey()
    {
        return handlerKey;
    }

}

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.types.Method;
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

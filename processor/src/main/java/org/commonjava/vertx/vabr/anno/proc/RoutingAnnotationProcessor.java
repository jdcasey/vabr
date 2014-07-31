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

import static org.commonjava.vertx.vabr.anno.proc.QualifierInfo.EMPTY_QUALIFIER;
import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Qualifier;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.codehaus.groovy.control.CompilationFailedException;
import org.commonjava.vertx.vabr.anno.FilterRoute;
import org.commonjava.vertx.vabr.anno.FilterRoutes;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;

/* @formatter:off */
@SupportedAnnotationTypes( { 
    "org.commonjava.vertx.vabr.anno.FilterRoutes",
    "org.commonjava.vertx.vabr.anno.FilterRoute",
    "org.commonjava.vertx.vabr.anno.Routes",
    "org.commonjava.vertx.vabr.anno.Route"
} )
/* @formatter:on */
@SupportedSourceVersion( SourceVersion.RELEASE_7 )
public class RoutingAnnotationProcessor
    extends AbstractProcessor
{

    public static final String TEMPLATE_PKG = "groovy";

    public static final String ROUTE_TEMPLATE = "routes.groovy";

    public static final String FILTER_TEMPLATE = "filters.groovy";

    final GStringTemplateEngine engine = new GStringTemplateEngine();

    private String pkg = null;

    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv )
    {
        //        if ( annotations.isEmpty() )
        //        {
        //            return true;
        //        }

        System.out.println( "Starting Route Processing..." );

        final Map<QualifierInfo, Set<RoutingTemplateInfo>> routingTemplates = processRoutes( roundEnv );
        final Map<QualifierInfo, Set<FilteringTemplateInfo>> filteringTemplates = processFilters( roundEnv );

        System.out.printf( "Using package: %s\n", pkg );

        if ( !routingTemplates.isEmpty() )
        {
            generateOutput( pkg, "Routes", ROUTE_TEMPLATE, RouteCollection.class, routingTemplates, roundEnv );
        }

        if ( !filteringTemplates.isEmpty() )
        {
            generateOutput( pkg, "Filters", FILTER_TEMPLATE, FilterCollection.class, filteringTemplates, roundEnv );
        }

        return true;
    }

    private Map<QualifierInfo, Set<RoutingTemplateInfo>> processRoutes( final RoundEnvironment roundEnv )
    {
        final Map<QualifierInfo, Set<RoutingTemplateInfo>> routingTemplates = new HashMap<>();

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( Routes.class ) )
        {
            System.out.printf( "Processing: %s\n", elem );

            final Handles handles = findTypeAnnotation( elem, Handles.class );

            pkg = selectShortestPackage( pkg, elem );

            final Routes routes = elem.getAnnotation( Routes.class );
            if ( routes != null )
            {
                for ( final Route route : routes.value() )
                {
                    final QualifierInfo key = findQualifierAnnotation( elem );
                    addTo( routingTemplates, new RoutingTemplateInfo( elem, route, handles ), key );
                }
            }
        }

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( Route.class ) )
        {
            final Handles handles = findTypeAnnotation( elem, Handles.class );
            final Route route = elem.getAnnotation( Route.class );
            pkg = selectShortestPackage( pkg, elem );

            final QualifierInfo key = findQualifierAnnotation( elem );
            addTo( routingTemplates, new RoutingTemplateInfo( elem, route, handles ), key );
        }

        return routingTemplates;
    }

    private <T> void addTo( final Map<QualifierInfo, Set<T>> templates, final T template, final QualifierInfo qi )
    {
        Set<T> routes = templates.get( qi );
        if ( routes == null )
        {
            routes = new HashSet<>();
            templates.put( qi, routes );
        }

        routes.add( template );
    }

    private QualifierInfo findQualifierAnnotation( final Element elem )
    {
        Element pe = elem;
        do
        {
            pe = pe.getEnclosingElement();
        }
        while ( pe != null && pe.getKind() != ElementKind.CLASS );

        final List<? extends AnnotationMirror> ams = pe.getAnnotationMirrors();
        for ( final AnnotationMirror am : ams )
        {
            final Element annoElem = am.getAnnotationType()
                                       .asElement();

            final Qualifier qualifier = annoElem.getAnnotation( Qualifier.class );
            if ( qualifier != null )
            {
                return new QualifierInfo( annoElem );
            }
        }

        return EMPTY_QUALIFIER;
    }

    private Map<QualifierInfo, Set<FilteringTemplateInfo>> processFilters( final RoundEnvironment roundEnv )
    {
        final Map<QualifierInfo, Set<FilteringTemplateInfo>> filteringTemplates = new HashMap<>();

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( FilterRoutes.class ) )
        {
            System.out.printf( "Processing: %s\n", elem );

            final Handles handles = findTypeAnnotation( elem, Handles.class );

            pkg = selectShortestPackage( pkg, elem );

            final FilterRoutes filters = elem.getAnnotation( FilterRoutes.class );
            if ( filters != null )
            {
                for ( final FilterRoute filter : filters.value() )
                {
                    final QualifierInfo key = findQualifierAnnotation( elem );
                    addTo( filteringTemplates, new FilteringTemplateInfo( elem, filter, handles ), key );
                }
            }
        }

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( FilterRoute.class ) )
        {
            final Handles handles = findTypeAnnotation( elem, Handles.class );
            final FilterRoute filter = elem.getAnnotation( FilterRoute.class );

            final QualifierInfo key = findQualifierAnnotation( elem );
            addTo( filteringTemplates, new FilteringTemplateInfo( elem, filter, handles ), key );
            pkg = selectShortestPackage( pkg, elem );
        }

        return filteringTemplates;
    }

    private <T extends Annotation> T findTypeAnnotation( final Element elem, final Class<T> annoCls )
    {
        Element pe = elem;
        do
        {
            pe = pe.getEnclosingElement();
        }
        while ( pe != null && pe.getKind() != ElementKind.CLASS );

        return pe.getAnnotation( annoCls );
    }

    private String selectShortestPackage( final String pkg, final Element elem )
    {
        Element pe = elem;
        do
        {
            pe = pe.getEnclosingElement();
        }
        while ( pe != null && pe.getKind() != ElementKind.PACKAGE );

        final String p = ( (PackageElement) pe ).getQualifiedName()
                                                .toString();

        if ( pkg == null || p.length() < pkg.length() )
        {
            System.out.printf( "Setting package: %s\n", p );
            return p;
        }

        return pkg;
    }

    private <T extends AbstractTemplateInfo> void generateOutput( final String pkg, final String simpleClassSuffix, final String codeTemplateName,
                                                                  final Class<?> collectionClass, final Map<QualifierInfo, Set<T>> templates,
                                                                  final RoundEnvironment roundEnv )
    {
        Template template;
        try
        {
            final FileObject resource = processingEnv.getFiler()
                                                     .getResource( StandardLocation.CLASS_PATH, TEMPLATE_PKG, codeTemplateName );

            if ( resource == null )
            {
                throw new IllegalStateException( "Cannot find route template: " + codeTemplateName );
            }

            template = engine.createTemplate( resource.toUri()
                                                      .toURL() );
        }
        catch ( CompilationFailedException | ClassNotFoundException | IOException e )
        {
            throw new IllegalStateException( "Cannot load template: " + TEMPLATE_PKG + "/" + codeTemplateName + ". Reason: " + e.getMessage(), e );
        }

        System.out.printf( "Package: %s\n", pkg );

        final Filer filer = processingEnv.getFiler();
        final Set<String> generatedClassNames = new HashSet<>();

        for ( final Entry<QualifierInfo, Set<T>> entry : templates.entrySet() )
        {
            final QualifierInfo key = entry.getKey();
            final Set<?> tmpls = entry.getValue();

            final Map<String, Object> params = new HashMap<>();
            params.put( "pkg", pkg );
            params.put( "templates", tmpls );

            params.put( "qualifier", EMPTY_QUALIFIER != key ? key : null );

            final Writable output = template.make( params );

            String simpleName = "";
            if ( key != EMPTY_QUALIFIER )
            {
                simpleName += key.getSimpleName();
            }
            simpleName += simpleClassSuffix;

            params.put( "className", simpleName );

            final String clsName = pkg + "." + simpleName;

            System.out.printf( "Generating class: %s\n", clsName );

            Writer sourceWriter = null;
            try
            {
                final FileObject file = filer.createSourceFile( clsName );

                sourceWriter = file.openWriter();
                output.writeTo( sourceWriter );
                generatedClassNames.add( clsName );
            }
            catch ( final IOException e )
            {
                processingEnv.getMessager()
                             .printMessage( Kind.ERROR, "While generating sources for class: '" + clsName + "', error: " + e.getMessage() );
            }
            finally
            {
                if ( sourceWriter != null )
                {
                    try
                    {
                        sourceWriter.close();
                    }
                    catch ( final IOException e )
                    {
                    }
                }
            }
        }

        final String resName = "META-INF/services/" + collectionClass.getName();

        Writer svcWriter = null;
        try
        {
            final FileObject file = filer.createResource( StandardLocation.SOURCE_OUTPUT, "", resName, (Element[]) null );
            System.out.printf( "Generating templates class service entry for:\n  %s\n\nin: %s\n", join( generatedClassNames, "\n  " ), file.toUri() );
            svcWriter = file.openWriter();
            svcWriter.write( join( generatedClassNames, "\n" ) );
        }
        catch ( final IOException e )
        {
            processingEnv.getMessager()
                         .printMessage( Kind.ERROR,
                                        "While generating service configuration for templates class: '" + resName + "', error: " + e.getMessage() );
        }
        finally
        {
            if ( svcWriter != null )
            {
                try
                {
                    svcWriter.close();
                }
                catch ( final IOException e )
                {
                }
            }
        }

    }

    private String join( final Iterable<?> objects, final String joint )
    {
        final StringBuilder sb = new StringBuilder();
        for ( final Object obj : objects )
        {
            if ( sb.length() > 0 )
            {
                sb.append( joint );
            }
            sb.append( obj );
        }

        return sb.toString();
    }

}

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

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
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
import org.commonjava.vertx.vabr.anno.PathPrefix;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.route.RouteCollection;

/* @formatter:off */
@SupportedAnnotationTypes( { 
    "org.commonjava.vertx.vabr.anno.FilterRoutes",
    "org.commonjava.vertx.vabr.anno.FilterRoute",
    "org.commonjava.vertx.vabr.anno.PathPrefix",
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

        final Set<RoutingTemplateInfo> routingTemplates = processRoutes( roundEnv );
        final Set<FilteringTemplateInfo> filteringTemplates = processFilters( roundEnv );

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

    private Set<RoutingTemplateInfo> processRoutes( final RoundEnvironment roundEnv )
    {
        final Set<RoutingTemplateInfo> routingTemplates = new HashSet<>();

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( Routes.class ) )
        {
            System.out.printf( "Processing: %s\n", elem );

            final PathPrefix prefix = findPathPrefix( elem );

            pkg = selectShortestPackage( pkg, elem );

            final Routes routes = elem.getAnnotation( Routes.class );
            if ( routes != null )
            {
                for ( final Route route : routes.value() )
                {
                    routingTemplates.add( new RoutingTemplateInfo( elem, route, prefix ) );
                }
            }
        }

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( Route.class ) )
        {
            final PathPrefix prefix = findPathPrefix( elem );
            final Route route = elem.getAnnotation( Route.class );

            routingTemplates.add( new RoutingTemplateInfo( elem, route, prefix ) );
            pkg = selectShortestPackage( pkg, elem );
        }

        return routingTemplates;
    }

    private Set<FilteringTemplateInfo> processFilters( final RoundEnvironment roundEnv )
    {
        final Set<FilteringTemplateInfo> filteringTemplates = new HashSet<>();

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( FilterRoutes.class ) )
        {
            System.out.printf( "Processing: %s\n", elem );

            final PathPrefix prefix = findPathPrefix( elem );

            pkg = selectShortestPackage( pkg, elem );

            final FilterRoutes filters = elem.getAnnotation( FilterRoutes.class );
            if ( filters != null )
            {
                for ( final FilterRoute filter : filters.value() )
                {
                    filteringTemplates.add( new FilteringTemplateInfo( elem, filter, prefix ) );
                }
            }
        }

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( FilterRoute.class ) )
        {
            final PathPrefix prefix = findPathPrefix( elem );
            final FilterRoute filter = elem.getAnnotation( FilterRoute.class );

            filteringTemplates.add( new FilteringTemplateInfo( elem, filter, prefix ) );
            pkg = selectShortestPackage( pkg, elem );
        }

        return filteringTemplates;
    }

    private PathPrefix findPathPrefix( final Element elem )
    {
        Element pe = elem;
        do
        {
            pe = pe.getEnclosingElement();
        }
        while ( pe != null && pe.getKind() != ElementKind.CLASS );

        return pe.getAnnotation( PathPrefix.class );
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

    private void generateOutput( final String pkg, final String simpleClassName, final String templateName, final Class<?> collectionClass,
                                 final Set<?> templates, final RoundEnvironment roundEnv )
    {
        Template template;
        try
        {
            final FileObject resource = processingEnv.getFiler()
                                                     .getResource( StandardLocation.CLASS_PATH, TEMPLATE_PKG, templateName );

            if ( resource == null )
            {
                throw new IllegalStateException( "Cannot find route template: " + templateName );
            }

            template = engine.createTemplate( resource.toUri()
                                                      .toURL() );
        }
        catch ( CompilationFailedException | ClassNotFoundException | IOException e )
        {
            throw new IllegalStateException( "Cannot load template: " + TEMPLATE_PKG + "/" + templateName + ". Reason: " + e.getMessage(), e );
        }

        System.out.printf( "Package: %s\n", pkg );
        final Map<String, Object> params = new HashMap<>();
        params.put( "pkg", pkg );
        params.put( "templates", templates );
        final Writable output = template.make( params );

        final String clsName = pkg + "." + simpleClassName;
        System.out.printf( "Generating class: %s\n", clsName );

        final Filer filer = processingEnv.getFiler();
        Writer sourceWriter = null;
        try
        {
            final FileObject file = filer.createSourceFile( clsName );

            sourceWriter = file.openWriter();
            output.writeTo( sourceWriter );
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

        final String resName = "META-INF/services/" + collectionClass.getName();

        Writer svcWriter = null;
        try
        {
            final FileObject file = filer.createResource( StandardLocation.SOURCE_OUTPUT, "", resName, (Element[]) null );
            System.out.printf( "Generating templates class service entry for: %s in: %s\n", clsName, file.toUri() );
            svcWriter = file.openWriter();
            svcWriter.write( clsName );
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

}

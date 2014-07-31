package org.commonjava.vertx.vabr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class ApplicationRouterConfig
{
    private List<RouteCollection> routeCollections = new ArrayList<>();

    private List<FilterCollection> filterCollections = new ArrayList<>();

    private Set<Object> handlers = new HashSet<>();

    private Handler<HttpServerRequest> noMatchHandler;

    private String prefix;

    private String appAcceptId = "api";

    private String defaultVersion = "v1";

    private ExecutorService handlerExecutor;

    public Handler<HttpServerRequest> getNoMatchHandler()
    {
        return noMatchHandler;
    }

    public ApplicationRouterConfig withNoMatchHandler( final Handler<HttpServerRequest> noMatchHandler )
    {
        this.noMatchHandler = noMatchHandler;
        return this;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public ApplicationRouterConfig withPrefix( final String prefix )
    {
        this.prefix = prefix;
        return this;
    }

    public String getAppAcceptId()
    {
        return appAcceptId;
    }

    public ApplicationRouterConfig withAppAcceptId( final String appAcceptId )
    {
        this.appAcceptId = appAcceptId;
        return this;
    }

    public String getDefaultVersion()
    {
        return defaultVersion;
    }

    public ApplicationRouterConfig withDefaultVersion( final String defaultVersion )
    {
        this.defaultVersion = defaultVersion;
        return this;
    }

    public ExecutorService getHandlerExecutor()
    {
        return handlerExecutor == null ? Executors.newCachedThreadPool() : handlerExecutor;
    }

    public ApplicationRouterConfig withHandlerExecutor( final ExecutorService handlerExecutor )
    {
        this.handlerExecutor = handlerExecutor;
        return this;
    }

    public List<RouteCollection> getRouteCollections()
    {
        return routeCollections;
    }

    public ApplicationRouterConfig withRouteCollections( final List<RouteCollection> routeCollections )
    {
        this.routeCollections = routeCollections;
        return this;
    }

    public List<FilterCollection> getFilterCollections()
    {
        return filterCollections;
    }

    public ApplicationRouterConfig withFilterCollections( final List<FilterCollection> filterCollections )
    {
        this.filterCollections = filterCollections;
        return this;
    }

    public Set<Object> getHandlers()
    {
        return handlers;
    }

    public ApplicationRouterConfig withHandlers( final Set<Object> handlers )
    {
        this.handlers = handlers;
        return this;
    }

    public ApplicationRouterConfig withHandler( final Object handler )
    {
        this.handlers.add( handler );
        return this;
    }

    public ApplicationRouterConfig withRouteCollection( final RouteCollection collection )
    {
        routeCollections.add( collection );
        return this;
    }
    
    public ApplicationRouterConfig withFilterCollection( final FilterCollection collection )
    {
        filterCollections.add( collection );
        return this;
    }

}

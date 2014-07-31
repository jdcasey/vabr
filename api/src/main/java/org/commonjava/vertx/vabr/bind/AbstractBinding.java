package org.commonjava.vertx.vabr.bind;

import java.util.Collections;
import java.util.List;

import org.commonjava.vertx.vabr.types.Method;

public abstract class AbstractBinding
{
    protected final String path;

    protected final Method method;

    protected final int priority;

    protected final String handlerKey;

    protected final List<String> versions;

    protected AbstractBinding( final int priority, final String path, final Method method, final String handlerKey,
                               final List<String> versions )
    {
        this.priority = priority;
        this.path = path;
        this.method = method;
        this.handlerKey = handlerKey;
        this.versions = Collections.unmodifiableList( versions );
    }

    public final List<String> getVersions()
    {
        return versions;
    }

    public final int getPriority()
    {
        return priority;
    }

    public final String getPath()
    {
        return path;
    }

    public final Method getMethod()
    {
        return method;
    }

    public final String getHandlerKey()
    {
        return handlerKey;
    }
}

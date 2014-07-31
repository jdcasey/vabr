package org.commonjava.vertx.vabr.bind;

import org.commonjava.vertx.vabr.types.Method;

public class BindingKey
{

    private final Method method;

    private final String version;

    public BindingKey( final Method method, final String version )
    {
        super();
        this.method = method;
        this.version = version;
    }

    public Method getMethod()
    {
        return method;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( method == null ) ? 0 : method.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
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
        final BindingKey other = (BindingKey) obj;
        if ( method != other.method )
        {
            return false;
        }
        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "BindingKey [method=%s, version=%s]", method, version );
    }

}

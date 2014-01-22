package org.commonjava.vertx.vabr;

public enum BuiltInParam
{

    _classBase, _routeBase;

    private String key;

    private BuiltInParam( final String key )
    {
        this.key = key;
    }

    private BuiltInParam()
    {
        this.key = null;
    }

    public String key()
    {
        return key == null ? name() : key;
    }

}

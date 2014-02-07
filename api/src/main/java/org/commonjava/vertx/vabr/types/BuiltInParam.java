package org.commonjava.vertx.vabr.types;

public enum BuiltInParam
{

    _classContextUrl, _classBase, _routeContextUrl, _routeBase;

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

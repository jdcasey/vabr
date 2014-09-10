package org.commonjava.vertx.vabr.types;

public enum ContentType
{

    application_json, application_xml, text_plain, text_html;

    public String value()
    {
        return name().replace( '_', '/' );
    }

}

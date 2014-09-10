package org.commonjava.vertx.vabr.util;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;

import org.commonjava.vertx.vabr.types.ApplicationHeader;
import org.commonjava.vertx.vabr.types.ApplicationStatus;
import org.commonjava.vertx.vabr.types.ContentType;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Respond
{

    private final HttpServerResponse response;

    private ApplicationStatus status = ApplicationStatus.BAD_REQUEST;

    private Object entity;

    private String contentType;

    private Respond( final HttpServerResponse response )
    {
        this.response = response;
    }

    public static Respond to( final HttpServerRequest request )
    {
        return new Respond( request.response() );
    }

    public Respond ok()
    {
        status = ApplicationStatus.OK;
        return this;
    }

    public Respond jsonEntity( final Object entity )
        throws JsonProcessingException
    {
        this.contentType = ContentType.application_json.value();
        this.entity = getJson().writeValueAsString( entity );

        return this;
    }

    public Respond entity( final Object entity )
    {
        this.entity = entity;
        return this;
    }

    public Respond type( final String contentType )
    {
        this.contentType = contentType;
        return this;
    }

    public Respond type( final ContentType type )
    {
        this.contentType = type.value();
        return this;
    }

    // FIXME: Allow configuration.
    private ObjectMapper getJson()
    {
        return new ObjectMapper();
    }

    public void send()
    {
        response.setStatusCode( status.code() ).setStatusMessage( status.message() );
        String content = null;
        if ( entity != null )
        {
            if ( entity instanceof File )
            {
                response.sendFile( ( (File) entity ).getAbsolutePath() );
                return;
            }

            content = entity.toString();
        }

        if ( content != null )
        {
            response.putHeader( ApplicationHeader.content_length.key(), Integer.toString( content.length() ) );

            if ( contentType != null )
            {
                response.putHeader( ApplicationHeader.content_type.key(), contentType );
            }

            response.write( content );
        }

        response.end();
    }

    public Respond status( final ApplicationStatus status )
    {
        this.status = status;
        return this;
    }

    public Respond serverError( final Throwable error, final boolean printStackTrace )
    {
        this.status = ApplicationStatus.SERVER_ERROR;
        this.contentType = ContentType.text_plain.value();

        if ( printStackTrace )
        {
            this.entity = String.format( "%s:\n  %s", error.getMessage(), join( error.getStackTrace(), "\n  " ) );
        }
        else
        {
            this.entity = "An internal server error has occurred. Please contact this application's administrator";
        }

        return this;
    }

}
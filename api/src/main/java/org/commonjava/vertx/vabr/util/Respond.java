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

    private ApplicationStatus status;

    private Object entity;

    private String contentType;

    private final HttpServerRequest request;

    private Respond( final HttpServerRequest request )
    {
        this.request = request;
    }

    public static Respond to( final HttpServerRequest request )
    {
        return new Respond( request );
    }

    public Respond ok()
    {
        status = ApplicationStatus.OK;
        return this;
    }

    public Respond jsonEntity( final Object entity )
        throws JsonProcessingException
    {
        return jsonEntity( entity, new ObjectMapper() );
    }

    public Respond jsonEntity( final Object entity, final ObjectMapper objectMapper )
        throws JsonProcessingException
    {
        if ( status == null )
        {
            status = ApplicationStatus.OK;
        }

        this.contentType = ContentType.application_json.value();
        this.entity = objectMapper.writeValueAsString( entity );

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

    public void send()
    {
        if ( status == null )
        {
            status = ApplicationStatus.BAD_REQUEST;
        }

        final HttpServerResponse response = request.resume()
                                                   .response();

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

        try
        {
            if ( content != null )
            {
                response.putHeader( ApplicationHeader.content_length.key(), Integer.toString( content.length() ) );

                if ( contentType != null )
                {
                    response.putHeader( ApplicationHeader.content_type.key(), contentType );
                }

                response.write( content );
            }
        }
        finally
        {
            response.end();
        }
    }

    public Respond status( final ApplicationStatus status )
    {
        this.status = status;
        return this;
    }

    public Respond serverError( final Throwable error, final boolean printStackTrace )
    {
        return serverError( error, null, printStackTrace );
    }

    public Respond serverError( final Throwable error, final String message, final boolean printStackTrace )
    {
        this.status = ApplicationStatus.SERVER_ERROR;
        this.contentType = ContentType.text_plain.value();

        final StringBuilder e = new StringBuilder();
        if ( message != null )
        {
            e.append( message )
             .append( "\n" );
        }

        if ( printStackTrace )
        {
            e.append( error.getMessage() )
             .append( ":\n  " )
             .append( join( error.getStackTrace(), "\n  " ) );
        }
        else
        {
            e.append( "An internal server error has occurred. Please contact this application's administrator" );
        }

        this.entity = e;

        return this;
    }

    public Respond badRequest( final String reason )
    {
        this.status = ApplicationStatus.BAD_REQUEST;
        this.entity = reason;
        this.contentType = ContentType.text_plain.value();

        return this;
    }

}

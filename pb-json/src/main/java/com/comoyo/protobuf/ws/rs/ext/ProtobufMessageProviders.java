/**
 * Copyright (C) 2014 Telenor Digital AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comoyo.protobuf.ws.rs.ext;

import com.comoyo.protobuf.json.PbJsonReader;
import com.comoyo.protobuf.json.PbJsonWriter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * A set of reader and writer providers for protobuf-based value type
 * objects.  Includes support for serializing to and from {@code
 * application/json}, {@code application/*+json}, {@code
 * application/x-protobuf} and {@code application/*+x-protobuf}.
 *
 */
public class ProtobufMessageProviders
{
    private static final MediaType MEDIA_TYPE_APPLICATION_PB_TYPE
        = new MediaType("application", "x-protobuf");

    private static final int MESSAGE_BUILDER_CACHE_SIZE = 100;
    /* Maps from Message types to their static .newBuilder() Method objects. */
    private static final LoadingCache<Class<Message>, Method> builders =
        CacheBuilder.newBuilder()
        .maximumSize(MESSAGE_BUILDER_CACHE_SIZE)
        .build(new CacheLoader<Class<Message>, Method>() {
                public Method load(Class<Message> type) throws NoSuchMethodException {
                    return type.getMethod("newBuilder");
                }
            });

    /**
     * Fetch list of the individual provider classes.  This list is
     * suitable for inserting into a Jersey client configuration
     * object {@link com.sun.jersey.api.client.config.ClientConfig} by
     * way of {@code getClasses().addAll(...);}
     *
     * @return list of provider classes
     */
    public static List<Class<?>> getProviderClasses()
    {
        return Arrays.asList(
            NativeReader.class,
            NativeWriter.class,
            JsonReader.class,
            JsonWriter.class);
    }

    private static boolean matchesTypeOrSuffix(MediaType input, MediaType filter)
    {
        if (input == null) {
            return false;
        }
        if (filter.isCompatible(input)) {
            return true;
        }
        if (filter.getType().equals(input.getType())) {
            return input.getSubtype().endsWith("+" + filter.getSubtype());
        }
        return false;
    }

    private static Message.Builder getBuilderForType(Class<Message> type)
        throws PbInstantiationException
    {
        try {
            return (Message.Builder) builders.get(type).invoke(null);
        }
        catch (ExecutionException e) {
            throw new PbInstantiationException(
                "Unable to instantiate builder for " + type, e.getCause());
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new PbInstantiationException(
                "Unable to instantiate builder for " + type, e);
        }
    }

    @Singleton
    @Provider
    public static class NativeReader
        implements MessageBodyReader<Message>
    {
        @Override
        public boolean isReadable(
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MEDIA_TYPE_APPLICATION_PB_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        @Override
        public Message readFrom(
            final Class<Message> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders,
            final InputStream entityStream)
            throws IOException
        {
            try {
                final Message.Builder builder = getBuilderForType(type);
                return builder.mergeFrom(entityStream).build();
            }
            catch (PbInstantiationException e) {
                throw new RuntimeException(e);
            }
            catch (UninitializedMessageException e) {
                Response failure =
                    Response.status(Status.BAD_REQUEST)
                    .entity("Invalid protocol buffer received: " +
                            e.getMessage())
                    .build();
                throw new WebApplicationException(e, failure);
            }
        }
    }

    @Singleton
    @Provider
    public static class NativeWriter
        implements MessageBodyWriter<Message>
    {
        @Override
        public boolean isWriteable(
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MEDIA_TYPE_APPLICATION_PB_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        @Override
        public long getSize(
            final Message m,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType)
        {
            return m.getSerializedSize();
        }

        @Override
        public void writeTo(
            final Message m,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream)
            throws IOException
        {
            m.writeTo(entityStream);
        }
    }

    @Singleton
    @Provider
    public static class JsonReader
        implements MessageBodyReader<Message>
    {
        private final PbJsonReader jsonReader = new PbJsonReader().withAllowUnknownFields();

        @Override
        public boolean isReadable(
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MediaType.APPLICATION_JSON_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        @Override
        public Message readFrom(
            final Class<Message> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders,
            final InputStream entityStream)
            throws IOException
        {
            try {
                final Message.Builder builder = getBuilderForType(type);
                return jsonReader.parse(builder, entityStream);
            }
            catch (PbInstantiationException e) {
                throw new RuntimeException(e);
            }
            catch (PbJsonReader.ReaderException e) {
                Response failure =
                    Response.status(Status.BAD_REQUEST)
                    .entity("Unable to deserialize JSON to protobuffer: " + e.getMessage())
                    .build();
                throw new WebApplicationException(e, failure);
            }
        }
    }

    @Singleton
    @Provider
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public static class JsonWriter
        implements MessageBodyWriter<Message>
    {
        private final PbJsonWriter jsonWriter = new PbJsonWriter();

        public boolean isWriteable(
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MediaType.APPLICATION_JSON_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        public long getSize(
            final Message m,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType)
        {
            return -1;
        }

        public void writeTo(
            final Message m,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream)
            throws IOException
        {
            try {
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                entityStream,
                                StandardCharsets.UTF_8));
                jsonWriter.generateTo(m, writer);
            } catch (PbJsonWriter.WriterException ex) {
                throw new IOException("Unable to write protocol buffer as JSON to OutputStream", ex);
            }
        }
    }

    public static class PbInstantiationException extends Exception
    {
        public PbInstantiationException(String message) { super(message); }
        public PbInstantiationException(String message, Throwable cause) { super(message, cause); }
        public PbInstantiationException(Throwable cause) { super(cause); }
    }
}

package com.comoyo.protobuf.ws.rs.ext;

import com.google.protobuf.Message;

import com.comoyo.protobuf.json.PbJsonReader;
import com.comoyo.protobuf.json.PbJsonWriter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * A set of reader and writer providers for protobuf-based value
 * type objects.  Includes support for serializing to and from
 * application/json, application/*+json, application/x-protobuf and
 * application/*+x-protobuf.
 *
 * @author argggh
 */

public class ProtobufMessageProviders
{
    private static final MediaType MEDIA_TYPE_APPLICATION_PB_TYPE
        = new MediaType("application", "x-protobuf");

    /**
     * Fetch list of the individual provider classes.  This list is
     * suitable for inserting into a Jersey client configuration
     * object {@link com.sun.jersey.api.client.config.ClientConfig} by
     * way of getClasses().addAll(...);
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

    static boolean matchesTypeOrSuffix(MediaType input, MediaType filter)
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

    @Singleton
    @Provider
    public static class NativeReader
        implements MessageBodyReader<Message>
    {
        private static final Logger logger = Logger.getLogger(NativeReader.class.getName());
        private static final int MESSAGE_TYPE_CACHE_SIZE = 100;
        /** Maps from Message types to their static .parseFrom(InputStream) Method objects. */
        private static final LoadingCache<Class<Message>, Method> parsers =
                CacheBuilder.newBuilder()
                        .maximumSize(MESSAGE_TYPE_CACHE_SIZE)
                        .build(new CacheLoader<Class<Message>, Method>() {
                            public Method load(Class<Message> type) throws NoSuchMethodException {
                                return type.getMethod("parseFrom", InputStream.class);
                            }
                        });

        public boolean isReadable(
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MEDIA_TYPE_APPLICATION_PB_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        public Message readFrom(
            Class<Message> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException
        {
            try {
                Method parseFrom = parsers.get(type);
                // Message.parseFrom is static, so invoking on it a null object is (technically) OK.
                Message parsed = (Message) parseFrom.invoke(null, entityStream);
                return parsed;
            } catch (IllegalAccessException | IllegalArgumentException | SecurityException | ExecutionException e) {
                logger.log(Level.SEVERE, "Broken code, reflection-related exception!", e);
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InvalidProtocolBufferException) {
                    Response failure =
                            Response.status(Status.BAD_REQUEST)
                                    .entity("Invalid protocol buffer received: " +
                                                cause.getMessage())
                                    .build();
                    throw new WebApplicationException(failure);
                }
                logger.log(Level.WARNING,
                        "Problem invoking Message.parseFrom other than " +
                        "InvalidProtocolBufferException.", cause);
                throw new WebApplicationException(cause);
            }
        }
    }

    @Singleton
    @Provider
    public static class NativeWriter
        implements MessageBodyWriter<Message>
    {
        public boolean isWriteable(
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MEDIA_TYPE_APPLICATION_PB_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        public long getSize(
            Message m, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return m.getSerializedSize();
        }

        public void writeTo(
            Message m, Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException
        {
            m.writeTo(entityStream);
        }
    }

    @Singleton
    @Provider
    public static class JsonReader
        implements MessageBodyReader<GeneratedMessage>
    {
        private static final Logger logger = Logger.getLogger(NativeReader.class.getName());
        private final PbJsonReader jsonReader = new PbJsonReader().withAllowUnknownFields();

        private static final int MESSAGE_TYPE_CACHE_SIZE = 100;
        // Maps from protocol buffer message types to their default instances.
        private static final LoadingCache<Class<GeneratedMessage>, GeneratedMessage> typeInstances =
                CacheBuilder.newBuilder()
                        .maximumSize(MESSAGE_TYPE_CACHE_SIZE)
                        .build(new CacheLoader<Class<GeneratedMessage>, GeneratedMessage>() {
                            @Override
                            public GeneratedMessage load(Class<GeneratedMessage> type) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
                                // static getDefaultInstance() is not actually a method defined on
                                // the GeneratedMessage class, but protoc is documented as
                                // generating this method on all generated Java classes, so we
                                // take a chance and bet on its existence.
                                // (This is also why we're referring to the GeneratedMessage class
                                // here in the first place,  as it's normally considered an
                                // implementation detail of Protobufs-on-Java.)
                                return (GeneratedMessage) type.getMethod("getDefaultInstance").invoke(null);
                            }
                        });
        @Override
        public boolean isReadable(
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MediaType.APPLICATION_JSON_TYPE)
                && GeneratedMessage.class.isAssignableFrom(type);
        }

        @Override
        public GeneratedMessage readFrom(
            Class<GeneratedMessage> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException
        {
            try {
                GeneratedMessage defaultMessageInstance = typeInstances.get(type);
                return jsonReader.parse(defaultMessageInstance, entityStream);
            } catch (PbJsonReader.ReaderException ex) {
                logger.log(Level.INFO, "Unable to deserialize JSON to protobuffer", ex);
                throw new IOException("Unable to deserialize JSON to protobuffer: " + ex.getMessage(), ex);
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, "Unable to instantiate target protobuffer class", ex);
                throw new IOException("Unable to instantiate target protobuffer class", ex);
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
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return matchesTypeOrSuffix(mediaType, MediaType.APPLICATION_JSON_TYPE)
                && Message.class.isAssignableFrom(type);
        }

        public long getSize(
            Message m, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return -1;
        }

        public void writeTo(
            Message m, Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException
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
}

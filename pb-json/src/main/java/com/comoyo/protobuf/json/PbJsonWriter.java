package com.comoyo.protobuf.json;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Serialize Protobuf messages to JSON using {@link JsonFactory
 * Jackson}.
 *
 */
public class PbJsonWriter
{
    private final JsonFactory factory = new JsonFactory();
    private boolean suppressEmptyRepeated = false;
    private boolean usePrettyPrinter = false;

    /**
     * Construct new writer instance.  Writers can be reused and
     * shared.  Setting feature flags between invocations is
     * supported, but discouraged if the instance is shared.
     */
    public PbJsonWriter()
    {
    }

    /**
     * Toggle behavior when serializing repeated fields with no
     * populated values.
     *
     * @param value when {@code true}, suppress empty repeated fields
     * (in the same as unset regular fields) when serializing, rather
     * than output empty array values.  Defaults to {@code false}.
     */
    public void setSuppressEmptyRepeatedFields(boolean value)
    {
        suppressEmptyRepeated = value;
    }

    /**
     * Fluent interface method that corresponds to calling {@link
     * #setSuppressEmptyRepeatedFields} with an argument of {@code
     * true}.
     * @return writer instance
     */
    public PbJsonWriter withSuppressEmptyRepeatedFields()
    {
        setSuppressEmptyRepeatedFields(true);
        return this;
    }

    /**
     * Toggle formatting of produced JSON document.
     *
     * @param value when {@code true}, include whitespace to make
     * resulting JSON document human-readable.  Defaults to {@code
     * false}.
     */
    public void setUsePrettyPrinter(boolean value)
    {
        usePrettyPrinter = value;
    }

    /**
     * Fluent interface method that corresponds to calling {@link
     * #setUsePrettyPrinter} with an argument of {@code true}.
     * @return writer instance
     */
    public PbJsonWriter withUsePrettyPrinter()
    {
        setUsePrettyPrinter(true);
        return this;
    }

    /**
     * Generate JSON document from given {@link Message} object.
     * @param message Protobuf value object to serialize
     * @return JSON document representing given value object
     * @throws WriterException if unable to produce a serialized
     * message.
     */
    public String generate(Message message)
        throws WriterException
    {
        try {
            StringWriter writer = new StringWriter();
            generateTo(message, writer);
            return writer.toString();
        }
        catch (IOException e) {
            throw new WriterException("Unable to write serialized message", e);
        }
    }

    /**
     * Write JSON document from given {@link Message} object to a {@link Writer}.
     * @param message Protobuf value object to serialize
     * @param writer Serialized JSON is written to this.
     * @throws IOException if unable to write the serialized message to the
     *         {@link Writer}
     * @throws WriterException if unable to produce a serialized message.
     */
    public void generateTo(Message message, Writer writer)
            throws IOException, WriterException
    {
        try (JsonGenerator generator = factory.createJsonGenerator(writer)) {
            generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            if (usePrettyPrinter) {
                generator.useDefaultPrettyPrinter();
            }
            generateObject(message, generator);
        }
        finally {
            writer.flush();
        }
    }

    protected void generateObject(Message message, JsonGenerator generator)
        throws WriterException, IOException
    {
        final Descriptors.Descriptor descriptor = message.getDescriptorForType();
        generator.writeStartObject();
        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            if (field.isRepeated()) {
                final int length = message.getRepeatedFieldCount(field);
                if (suppressEmptyRepeated && length == 0) {
                    continue;
                }
                generator.writeFieldName(field.getName());
                generator.writeStartArray();
                for (int i = 0; i < length; i++) {
                    generateValue(field, message.getRepeatedField(field, i), generator);
                }
                generator.writeEndArray();
            }
            else {
                if (!message.hasField(field)) {
                    continue;
                }
                generator.writeFieldName(field.getName());
                generateValue(field, message.getField(field), generator);
            }
        }
        generator.writeEndObject();
    }

    private void generateValue(
        final Descriptors.FieldDescriptor field,
        final Object value,
        final JsonGenerator generator)
        throws WriterException, IOException
    {
        switch (field.getType()) {
        case MESSAGE:
            generateObject((Message) value, generator);
            break;
        case BOOL:
            generator.writeBoolean((Boolean) value);
            break;
        case DOUBLE:
            generator.writeNumber((Double) value);
            break;
        case FLOAT:
            generator.writeNumber((Float) value);
            break;
        case INT32:
        case SINT32:
        case UINT32:
        case FIXED32:
        case SFIXED32:
        case INT64:
        case SINT64:
        case UINT64:
        case FIXED64:
        case SFIXED64:
            if (value instanceof Long) {
                generator.writeNumber((Long) value);
            }
            else if (value instanceof Integer) {
                generator.writeNumber((Integer) value);
            }
            else {
                throw new WriterException(
                    "Unable to serialize " + value.getClass()
                        + " to " + field.getType().name()
                        + " for field " + field.getName());
            }
            break;
        case STRING:
            generator.writeString((String) value);
            break;
        case BYTES:
            generator.writeBinary(((String) value).getBytes(StandardCharsets.UTF_8));
            break;
        case ENUM:
            generator.writeString(((Descriptors.EnumValueDescriptor) value).getName());
            break;
        default:
            throw new WriterException("Unable to serialize value of type "
                                      + field.getType().name());
        }
    }

    public static class WriterException extends Exception
    {
        public WriterException(String message) { super(message); }
        public WriterException(String message, Throwable cause) { super(message, cause); }
        public WriterException(Throwable cause) { super(cause); }
    }
}

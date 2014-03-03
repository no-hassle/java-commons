package com.comoyo.protobuf.json;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Deserialize JSON documents to Protobuf messages using {@link
 * JsonFactory Jackson}.
 *
 */
public class PbJsonReader
{
    private final JsonFactory factory = new JsonFactory();
    private boolean allowUnknownFields = false;
    private boolean allowComments = false;

    /**
     * Construct new reader instance.  Readers can be reused and
     * shared.  Setting feature flags between invocations is
     * supported, but discouraged if the instance is shared.
     */
    public PbJsonReader()
    {
    }

    /**
     * Toggle behavior when encountering fields in the JSON documents
     * that are not described in the schema for the protobuf message
     * type being marshalled.
     *
     * @param value when {@code true}, allow (and ignore) unknown
     * fields instead of throwing parse error exceptions.  Defaults to
     * {@code false}.
     */
    public void setAllowUnknownFields(boolean value)
    {
        allowUnknownFields = value;
    }

    /**
     * Fluent interface method that corresponds to calling {@link
     * #setAllowUnknownFields} with an argument of {@code true}.
     * @return reader instance
     */
    public PbJsonReader withAllowUnknownFields()
    {
        setAllowUnknownFields(true);
        return this;
    }

    /**
     * Toggle behavior when encountering JavaScript-type comments in
     * JSON documents being parsed.
     *
     * @param value when {@code true}, allow (and ignore) commented
     * regions, instead of throwing parse error exceptions.  Defaults
     * to {@code false}.
     */
    public void setAllowComments(boolean value)
    {
        allowComments = value;
    }

    /**
     * Fluent interface method that corresponds to calling {@link
     * #setAllowComments} with an argument of {@code true}.
     * @return reader instance
     */
    public PbJsonReader withAllowComments()
    {
        setAllowComments(true);
        return this;
    }

    /**
     * Parse a JSON document given as a {@link String}.
     *
     * @param typeInstance {@link Message} instance describing object
     * type to marshall to.  Typically an incomplete message obtained
     * using e.g getDefaultInstance().  The instance is used only for
     * schema reference, existing values are not merged to the
     * resulting message object.
     * @param json JSON document.
     */
    public <T extends Message> T parse(T typeInstance, String json)
        throws ReaderException
    {
        try {
            JsonParser parser = factory.createJsonParser(json);
            return parse(typeInstance, parser);
        }
        catch (IOException e) {
            throw new InputException(e);
        }
    }

    /**
     * Parse a JSON document given as an {@link InputStream}.  See
     * {@link #parse parse(T, String)}.
     *
     * @param typeInstance {@link Message} instance describing object
     * type to marshall to.
     * @param stream JSON document.
     */
    public <T extends Message> T parse(T typeInstance, InputStream stream)
        throws ReaderException
    {
        try {
            JsonParser parser = factory.createJsonParser(stream);
            return parse(typeInstance, parser);
        }
        catch (IOException e) {
            throw new InputException(e);
        }
    }

    /**
     * Parse a JSON document given as a {@link File}.  See {@link
     * #parse parse(T, String)}.
     *
     * @param typeInstance {@link Message} instance describing object
     * type to marshall to.
     * @param file JSON document.
     */
    public <T extends Message> T parse(T typeInstance, File file)
        throws ReaderException
    {
        try {
            JsonParser parser = factory.createJsonParser(file);
            return parse(typeInstance, parser);
        }
        catch (IOException e) {
            throw new InputException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Message> T parse(T typeInstance, JsonParser parser)
        throws ReaderException
    {
        parser.configure(JsonParser.Feature.ALLOW_COMMENTS, allowComments);
        try {
            JsonToken token = parser.nextToken();
            if (token == null) {
                return null;
            }
            if (!token.equals(JsonToken.START_OBJECT)) {
                throw new ReaderException(
                    "JSON document did not start with opening brace");
            }

            Message.Builder builder
                = parseObject(typeInstance.newBuilderForType(), parser);
            Message result = builder.build();
            if (!result.getClass().isAssignableFrom(typeInstance.getClass())) {
                throw new ReaderException(
                    "Unexpected class mismatch: builder constructed for "
                        + typeInstance.getClass() + " built object of class "
                        + result.getClass());
            }
            return (T) result;
        }
        catch (IOException e) {
            throw new InputException(e);
        }
    }

    private <T extends Message.Builder> T parseObject(T builder, JsonParser parser)
        throws ReaderException, IOException
    {
        final Descriptors.Descriptor descriptor = builder.getDescriptorForType();

        while (true) {
            final JsonToken token = parser.nextToken();
            if (token == null) {
                throw new InputException("EOF during parsing");
            }
            switch (token) {
            case END_OBJECT:
                return builder;
            case FIELD_NAME:
                final String name = parser.getText();
                final JsonToken valueToken = parser.nextToken();
                if (valueToken == null) {
                    throw new InputException("EOF during parsing");
                }
                final Descriptors.FieldDescriptor field = descriptor.findFieldByName(name);
                if (field == null) {
                    if (!allowUnknownFields) {
                        throw new TypeMismatchException("Unknown field " + name + " encountered in input");
                    }
                    parser.skipChildren();
                    continue;
                }
                if (valueToken.equals(JsonToken.VALUE_NULL)) {
                    continue;
                }
                if (valueToken.equals(JsonToken.START_ARRAY) != field.isRepeated()) {
                    throw new TypeMismatchException(
                        name,
                        valueToken.equals(JsonToken.START_ARRAY) ? "ARRAY" : "SINGULAR",
                        field.isRepeated() ? "REPEATED" : "SINGULAR");
                }
                if (valueToken.equals(JsonToken.START_ARRAY)) {
                    parseRepeatedField(builder, field, parser);
                }
                else {
                    Object value = parseValue(builder, field, parser);
                    if (value != null) {
                        // Null values in the JSON document must be
                        // represented as absent values in the
                        // protobuf representation.
                        builder.setField(field, value);
                    }
                }
                break;
            default:
                throw new InputException(
                    "Saw token " + token.name()
                        + " when expecting either END_OBJECT or FIELD_NAME");
            }
        }
    }

    private Object parseValue(
        final Message.Builder builder,
        final Descriptors.FieldDescriptor field,
        final JsonParser parser)
        throws ReaderException, IOException
    {
        final JsonToken token = parser.getCurrentToken();
        final Type type = field.getType();
        switch (token) {
        case START_OBJECT:
            if (!type.equals(Type.MESSAGE)) {
                throw new TypeMismatchException(field.getName(), "OBJECT", type.name());
            }
            return parseObject(builder.newBuilderForField(field), parser).build();
        case VALUE_FALSE:
            if (!type.equals(Type.BOOL)) {
                throw new TypeMismatchException(field.getName(), "BOOLEAN", type.name());
            }
            return Boolean.FALSE;
        case VALUE_TRUE:
            if (!type.equals(Type.BOOL)) {
                throw new TypeMismatchException(field.getName(), "BOOLEAN", type.name());
            }
            return Boolean.TRUE;
        case VALUE_NUMBER_FLOAT:
            switch (type) {
            case DOUBLE:
                return Double.valueOf(parser.getDoubleValue());
            case FLOAT:
                return Float.valueOf(parser.getFloatValue());
            default:
                throw new TypeMismatchException(field.getName(), "FRACTIONAL", type.name());
            }
        case VALUE_NUMBER_INT:
            switch (type) {
            case INT32:
            case SINT32:
            case UINT32:
            case FIXED32:
            case SFIXED32:
                return Integer.valueOf(parser.getIntValue());
            case INT64:
            case SINT64:
            case UINT64:
            case FIXED64:
            case SFIXED64:
                return Long.valueOf(parser.getLongValue());
            case DOUBLE:
                return Double.valueOf(parser.getLongValue());
            case FLOAT:
                return Float.valueOf(parser.getLongValue());
            case ENUM:
                Descriptors.EnumDescriptor desc = field.getEnumType();
                Descriptors.EnumValueDescriptor val
                    = desc.findValueByNumber(parser.getIntValue());
                if (val == null) {
                    throw new TypeMismatchException(
                        "Schema/input mismatch, enum " + field.getName()
                            + " has no member numbered " + parser.getIntValue());
                }
                return val;
            default:
                throw new TypeMismatchException(field.getName(), "NUMBER", type.name());
            }
        case VALUE_STRING:
            switch (type) {
            case STRING:
                return parser.getText();
            case BYTES:
                return parser.getBinaryValue();
            case ENUM:
                Descriptors.EnumDescriptor desc = field.getEnumType();
                Descriptors.EnumValueDescriptor val
                    = desc.findValueByName(parser.getText());
                if (val == null) {
                    throw new TypeMismatchException(
                        "Schema/input mismatch, enum " + field.getName()
                            + " has no member " + parser.getText());
                }
                return val;
            default:
                throw new TypeMismatchException(field.getName(), "STRING", type.name());
            }
        default:
            throw new TypeMismatchException(
                "Schema/input mismatch, don't know how to parse " + token.name());
        }
    }

    private void parseRepeatedField(
        final Message.Builder builder,
        final Descriptors.FieldDescriptor field,
        final JsonParser parser)
        throws ReaderException, IOException
    {
        while (true) {
            final JsonToken elem = parser.nextToken();
            if (elem == null) {
                throw new InputException("EOF during parsing");
            }
            if (elem.equals(JsonToken.END_ARRAY)) {
                break;
            }
            Object value = parseValue(builder, field, parser);
            if (value != null) {
                // This skips null-value array elements when
                // representing as protobuf.  Arguably we might want
                // to throw a parse error here.
                builder.addRepeatedField(field, value);
            }
        }
    }

    public static class ReaderException extends Exception
    {
        public ReaderException(String message) { super(message); }
        public ReaderException(String message, Throwable cause) { super(message, cause); }
        public ReaderException(Throwable cause) { super(cause); }
    }

    public static class TypeMismatchException extends ReaderException
    {
        public TypeMismatchException(String message) { super(message); }
        public TypeMismatchException(String message, Throwable cause) { super(message, cause); }
        public TypeMismatchException(Throwable cause) { super(cause); }
        public TypeMismatchException(String field, String input, String schema)
        {
            super("Schema/input mismatch for field " + field
                  + ", can't coerce " + input + " to " + schema);
        }
    }

    public static class InputException extends ReaderException
    {
        public InputException(String message) { super(message); }
        public InputException(String message, Throwable cause) { super(message, cause); }
        public InputException(Throwable cause) { super(cause); }
    }
}

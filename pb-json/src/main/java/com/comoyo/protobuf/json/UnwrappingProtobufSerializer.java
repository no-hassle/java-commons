package com.comoyo.protobuf.json;

import com.google.protobuf.Message;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;

/**
 * Json serializer for {@link Message} derived protobuf value objects
 * to support {@link org.codehaus.jackson.annotate.JsonUnwrapped}
 * annotation.
 *
 */
public class UnwrappingProtobufSerializer
    extends SerializerBase<Message>
{
    private final PbJsonWriter writer;

    public UnwrappingProtobufSerializer(PbJsonWriter writer)
    {
        super(Message.class);
        this.writer = writer;
    }

    @Override
    public void serialize(
        final Message value,
        final JsonGenerator jgen,
        final SerializerProvider provider)
            throws IOException
    {
        try {
            writer.generateUnwrappedObject(value, jgen);
        }
        catch (PbJsonWriter.WriterException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isUnwrappingSerializer()
    {
        return true;
    }

    @Override
    public JsonSerializer<Message> unwrappingSerializer()
    {
        return this;
    }
}

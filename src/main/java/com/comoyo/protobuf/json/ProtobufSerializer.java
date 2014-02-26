package com.comoyo.protobuf.json;

import com.google.protobuf.Message;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;

/**
 * Wire up Jackson to do json serialization of {@link Message} derived
 * protobuf value objects.
 *
 */
public class ProtobufSerializer
    extends SerializerBase<Message>
{
    private final PbJsonWriter writer;

    public ProtobufSerializer(PbJsonWriter writer)
    {
        super(Message.class);
        this.writer = writer;
    }

    public ProtobufSerializer()
    {
        this(new PbJsonWriter());
    }

    @Override
    public void serialize(
        final Message value,
        final JsonGenerator jgen,
        final SerializerProvider provider)
            throws IOException
    {
        try {
            writer.generateObject(value, jgen);
        }
        catch (PbJsonWriter.WriterException e) {
            throw new IOException(e);
        }
    }
}

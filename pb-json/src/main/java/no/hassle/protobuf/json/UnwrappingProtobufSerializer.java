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

package no.hassle.protobuf.json;

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

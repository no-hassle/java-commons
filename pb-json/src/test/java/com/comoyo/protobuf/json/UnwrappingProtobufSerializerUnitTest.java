package com.comoyo.protobuf.json;

import com.comoyo.protobuf.json.TestObjects;
import com.google.protobuf.Message;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.module.SimpleModule;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class UnwrappingProtobufSerializerUnitTest
{
    public static class Inner
    {
        public final String inner;
        public Inner(String inner)
        {
            this.inner = inner;
        }
    }

    public static class Encapsulate
    {
        public final String toplevel;
        @JsonUnwrapped
        public final Inner inner;
        @JsonUnwrapped
        public final Message entity;

        public Encapsulate(String toplevel, Inner inner, Message entity)
        {
            this.toplevel = toplevel;
            this.inner = inner;
            this.entity = entity;
        }
    }

    @Test
    public void test()
        throws Exception
    {
        final String EXPECTED_JSON
            = "{\"toplevel\":\"foo\",\"inner\":\"bar\",\"child\":{\"grand_child\":[{\"string_1\":\"quux\"}]}}";

        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule("Unwr", new Version(1, 0, 0, null));
        module.addSerializer(new ProtobufSerializer());
        mapper.registerModule(module);
        final ObjectWriter writer = mapper.writer();

        final Inner inner = new Inner("bar");
        final TestObjects.Nested.Builder builder = TestObjects.Nested.newBuilder();
        builder
            .getChildBuilder()
            .addGrandChildBuilder()
            .setString1("quux");
        final Encapsulate enc = new Encapsulate("foo", inner, builder.build());

        final String serialized = writer.writeValueAsString(enc);
        assertEquals("Serialized string did not match expected JSON",
                     EXPECTED_JSON, serialized);
    }
}

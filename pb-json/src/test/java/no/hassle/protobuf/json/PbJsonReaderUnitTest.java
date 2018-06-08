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

import no.hassle.protobuf.json.TestObjects;

import org.apache.commons.io.IOUtils;
import com.google.protobuf.Message;
import java.io.InputStream;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class PbJsonReaderUnitTest
{
    @Test
    public void testAllFields()
        throws Exception
    {
        test("all-fields.json", TestObjects.AllFields.getDefaultInstance(), false);
    }

    @Test
    public void testAllFieldsStrict()
        throws Exception
    {
        test("all-fields-strict.json", TestObjects.AllFields.getDefaultInstance(), true);
    }

    @Test
    public void testStrictVsLax()
        throws Exception
    {
        TestObjects.AllFields strict
            = test("all-fields-strict.json", TestObjects.AllFields.getDefaultInstance(), false);
        TestObjects.AllFields lax
            = test("all-fields.json", TestObjects.AllFields.getDefaultInstance(), false);
        assertEquals("Deserialized lax and strict json documents did not result in same pb representation",
                     strict, lax);
    }

    @Test
    public void testDefaults()
        throws Exception
    {
        PbJsonReader reader = new PbJsonReader();
        TestObjects.Defaults defaults = reader.parse(TestObjects.Defaults.getDefaultInstance(), "{}");
        assertEquals("string_1 should be empty", "", defaults.getString1());
        assertEquals("string_2 should not be empty", "default_2", defaults.getString2());
        assertEquals("int32_1 should be zero", 0, defaults.getInt321());
        assertEquals("int32_2 should not be zero", 42, defaults.getInt322());
        assertEquals("bool_1 should be false", false, defaults.getBool1());
        assertEquals("bool_2 should not be false", true, defaults.getBool2());
    }

    @Test
    public void testNestedFields()
        throws Exception
    {
        test("nested.json", TestObjects.Nested.getDefaultInstance(), true);
    }

    private <T extends Message> T test(String resource, T prototype, boolean strict)
        throws Exception
    {
        PbJsonReader reader = new PbJsonReader();
        PbJsonWriter writer = new PbJsonWriter()
            .withSuppressEmptyRepeatedFields();

        InputStream jsonStream =
            this.getClass().getResourceAsStream(resource);
        String json = IOUtils.toString(jsonStream, "UTF-8");
        T message = reader.parse(prototype, json);
        if (strict) {
            String result = writer.generate(message);
            String in = json.replaceAll("[ \n\t]+", "");
            String out = result.replaceAll("[ \n\t]+", "");
            assertEquals("Produced json did not match input", in, out);
        }
        return message;
    }
}


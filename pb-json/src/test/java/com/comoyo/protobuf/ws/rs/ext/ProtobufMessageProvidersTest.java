package com.comoyo.protobuf.ws.rs.ext;

import com.comoyo.protobuf.json.PbJsonReader;
import com.comoyo.protobuf.json.PbJsonReaderUnitTest;
import com.comoyo.protobuf.json.TestObjects;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.WebAppDescriptor.Builder;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.inmemory.InMemoryTestContainerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProtobufMessageProvidersTest
    extends JerseyTest
{
    private static final String MEDIA_TYPE_APPLICATION_PB = "application/x-protobuf";
    private static final MediaType MEDIA_TYPE_APPLICATION_PB_TYPE
        = MediaType.valueOf(MEDIA_TYPE_APPLICATION_PB);

    private static final PbJsonReader reader = new PbJsonReader();
    private static TestObjects.AllFields testData;
    private static String testDataJson;

    @Path("/")
    public static class RootResource
    {
        @POST
        @Path("post")
        @Consumes({MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_PB})
        public Response postResource(TestObjects.AllFields input)
        {
            return testData.equals(input)
                ? Response.ok("TEST OK").build()
                : Response.serverError().build();
        }

        @GET
        @Path("get")
        @Produces({MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_PB})
        public TestObjects.AllFields getResource()
        {
            return testData;
        }
    }

    public ProtobufMessageProvidersTest()
        throws Exception
    {
        super();
        InputStream jsonStream =
            PbJsonReaderUnitTest.class.getResourceAsStream("all-fields.json");
        testDataJson = IOUtils.toString(jsonStream, "UTF-8");
        testData = reader.parse(TestObjects.AllFields.getDefaultInstance(), testDataJson);
    }

    @Override
    public WebAppDescriptor configure()
    {
        final ClientConfig clientConfig
            = new DefaultClientConfig(
                new HashSet<>(ProtobufMessageProviders.getProviderClasses()));
        return new WebAppDescriptor.Builder(
            ProtobufMessageProvidersTest.class.getPackage().getName())
            .clientConfig(clientConfig)
            .build();
    }

    @Override
    public TestContainerFactory getTestContainerFactory() {
        return new InMemoryTestContainerFactory();
    }

    @Test
    public void testPostJson()
    {
        WebResource webResource = resource();
        String status = webResource
            .path("post")
            .type(MediaType.APPLICATION_JSON)
            .post(String.class, testDataJson);
        assertEquals(
            "JSON passed to POST resource was not accepted",
            "TEST OK", status);
    }

    @Test
    public void testPostPb()
    {
        WebResource webResource = resource();
        byte[] pbData = testData.toByteArray();
        String status = webResource
            .path("post")
            .type(MEDIA_TYPE_APPLICATION_PB_TYPE)
            .post(String.class, pbData);
        assertEquals(
            "Pb data passed to POST resource was not accepted",
            "TEST OK", status);
    }

    @Test
    public void testGetJson()
        throws Exception
    {
        final WebResource webResource = resource();
        final InputStream stream = webResource
            .path("get")
            .accept(MediaType.APPLICATION_JSON)
            .get(InputStream.class);
        final TestObjects.AllFields response
            = reader.parse(TestObjects.AllFields.getDefaultInstance(), stream);
        assertEquals(
            "JSON returned from GET resource did not match expected contents",
            testData, response);
    }

    @Test
    public void testGetPb()
        throws IOException
    {
        final WebResource webResource = resource();
        final InputStream stream = webResource
            .path("get")
            .accept(MEDIA_TYPE_APPLICATION_PB_TYPE)
            .get(InputStream.class);
        final TestObjects.AllFields response
            = TestObjects.AllFields.parseFrom(stream);
        assertEquals(
            "Pb data returned from GET resource did not match expected contents",
            testData, response);
    }
}

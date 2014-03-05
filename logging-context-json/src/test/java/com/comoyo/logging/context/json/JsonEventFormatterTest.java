package com.comoyo.logging.context.json;

import com.comoyo.commons.logging.context.LoggingContext;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import junit.framework.TestCase;

/**
 *
 * Test that JsonEventFormatter is well-behaved both as a unit and with java.util.logging.
 *
 * @author anders
 */
public class JsonEventFormatterTest extends TestCase {
    static final String TEST_SOURCE = "testSource";
    static final String TEST_HOSTNAME = "testhostname";
    static final String TEST_LOG_MESSAGE = "This is a log message";
    static final List<String> TEST_TAGS = Arrays.asList(new String[] {"tag1", "tag2"});

    JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);
    JsonGeneratorFactory prettyJsonFactory = Json.createGeneratorFactory(
            ImmutableMap.of(JsonGenerator.PRETTY_PRINTING, "true"));

    /**
     * Base case test - send in a simple log message, see if JSON with required fields comes out.
     */
    public void testSimpleFormat() {
        final LogRecord record = new LogRecord(Level.INFO, TEST_LOG_MESSAGE);
        final JsonObject resultObject = logToJson(record);
        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_TIMESTAMP));
        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_VERSION));
        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_LEVEL));
        assertEquals(Level.INFO.getName(), resultObject.getString(JsonEventFormatter.JSON_KEY_LEVEL));
    }

    /**
     * Test that logging contexts are propagated to log entries.
     */
    public void testLogWithContext() {
        final LogRecord record = new LogRecord(Level.INFO, TEST_LOG_MESSAGE);
        final JsonObject resultObject;
        try (LoggingContext.Scope context = LoggingContext.openContext()) {
            context.addField("magic", "dragon");
            resultObject = logToJson(record);
        }
        JsonObject contextObject = resultObject.getJsonObject(JsonEventFormatter.JSON_KEY_CONTEXT);
        assertEquals("dragon", contextObject.getString("magic"));
    }

    /**
     * Test formatting of log messages with embedded exceptions.
     */
    public void testFormatException() {
        final LogRecord record = new LogRecord(Level.WARNING, TEST_LOG_MESSAGE);
        try {
            int[] fooArray = {0};
            System.out.println(fooArray[1]);  // throws a genuine exception!
        } catch (ArrayIndexOutOfBoundsException ex) {
            record.setThrown(ex);
        }
        final JsonObject resultObject = logToJson(record);
        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_EXCEPTION));
        final JsonObject exceptionObject = resultObject.getJsonObject(JsonEventFormatter.JSON_KEY_EXCEPTION);
        assertTrue(exceptionObject.containsKey(JsonEventFormatter.JSON_KEY_EXCEPTION_CLASS));
        assertEquals(
                ArrayIndexOutOfBoundsException.class.getName(),
                exceptionObject.getString(JsonEventFormatter.JSON_KEY_EXCEPTION_CLASS));
    }

    /**
     * Tests a more elaborate exception structure with a cause as well as a suppressed exception.
     * Not a very common case, but it would be bad to have a logger that chokes on the uncommon
     * errors.
     *
     * @throws Exception
     */
    public void testFormatElaborateException() throws Exception {
        final LogRecord record = new LogRecord(Level.WARNING, TEST_LOG_MESSAGE);
        AutoCloseable failsOnClose = new AutoCloseable() {
                @Override
                public void close() throws Exception {
                    throw new UnsupportedOperationException("I'm afraid I can't do that, Dave.");
                }
            };
        // Provoke a suppressed exception:
        try (AutoCloseable failureWaitingToHappen = failsOnClose)
        {
            int[] fooArray = {0};
            System.out.println(fooArray[1]);  // throws a genuine exception!
        } catch (ArrayIndexOutOfBoundsException ex) {
            // ..and wrap the AIOOBE and its suppressed exception in a new Exception for fun.
            record.setThrown(new RuntimeException(ex));
        }

        final JsonObject resultObject = logToJson(record);

        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_EXCEPTION));

        final JsonObject exceptionObject = resultObject.getJsonObject(JsonEventFormatter.JSON_KEY_EXCEPTION);
        assertTrue(exceptionObject.containsKey(JsonEventFormatter.JSON_KEY_EXCEPTION_CLASS));

        final JsonObject nestedExceptionObject = exceptionObject.getJsonObject(JsonEventFormatter.JSON_KEY_CAUSE);
        assertTrue(nestedExceptionObject.containsKey(JsonEventFormatter.JSON_KEY_EXCEPTION_CLASS));
        assertTrue(nestedExceptionObject.containsKey(JsonEventFormatter.JSON_KEY_SUPPRESSED));

        final JsonArray suppressedExceptionArray = nestedExceptionObject.getJsonArray(JsonEventFormatter.JSON_KEY_SUPPRESSED);
        final JsonObject suppressedExceptionObject = suppressedExceptionArray.getJsonObject(0);
        assertTrue(suppressedExceptionObject.containsKey(JsonEventFormatter.JSON_KEY_EXCEPTION_CLASS));
    }

    /**
     * Verifies that JsonEventFormatter behaves itself well when acting in concert
     * with the rest of java.util.logging. Not really a unit test, but as far as integration
     * tests go, this one is lightweight enough to hide among the unit tests.
     *
     * @throws Exception if anders made a boo-boo.
     */
    public void testJulIntegration() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final JsonEventFormatter instance = new JsonEventFormatter(TEST_HOSTNAME, TEST_SOURCE, TEST_TAGS, prettyJsonFactory);
        StreamHandler logHandler = new StreamHandler(outputStream, instance);
        final LogManager logManager = LogManager.getLogManager();
        final Logger rootLogger = logManager.getLogger("");

        rootLogger.addHandler(logHandler);
        rootLogger.severe(TEST_LOG_MESSAGE);
        rootLogger.removeHandler(logHandler);
        logHandler.flush();

        final String logged = outputStream.toString("UTF-8");
        final JsonObject resultObject = jsonReaderFactory.createReader(
                new StringReader(logged)).readObject();

        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_TIMESTAMP));
        assertTrue(resultObject.containsKey(JsonEventFormatter.JSON_KEY_VERSION));
    }

    public void testJulInitialization() throws Exception {
        final String formatterName = JsonEventFormatter.class.getName();
        final String propPrefix = formatterName + ".";
        String loggingProperties =
                "handlers=java.util.logging.ConsoleHandler\n" +
                "java.util.logging.ConsoleHandler.formatter=" + formatterName + "\n" +
                propPrefix + JsonEventFormatter.PRETTY_PROPERTY + "=true\n" +
                propPrefix + JsonEventFormatter.SOURCE_PROPERTY + "=lol\n" +
                propPrefix + JsonEventFormatter.SOURCEHOST_PROPERTY + "=lolhost\n" +
                propPrefix + JsonEventFormatter.TAG_PROPERTY + "=omg,wtf,bbq";
        ByteArrayInputStream propsInputStream = new ByteArrayInputStream(loggingProperties.getBytes());

        LogManager logManager = LogManager.getLogManager();
        logManager.reset();
        logManager.readConfiguration(propsInputStream);

        final Logger rootLogger = logManager.getLogger("");
        rootLogger.severe(TEST_LOG_MESSAGE);
        Handler logHandler = rootLogger.getHandlers()[0];
        Formatter formatter = logHandler.getFormatter();
        assertEquals(JsonEventFormatter.class, formatter.getClass());
        JsonEventFormatter ourFormatter = (JsonEventFormatter) formatter;
        assertEquals("lolhost", ourFormatter.getHostName());
        assertEquals("lol", ourFormatter.getSourceName());
        assertTrue(ourFormatter.getTags().containsAll(
                Arrays.asList(new String[] {"omg", "wtf", "bbq"})));

        // If we get this far, we're probably out of the woods, reset the logging subsystem.
        logManager.reset();
        logManager.readConfiguration();
    }

    // Laziness methods.
    private JsonObject logToJson(LogRecord record, JsonEventFormatter instance) {
        final String result = instance.format(record);
        System.out.println(getName() + " test log result:\n" + result);
        final JsonObject resultObject = jsonReaderFactory.createReader(new StringReader(result)).readObject();
        return resultObject;
    }

    private JsonObject logToJson(LogRecord record) {
        final JsonEventFormatter instance = new JsonEventFormatter(TEST_HOSTNAME, TEST_SOURCE, TEST_TAGS, prettyJsonFactory);
        return logToJson(record, instance);

    }

}

package com.comoyo.logging.context.json;

import com.comoyo.commons.logging.context.LoggingContext;
import com.google.common.base.Optional;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

/**
 * LoggingContext-aware log formatter for producing logstash-ready JSON.
 * 
 * This is just a formatter, shipping the product off to logstash is left as an
 * exercise for the reader. One recommended option is log with a regular FileHandler
 * and asynchronously transport those to logstash, an alternative is to send the events
 * directly into a Redis list with a custom LogHandler.
 *
 * JSON structure and names based on JSONEventLayoutV1.java from log4j-jsonevent-layout.
 *
 * @author anders
 */
public class JsonEventFormatter extends Formatter {
    public static final String JSON_KEY_VERSION = "@version";
    public static final String JSON_KEY_TIMESTAMP = "@timestamp";
    public static final String JSON_KEY_LEVEL = "level";
    public static final String JSON_KEY_MESSAGE = "message";
    public static final String JSON_KEY_SOURCE = "source";
    public static final String JSON_KEY_SOURCE_HOST = "source_host";
    public static final String JSON_KEY_LOGGER_NAME = "logger_name";
    public static final String JSON_KEY_SOURCE_METHOD = "source_method";
    public static final String JSON_KEY_SOURCE_CLASS = "source_class";
    public static final String JSON_KEY_CONTEXT = "context";
    public static final String JSON_KEY_EXCEPTION = "exception";
    public static final String JSON_KEY_CAUSE = "cause";
    public static final String JSON_KEY_SUPPRESSED = "suppressed";
    public static final String JSON_KEY_EXCEPTION_CLASS = "exception_class";
    public static final String JSON_KEY_EXCEPTION_MESSAGE = "exception_message";
    public static final String JSON_KEY_EXCEPTION_STACKTRACE = "stacktrace";

    static final int LOGSTASH_JSON_VERSION = 1;
    static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
    static final String PROPERTY_PREFIX = JsonEventFormatter.class.getName() + ".";
    static final String TAG_PROPERTY = "tags";
    static final String PRETTY_PROPERTY = "pretty";
    static final String SOURCE_PROPERTY = "source";
    static final String SOURCEHOST_PROPERTY = "source_host";
    static final String TAG_SEPARATOR_REGEX = "[, ]+";

    // MUST NOT be modified after initialization - clone()d without synchronization on all format/1 calls.
    private static final SimpleDateFormat dateFormatterMaster = new SimpleDateFormat(ISO8601_DATE_FORMAT);
    static {
        dateFormatterMaster.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final JsonGeneratorFactory jsonFactory;
    private final String hostName;
    private final String sourceName;
    private final List<String> tags;

    /**
     * Default constructor for use by java.util.logging - retrieves configuration
     * from logging properties:
     *
     * <li><pre>com.comoyo.logging.context.json.JsonEventFormatter.source_host</pre>: explicit
     *      configuration of host name in log entries - default auto-determined.
     * <li><pre>com.comoyo.logging.context.json.JsonEventFormatter.source</pre>: Log source, i.e.
     *      application name
     * <li><pre>com.comoyo.logging.context.json.JsonEventFormatter.tags</pre>: Comma/space separated
     *      list of strings to tag log entries with, i.e. runtime environment
     * <li><pre>com.comoyo.logging.context.json.JsonEventFormatter.pretty</pre>: If "true" or "yes",
     *      output JSON formatted with indentation and line breaks for human consumption. Possibly
     *      useful for debugging, not recommended for machine consumption.
     */
    public JsonEventFormatter() {
        Optional<String> sourceHostProperty = getLoggerProperty(SOURCEHOST_PROPERTY);
        if (sourceHostProperty.isPresent()) {
            hostName = sourceHostProperty.get();
        } else {
            String foundHostName;
            try {
                foundHostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                foundHostName = "unknown-host";
            }
            hostName = foundHostName;
        }

        sourceName = getLoggerProperty(SOURCE_PROPERTY).or("java");

        final String[] configuredTags = getLoggerProperty(TAG_PROPERTY).or("").split(TAG_SEPARATOR_REGEX);
        this.tags = Arrays.asList(configuredTags);

        final String prettyProperty = getLoggerProperty(PRETTY_PROPERTY).or("false");
        final HashMap<String, String> jsonConfig = new HashMap<>(1);
        if ("true".equalsIgnoreCase(prettyProperty) || "yes".equalsIgnoreCase(prettyProperty)) {
            jsonConfig.put(JsonGenerator.PRETTY_PRINTING, prettyProperty);
        }
        jsonFactory = Json.createGeneratorFactory(jsonConfig);
    }

    /**
     * Construct a JSON log event formatter with explicit configuration, without looking at the
     * java.util.logging properties.
     * @param hostName Host name to use in log entries.
     * @param source Log source - i.e. application name.
     * @param tags Strings to tag log entries with, i.e. runtime environment: "production", "test"
     * @param prettyJson If true; output more human-readable JSON. Not recommended for machine
     * consumption, but perhaps more convenient when debugging.
     * @param jsonFactory - a JsonFactory configured to your liking, i.e.
     * <pre>
     * final HashMap<String, String> jsonConfig = new HashMap<>(1);
     * jsonConfig.put(JsonGenerator.PRETTY_PRINTING, "true");
     * final JsonFactory jsonFactory = Json.createGeneratorFactory(jsonConfig);
     * </pre>
     * to get pretty-printed JSON with whitespace and indentation.
     */
    public JsonEventFormatter(final String hostName, final String source, final List<String> tags, JsonGeneratorFactory jsonFactory) {
        this.hostName = hostName;
        sourceName = source;
        this.tags = tags;
        this.jsonFactory = jsonFactory;
    }

    /**
     * {@inheritDoc}
     * @param record A Java log record.
     * @return A string containing a logstash-style JSON log event.
     */
    @Override
    public String format(final LogRecord record) {
        final SimpleDateFormat dateFormatter = (SimpleDateFormat) dateFormatterMaster.clone();
        final String timeStampString = dateFormatter.format(new Date(record.getMillis()));

        final String formattedMessage = formatMessage(record);

        final StringWriter writer = new StringWriter();
        final JsonGenerator json = jsonFactory.createGenerator(writer);
        {
            json.writeStartObject()
                    .write(JSON_KEY_VERSION, LOGSTASH_JSON_VERSION)
                    .write(JSON_KEY_TIMESTAMP, timeStampString)
                    .write(JSON_KEY_SOURCE_HOST, hostName)
                    .write(JSON_KEY_SOURCE, sourceName)
                    .write(JSON_KEY_LEVEL, record.getLevel().getName())
                    .write(JSON_KEY_MESSAGE, formattedMessage);

            if (null != record.getLoggerName()) {
                json.write(JSON_KEY_LOGGER_NAME, record.getLoggerName());
            }
            if (null != record.getSourceClassName()) {
                json.write(JSON_KEY_SOURCE_CLASS, record.getSourceClassName());
            }
            if (null != record.getSourceMethodName()) {
                json.write(JSON_KEY_SOURCE_METHOD, record.getSourceMethodName());
            }

            if (!tags.isEmpty()) {
                json.writeStartArray("tags");
                for (String tag: tags) {
                    json.write(tag);
                }
                json.writeEnd();
            }

            final Optional<Throwable> thrown = Optional.fromNullable(record.getThrown());
            if (thrown.isPresent()) {
                json.writeStartObject(JSON_KEY_EXCEPTION);
                boolean withDescription = true;
                writeThrowableToJson(json, thrown.get(), withDescription);
                json.writeEnd();
            }

            final Optional<Map<String, String> > context = thrown.isPresent() ?
                    LoggingContext.getLastEnteredContext() :
                    LoggingContext.getContext();
            if (context.isPresent()) {
                writeContextToJson(json, context.get());
            }
            json.writeEnd();
        }
        json.flush();
        return writer.toString();
    }

    /**
     * Write a set of key/value pairs to JSON object called "context". No output is made
     * if there are no key/value pairs.
     * @param json A JsonGenerator to output the JSON object to.
     * @param context A map to get the context data from.
     */
    private static void writeContextToJson(JsonGenerator json, Map<String, String> context) {
        if (!context.isEmpty()) {
            json.writeStartObject(JSON_KEY_CONTEXT);
            for (Map.Entry<String, String> entry: context.entrySet()) {
                json.write(entry.getKey(), entry.getValue());
            }
            json.writeEnd();
        }
    }

    /**
     * Write a Throwable to a JSON generator stream. In the normal case a trivial
     * affair, but as Throwables can have both causes (recursively) and zero or more
     * suppressed "sibling Throwables", the rabbit hole is potentially deep. JSON is a pretty good
     * format for representing all of this, though.
     *
     * The description field is skipped on all nested/suppressed exceptions, as their stack traces
     * are already contained in the description of the root exception.
     * 
     * Assumes that the start of the object has already been written, and that the end of the object
     * will be written after its return. (To facilitate writing Throwables in different contexts,
     * which is exploited when this function calls itself to write Throwables in both sub-objects
     * and JSON arrays).
     *
     * @param json A JsonGenerator to output the JSON structure to.
     * @param thrown A Throwable to process.
     */
    private static void writeThrowableToJson(
            final JsonGenerator json, final Throwable thrown, boolean withDescription) {
        final Optional<String> exceptionClass = Optional.fromNullable(thrown.getClass().getCanonicalName());
        final Optional<String> exceptionMessage = Optional.fromNullable(thrown.getMessage());

        if (exceptionClass.isPresent()) {
            json.write(JSON_KEY_EXCEPTION_CLASS, exceptionClass.get());
        }
        if (exceptionMessage.isPresent()) {
            json.write(JSON_KEY_EXCEPTION_MESSAGE, exceptionMessage.get());
        }
        if (withDescription) {
            final StringWriter stackWriter = new StringWriter();
            thrown.printStackTrace(new PrintWriter(stackWriter));
            String stack = stackWriter.toString();

            if (!stack.isEmpty()) {
                json.write(JSON_KEY_EXCEPTION_STACKTRACE, stack);
            }
        }

        final boolean noDescription = false;
        final Throwable[] suppressed = thrown.getSuppressed();
        if (null != suppressed && suppressed.length > 0) {
                json.writeStartArray(JSON_KEY_SUPPRESSED);
                for (Throwable oneSuppressed: suppressed) {
                    json.writeStartObject();
                    writeThrowableToJson(json, oneSuppressed, noDescription);
                    json.writeEnd();
                }
                json.writeEnd();
        }

        final Throwable cause = thrown.getCause();
        if (null != cause) {
            json.writeStartObject(JSON_KEY_CAUSE);
            writeThrowableToJson(json, cause, noDescription);
            json.writeEnd();
        }
    }

    private static Optional<String> getLoggerProperty(final String propertyName) {
        return Optional.fromNullable(LogManager.getLogManager().getProperty(PROPERTY_PREFIX + propertyName));
    }

    // Package private accessors for test usage.
    String getHostName() {
        return this.hostName;
    }
    String getSourceName() {
        return this.sourceName;
    }
    List<String> getTags() {
        return new ArrayList<>(this.tags);
    }
}
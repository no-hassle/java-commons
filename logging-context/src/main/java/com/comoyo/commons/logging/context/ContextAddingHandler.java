package com.comoyo.commons.logging.context;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Wrapper class that will enrich log messages passing through wrapped
 * {@link Handler}s with context information available through the
 * {@link LoggingContext} interface.
 *
 */
public class ContextAddingHandler
    extends Handler
{
    private final Handler wrapped;

    private ContextAddingHandler(final Handler wrapped)
    {
        this.wrapped = wrapped;
    }

    /**
     * Enrich messages passing through a single handler with context information.
     *
     * @param handler the underlying logging handler
     * @return a version of the given handler that also logs context information
     */
    public static Handler wrapHandler(final Handler handler)
    {
        return new ContextAddingHandler(handler);
    }

    /**
     * Enrich messages passing through all {@link Handler}s of a given
     * logger with context information.  This modifies the set of
     * handlers for the given logger.  Handlers added after this
     * function has been called are not affected.  Calling this
     * function multiple times is supported, and will not affect
     * already modified handlers in this logger's handler set.
     *
     * @param logger the logger to modify
     */
    public static void wrapAllHandlers(final Logger logger)
    {
        final Handler[] handlers = logger.getHandlers();
        for (final Handler handler : handlers) {
            if (!(handler instanceof ContextAddingHandler)) {
                logger.removeHandler(handler);
                logger.addHandler(wrapHandler(handler));
            }
        }
    }

    private static String escapeString(final String string)
    {
        return string.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
    }

    private static LogRecord addContextToRecord(final LogRecord original)
    {
        final Map<String, String> context
            = original.getThrown() == null
            ? LoggingContext.getContext()
            : LoggingContext.getLastEnteredContext();
        if (context == null) {
            return original;
        }

        final ResourceBundle bundle = original.getResourceBundle();
        final String message = original.getMessage();
        String localized = message;
        if (message == null) {
            localized = "";
        }
        else {
            if (bundle != null) {
                try {
                    localized = bundle.getString(message);
                }
                catch (MissingResourceException e) {
                    localized = message;
                }
            }
            else {
                localized = message;
            }
        }

        final StringBuilder sb = new StringBuilder(localized);
        sb.append(" | context: {");
        boolean first = true;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"")
                .append(escapeString(entry.getKey()))
                .append("\": \"")
                .append(escapeString(entry.getValue()))
                .append("\"");
            first = false;
        }
        sb.append("}");

        final LogRecord record = new LogRecord(original.getLevel(), sb.toString());
        record.setLevel(original.getLevel());
        record.setLoggerName(original.getLoggerName());
        record.setMillis(original.getMillis());
        record.setParameters(original.getParameters());
        record.setSequenceNumber(original.getSequenceNumber());
        record.setSourceClassName(original.getSourceClassName());
        record.setSourceMethodName(original.getSourceMethodName());
        record.setThreadID(original.getThreadID());
        record.setThrown(original.getThrown());
        return record;
    }

    @Override
    public void publish(final LogRecord record)
    {
        wrapped.publish(addContextToRecord(record));
    }

    @Override
    public void close()
    {
        wrapped.close();
    }

    @Override
    public void flush()
    {
        wrapped.flush();
    }

    @Override
    public String getEncoding()
    {
        return wrapped.getEncoding();
    }

    @Override
    public ErrorManager getErrorManager()
    {
        return wrapped.getErrorManager();
    }

    @Override
    public Filter getFilter()
    {
        return wrapped.getFilter();
    }

    @Override
    public Formatter getFormatter()
    {
        return wrapped.getFormatter();
    }

    @Override
    public Level getLevel()
    {
        return wrapped.getLevel();
    }

    @Override
    public boolean isLoggable(final LogRecord record)
    {
        return wrapped.isLoggable(record);
    }

    @Override
    public void setEncoding(final String encoding)
        throws UnsupportedEncodingException
    {
        wrapped.setEncoding(encoding);
    }

    @Override
    public void setErrorManager(final ErrorManager em)
    {
        wrapped.setErrorManager(em);
    }

    @Override
    public void setFilter(final Filter newFilter)
    {
        wrapped.setFilter(newFilter);
    }

    @Override
    public void setFormatter(final Formatter newFormatter)
    {
        wrapped.setFormatter(newFormatter);
    }

    @Override
    public void setLevel(final Level newLevel)
    {
        wrapped.setLevel(newLevel);
    }
}

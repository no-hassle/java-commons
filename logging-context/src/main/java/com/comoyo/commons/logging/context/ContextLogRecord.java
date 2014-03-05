package com.comoyo.commons.logging.context;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;

// package-private
class ContextLogRecord
    extends LogRecord
{
    private static final long serialVersionUID = -8393898638163295583L;
    private final LogRecord wrapped;
    private final Optional<Map<String, String>> context;

    public ContextLogRecord(final LogRecord record, final Optional<Map<String, String>> fields)
    {
        super(record.getLevel(), record.getMessage());
        this.wrapped = record;
        this.context = fields;
    }

    private String escapeString(final String string)
    {
        return string.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
    }

    @Override
    public String getMessage()
    {
        final String message = wrapped.getMessage();
        if (!context.isPresent()) {
            return message;
        }
        final ResourceBundle bundle = wrapped.getResourceBundle();
        final String localized
            = bundle != null
            ? bundle.getString(message)
            : null;
        final StringBuilder sb = new StringBuilder(
            localized != null
            ? localized
            : message);
        sb.append(" | context: {");
        boolean first = true;
        for (Map.Entry<String, String> entry : context.get().entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"" + escapeString(entry.getKey()) + "\": \""
                      + escapeString(entry.getValue()) + "\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Level getLevel()
    {
        return wrapped.getLevel();
    }

    @Override
    public String getLoggerName()
    {
        return wrapped.getLoggerName();
    }

    @Override
    public long getMillis()
    {
        return wrapped.getMillis();
    }

    @Override
    public Object[] getParameters()
    {
        return wrapped.getParameters();
    }

    @Override
    public ResourceBundle getResourceBundle()
    {
        return wrapped.getResourceBundle();
    }

    @Override
    public String getResourceBundleName()
    {
        return wrapped.getResourceBundleName();
    }

    @Override
    public long getSequenceNumber()
    {
        return wrapped.getSequenceNumber();
    }

    @Override
    public String getSourceClassName()
    {
        return wrapped.getSourceClassName();
    }

    @Override
    public String getSourceMethodName()
    {
        return wrapped.getSourceMethodName();
    }

    @Override
    public int getThreadID()
    {
        return wrapped.getThreadID();
    }

    @Override
    public Throwable getThrown()
    {
        return wrapped.getThrown();
    }

    @Override
    public void setLevel(final Level level)
    {
        wrapped.setLevel(level);
    }

    @Override
    public void setLoggerName(final String name)
    {
        wrapped.setLoggerName(name);
    }

    @Override
    public void setMessage(final String message)
    {
        wrapped.setMessage(message);
    }

    @Override
    public void setMillis(final long millis)
    {
        wrapped.setMillis(millis);
    }

    @Override
    public void setParameters(final Object[] parameters)
    {
        wrapped.setParameters(parameters);
    }

    @Override
    public void setResourceBundle(final ResourceBundle bundle)
    {
        wrapped.setResourceBundle(bundle);
    }

    @Override
    public void setResourceBundleName(final String name)
    {
        wrapped.setResourceBundleName(name);
    }

    @Override
    public void setSequenceNumber(final long seq)
    {
        wrapped.setSequenceNumber(seq);
    }

    @Override
    public void setSourceClassName(final String sourceClassName)
    {
        wrapped.setSourceClassName(sourceClassName);
    }

    @Override
    public void setSourceMethodName(final String sourceMethodName)
    {
        wrapped.setSourceMethodName(sourceMethodName);
    }

    @Override
    public void setThreadID(final int threadID)
    {
        wrapped.setThreadID(threadID);
    }

    @Override
    public void setThrown(final Throwable thrown)
    {
        wrapped.setThrown(thrown);
    }
}

package com.comoyo.commons.logging.context.gelf;

import biz.paluch.logging.gelf.jul.GelfLogHandler;
import biz.paluch.logging.gelf.intern.GelfMessage;
import com.comoyo.commons.logging.context.LoggingContext;

import java.util.Map;
import java.util.logging.LogRecord;

/**
 * Derivative of {@link GelfLogHandler} that enriches log messages
 * with data from {@link LoggingContext}.
 *
 */
public class ContextAwareGelfLogHandler
    extends GelfLogHandler
{
    @Override
    protected GelfMessage createGelfMessage(final LogRecord record) {
        final GelfMessage message = super.createGelfMessage(record);
        final Map<String, String> ctxFields
            = record.getThrown() == null
            ? LoggingContext.getContext()
            : LoggingContext.getLastEnteredContext();
        if (ctxFields != null) {
            final Map<String, String> logFields = message.getAdditonalFields();
            for (Map.Entry<String, String> entry : ctxFields.entrySet()) {
                logFields.put("context." + entry.getKey(), entry.getValue());
            }
        }
        return message;
    }
}

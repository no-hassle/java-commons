package com.comoyo.commons.logging.context;

import java.util.Map;

public interface LoggingContextFactory
{
    /**
     * See {@link LoggingContext#openContext}
     */
    LoggingContext.Scope openContext();

    /**
     * See {@link LoggingContext#currentContext}
     */
    LoggingContext.Scope currentContext();

    /**
     * See {@link LoggingContext#getContext}
     */
    Map<String, String> getContext();

    /**
     * See {@link LoggingContext#getLastEnteredContext}
     */
    Map<String, String> getLastEnteredContext();
}

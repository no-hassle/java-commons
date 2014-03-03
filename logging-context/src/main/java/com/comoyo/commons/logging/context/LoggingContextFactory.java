package com.comoyo.commons.logging.context;

import com.google.common.base.Optional;
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
    Optional<Map<String, String>> getContext();

    /**
     * See {@link LoggingContext#getLastEnteredContext}
     */
    Optional<Map<String, String>> getLastEnteredContext();
}

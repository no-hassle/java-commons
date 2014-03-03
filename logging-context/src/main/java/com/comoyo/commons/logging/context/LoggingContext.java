package com.comoyo.commons.logging.context;

import com.google.common.base.Optional;
import java.util.Map;

public class LoggingContext
{
    public interface Scope
        extends AutoCloseable
    {
        /**
         * Set a [key, value] pair for this context.  If the context was
         * obtained using {@link #currentContext} when no opened context
         * was in scope, does nothing.  If either key or value is null,
         * does nothing.
         */
        void addField(final String key, final String value);

        @Override
        void close();
    }

    private static LoggingContextFactory factory = new BasicLoggingContextFactory();

    private LoggingContext()
    {
    }

    /**
     * Get the actual {@link LoggingContextFactory} currently in use.
     *
     * @return current factory
     */
    public static LoggingContextFactory getFactory()
    {
        return factory;
    }

    /**
     * Set the {@link LoggingContextFactory} to use.
     *
     * @param newFactory logging context factory to use.
     */
    public static void setFactory(final LoggingContextFactory newFactory)
    {
        factory = newFactory;
    }

    /**
     * Create a new logging context.  If a context is already
     * established, inherits field values from existing context.
     * Fields added inside the scope if this context instance will be
     * abandoned when it is closed.
     *
     * @return logging context scope
     */
    public static Scope openContext()
    {
        return factory.openContext();
    }

    /**
     * Access existing logging context.  Updates to the context will
     * be kept when this context instance is closed.
     *
     * @return logging context scope
     */
    public static Scope currentContext()
    {
        return factory.currentContext();
    }

    /**
     * Get the populated fields for the current context.  If no
     * context has been established using {@link #openContext},
     * returns absent value.
     * @return optionally, map of [key, value] pairs for the current
     * context.
     */
    public static Optional<Map<String, String>> getContext()
    {
        return factory.getContext();
    }

    /**
     * Get the populated fields for the last opened context.  If no
     * context has been established using {@link #openContext},
     * returns absent value.  This differs from {@link #getContext} in
     * that it may return context for a scope that has been abandoned.
     * This function is primarily useful when logging an event at a
     * different level then where it occurred, e.g an exception.
     *
     * @return optionally, map of [key, value] pairs for the last
     * entered context.
     */
    public static Optional<Map<String, String>> getLastEnteredContext()
    {
        return factory.getLastEnteredContext();
    }
}

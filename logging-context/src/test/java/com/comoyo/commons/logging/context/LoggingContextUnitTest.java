package com.comoyo.commons.logging.context;

import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoggingContextUnitTest
{
    @Test
    public void testNoContext()
    {
        Optional<Map<String, String>> fields = LoggingContext.getContext();
        assertFalse("Unset context was not absent", fields.isPresent());
    }

    @Test
    public void testUpdateMissingContext()
    {
        try (final LoggingContext.Scope context = LoggingContext.currentContext()) {
            assertNotNull("Established context was null", LoggingContext.getContext());
            context.addField("key", "value");
            assertFalse("Unopened context was present", LoggingContext.getContext().isPresent());
        }
    }

    @Test
    public void testSimpleContext()
    {
        try (final LoggingContext.Scope context = LoggingContext.openContext()) {
            assertNotNull("Established context was null", LoggingContext.getContext());
            context.addField("key", "value");
            final Optional<Map<String, String>> fields = LoggingContext.getContext();
            assertTrue("No context present", fields.isPresent());
            assertEquals("Context did not contain established key", "value", fields.get().get("key"));
        }
        assertFalse("Abandoned context was not absent", LoggingContext.getContext().isPresent());
    }

    @Test
    public void testNestedContext()
    {
        try (final LoggingContext.Scope context1 = LoggingContext.openContext()) {
            context1.addField("key1", "value1");
            try (final LoggingContext.Scope context2 = LoggingContext.openContext()) {
                context2.addField("key2", "value2");
                final Optional<Map<String, String>> fields = LoggingContext.getContext();
                assertTrue("No context present", fields.isPresent());
                assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
                assertEquals("Context did not contain established key", "value2", fields.get().get("key2"));
            }
            final Optional<Map<String, String>> fields = LoggingContext.getContext();
            assertTrue("No context present", fields.isPresent());
            assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
            assertEquals("Context contained key from abandoned frame", null, fields.get().get("key2"));

            final Optional<Map<String, String>> entered = LoggingContext.getLastEnteredContext();
            assertTrue("No last entered context present", entered.isPresent());
            assertEquals("Last entered context did not contain established key",
                         "value1", entered.get().get("key1"));
            assertEquals("Last entered context did not contain established key",
                         "value2", entered.get().get("key2"));
        }
        assertFalse("Abandoned context was not absent", LoggingContext.getContext().isPresent());
        assertFalse("Abandoned last entered context was not absent",
                    LoggingContext.getLastEnteredContext().isPresent());
    }

    @Test
    public void testNestedSameContext()
    {
        try (final LoggingContext.Scope context1 = LoggingContext.openContext()) {
            context1.addField("key1", "value1");
            try (final LoggingContext.Scope context2 = LoggingContext.currentContext()) {
                context2.addField("key2", "value2");
                final Optional<Map<String, String>> fields = LoggingContext.getContext();
                assertTrue("No context present", fields.isPresent());
                assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
                assertEquals("Context did not contain established key", "value2", fields.get().get("key2"));
            }
            final Optional<Map<String, String>> fields = LoggingContext.getContext();
            assertTrue("No context present", fields.isPresent());
            assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
            assertEquals("Context did not contain established key", "value2", fields.get().get("key2"));
        }
        assertFalse("Abandoned context was not absent", LoggingContext.getContext().isPresent());
    }

    @Test
    public void testLexicalThreadedContext()
        throws Exception
    {
        try (final LoggingContext.Scope context = LoggingContext.openContext()) {
            context.addField("key1", "value1");
            final FutureTask<AssertionError> future = new FutureTask<>(new Callable<AssertionError>() {
                @Override
                public AssertionError call()
                {
                    context.addField("key2", "value2");
                    final Optional<Map<String, String>> fields = LoggingContext.getContext();
                    try {
                        assertTrue("No context present", fields.isPresent());
                        assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
                        assertEquals("Context did not contain established key", "value2", fields.get().get("key2"));
                    }
                    catch (final AssertionError e) {
                        return e;
                    }
                    return null;
                }
            });

            final Thread thread = new Thread(future);
            thread.start();
            thread.join();
            final AssertionError result = future.get();
            if (result != null) {
                throw result;
            }

            final Optional<Map<String, String>> fields = LoggingContext.getContext();
            assertTrue("No context present", fields.isPresent());
            assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
            assertEquals("Context did not contain established key", "value2", fields.get().get("key2"));
        }
        assertFalse("Abandoned context was not absent", LoggingContext.getContext().isPresent());
    }

    @Test
    public void testExplicitThreadedContext()
        throws Exception
    {
        try (final LoggingContext.Scope context1 = LoggingContext.openContext()) {
            context1.addField("key1", "value1");
            final FutureTask<AssertionError> future = new FutureTask<>(new Callable<AssertionError>() {
                @Override
                public AssertionError call()
                {
                    try (final LoggingContext.Scope context2 = LoggingContext.openContext()) {
                        context2.addField("key2", "value2");
                        final Optional<Map<String, String>> fields = LoggingContext.getContext();
                        assertTrue("No context present", fields.isPresent());
                        assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
                        assertEquals("Context did not contain established key", "value2", fields.get().get("key2"));
                    }
                    catch (final AssertionError e) {
                        return e;
                    }
                    return null;
                }
            });

            final Thread thread = new Thread(future);
            thread.start();
            thread.join();
            final AssertionError result = future.get();
            if (result != null) {
                throw result;
            }

            final Optional<Map<String, String>> fields = LoggingContext.getContext();
            assertTrue("No context present", fields.isPresent());
            assertEquals("Context did not contain established key", "value1", fields.get().get("key1"));
            assertEquals("Context contained key from abandoned frame", null, fields.get().get("key2"));
        }
        assertFalse("Abandoned context was not absent", LoggingContext.getContext().isPresent());
    }
}

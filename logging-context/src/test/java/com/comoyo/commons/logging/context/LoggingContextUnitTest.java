package com.comoyo.commons.logging.context;

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
        Map<String, String> fields = LoggingContext.getContext();
        assertNull("Unset context was not absent", fields);
    }

    @Test
    public void testUpdateMissingContext()
    {
        try (final LoggingContext.Scope context = LoggingContext.currentContext()) {
            assertNull("Unestablished context was not null", LoggingContext.getContext());
            context.addField("key", "value");
            assertNull("Unopened context was present", LoggingContext.getContext());
        }
    }

    @Test
    public void testSimpleContext()
    {
        try (final LoggingContext.Scope context = LoggingContext.openContext()) {
            assertNotNull("Established context was null", LoggingContext.getContext());
            context.addField("key", "value");
            final Map<String, String> fields = LoggingContext.getContext();
            assertNotNull("No context present", fields);
            assertEquals("Context did not contain established key", "value", fields.get("key"));
        }
        assertNull("Abandoned context was not absent", LoggingContext.getContext());
    }

    @Test
    public void testNestedContext()
    {
        try (final LoggingContext.Scope context1 = LoggingContext.openContext()) {
            context1.addField("key1", "value1");
            try (final LoggingContext.Scope context2 = LoggingContext.openContext()) {
                context2.addField("key2", "value2");
                final Map<String, String> fields = LoggingContext.getContext();
                assertNotNull("No context present", fields);
                assertEquals("Context did not contain established key", "value1", fields.get("key1"));
                assertEquals("Context did not contain established key", "value2", fields.get("key2"));
            }
            final Map<String, String> fields = LoggingContext.getContext();
            assertNotNull("No context present", fields);
            assertEquals("Context did not contain established key", "value1", fields.get("key1"));
            assertEquals("Context contained key from abandoned frame", null, fields.get("key2"));

            final Map<String, String> entered = LoggingContext.getLastEnteredContext();
            assertNotNull("No last entered context present", entered);
            assertEquals("Last entered context did not contain established key",
                         "value1", entered.get("key1"));
            assertEquals("Last entered context did not contain established key",
                         "value2", entered.get("key2"));
        }
        assertNull("Abandoned context was not absent", LoggingContext.getContext());
        assertNull("Abandoned last entered context was not absent",
                   LoggingContext.getLastEnteredContext());
    }

    @Test
    public void testNestedSameContext()
    {
        try (final LoggingContext.Scope context1 = LoggingContext.openContext()) {
            context1.addField("key1", "value1");
            try (final LoggingContext.Scope context2 = LoggingContext.currentContext()) {
                context2.addField("key2", "value2");
                final Map<String, String> fields = LoggingContext.getContext();
                assertNotNull("No context present", fields);
                assertEquals("Context did not contain established key", "value1", fields.get("key1"));
                assertEquals("Context did not contain established key", "value2", fields.get("key2"));
            }
            final Map<String, String> fields = LoggingContext.getContext();
            assertNotNull("No context present", fields);
            assertEquals("Context did not contain established key", "value1", fields.get("key1"));
            assertEquals("Context did not contain established key", "value2", fields.get("key2"));
        }
        assertNull("Abandoned context was not absent", LoggingContext.getContext());
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
                    final Map<String, String> fields = LoggingContext.getContext();
                    try {
                        assertNotNull("No context present", fields);
                        assertEquals("Context did not contain established key", "value1", fields.get("key1"));
                        assertEquals("Context did not contain established key", "value2", fields.get("key2"));
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

            final Map<String, String> fields = LoggingContext.getContext();
            assertNotNull("No context present", fields);
            assertEquals("Context did not contain established key", "value1", fields.get("key1"));
            assertEquals("Context did not contain established key", "value2", fields.get("key2"));
        }
        assertNull("Abandoned context was not absent", LoggingContext.getContext());
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
                        final Map<String, String> fields = LoggingContext.getContext();
                        assertNotNull("No context present", fields);
                        assertEquals("Context did not contain established key", "value1", fields.get("key1"));
                        assertEquals("Context did not contain established key", "value2", fields.get("key2"));
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

            final Map<String, String> fields = LoggingContext.getContext();
            assertNotNull("No context present", fields);
            assertEquals("Context did not contain established key", "value1", fields.get("key1"));
            assertEquals("Context contained key from abandoned frame", null, fields.get("key2"));
        }
        assertNull("Abandoned context was not absent", LoggingContext.getContext());
    }
}

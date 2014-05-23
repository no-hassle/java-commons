/**
 * Copyright (C) 2014 Telenor Digital AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comoyo.commons.logging.context;

import java.io.ByteArrayOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContextAddingHandlerUnitTest
{
    private static final String LOG_MESSAGE1 = "This is a test";

    private ByteArrayOutputStream out;
    private Handler handler;
    private Logger log;

    @Before
    public void setUp()
    {
        out = new ByteArrayOutputStream();
        handler = new StreamHandler(out, new SimpleFormatter());
        log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
    }

    @Test
    public void testNoContext()
        throws Exception
    {
        log.addHandler(ContextAddingHandler.wrapHandler(handler));
        log.warning(LOG_MESSAGE1);
        handler.flush();
        final String output = out.toString("UTF-8");
        final String suffix = "\nWARNING: " + LOG_MESSAGE1 + "\n";
        assertTrue("Log message `" + output + "' did not end with `" + suffix + "'",
                   output.endsWith(suffix));
    }

    @Test
    public void testSimpleContext()
        throws Exception
    {
        log.addHandler(ContextAddingHandler.wrapHandler(handler));
        try (final LoggingContext.Scope context = LoggingContext.openContext()) {
            context.addField("key1", "value1");
            log.warning(LOG_MESSAGE1);
        }
        handler.flush();
        final String output = out.toString("UTF-8");
        final String suffix = "\nWARNING: " + LOG_MESSAGE1
            + " | context: {\"key1\": \"value1\"}\n";
        assertTrue("Log message `" + output + "' did not end with `" + suffix + "'",
                   output.endsWith(suffix));
    }

    @Test
    public void testWrapAll()
        throws Exception
    {
        log.addHandler(handler);
        ContextAddingHandler.wrapAllHandlers(log);
        try (final LoggingContext.Scope context = LoggingContext.openContext()) {
            context.addField("key1", "value1");
            log.warning(LOG_MESSAGE1);
        }
        handler.flush();
        final String output = out.toString("UTF-8");
        final String suffix = "\nWARNING: " + LOG_MESSAGE1
            + " | context: {\"key1\": \"value1\"}\n";
        assertTrue("Log message `" + output + "' did not end with `" + suffix + "'",
                   output.endsWith(suffix));
    }

    @Test
    public void testExitedContext()
        throws Exception
    {
        log.addHandler(ContextAddingHandler.wrapHandler(handler));
        try (final LoggingContext.Scope context = LoggingContext.openContext()) {
            context.addField("key1", "value1");
        }
        finally {
            log.warning(LOG_MESSAGE1);
        }
        handler.flush();
        final String output = out.toString("UTF-8");
        final String suffix = "\nWARNING: " + LOG_MESSAGE1 + "\n";
        assertTrue("Log message `" + output + "' did not end with `" + suffix + "'",
                   output.endsWith(suffix));
    }

    @Test
    public void testExceptionContext()
        throws Exception
    {
        log.addHandler(ContextAddingHandler.wrapHandler(handler));
        try (final LoggingContext.Scope context1 = LoggingContext.openContext()) {
            try (final LoggingContext.Scope context2 = LoggingContext.openContext()) {
                context2.addField("key1", "value1");
                throw new Exception("exception");
            }
            catch (Exception e) {
                log.log(Level.WARNING, LOG_MESSAGE1, e);
            }
        }
        handler.flush();
        final String output = out.toString("UTF-8");
        final String substring = "\nWARNING: " + LOG_MESSAGE1
            + " | context: {\"key1\": \"value1\"}\n"
            + "java.lang.Exception: exception\n";
        assertTrue("Log message `" + output + "' did not contain `" + substring + "'",
                   output.indexOf(substring) >= 0);
    }
}

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicLoggingContextFactory
    implements LoggingContextFactory
{
    private final InheritableThreadLocal<Map<String, String>> currentContext
        = new InheritableThreadLocal<Map<String, String>>();
    private final InheritableThreadLocal<Map<String, String>> lastEnteredContext
        = new InheritableThreadLocal<Map<String, String>>();

    private class Scope
        implements LoggingContext.Scope
    {
        private Map<String, String> currentFields;
        private Map<String, String> parentFields;

        public Scope(
            final Map<String, String> currentFields,
            final Map<String, String> parentFields)
        {
            this.currentFields = currentFields;
            this.parentFields = parentFields;
        }

        @Override
        public void addField(final String key, final String value)
        {
            if (currentFields != null && key != null && value != null) {
                currentFields.put(key, value);
            }
        }

        @Override
        public void close()
        {
            if (parentFields != null) {
                currentContext.set(parentFields);
            }
            else {
                currentContext.remove();
                lastEnteredContext.remove();
            }
        }
    }

    public BasicLoggingContextFactory()
    {
    }

    public LoggingContext.Scope openContext()
    {
        final Map<String, String> parentFields = currentContext.get();
        final Map<String, String> currentFields
            = parentFields == null
            ? new ConcurrentHashMap<String, String>()
            : new ConcurrentHashMap<String, String>(parentFields);
        currentContext.set(currentFields);
        lastEnteredContext.set(currentFields);
        return new Scope(currentFields, parentFields);
    }

    public LoggingContext.Scope currentContext()
    {
        final Map<String, String> currentFields = currentContext.get();
        return new Scope(currentFields, currentFields);
    }

    public Map<String, String> getContext()
    {
        return currentContext.get();
    }

    public Map<String, String> getLastEnteredContext()
    {
        return lastEnteredContext.get();
    }
}

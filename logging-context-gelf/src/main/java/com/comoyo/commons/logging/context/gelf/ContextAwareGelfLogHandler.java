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

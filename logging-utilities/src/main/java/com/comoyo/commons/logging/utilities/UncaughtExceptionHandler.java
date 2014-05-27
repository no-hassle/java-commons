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

package com.comoyo.commons.logging.utilities;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uncaught exception handler which logs exceptions using JUL.
 */
public final class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger logger = Logger.getLogger(UncaughtExceptionHandler.class.getName());

    private UncaughtExceptionHandler() {}

    public void uncaughtException(Thread thread, Throwable throwable) {
        if (throwable instanceof ThreadDeath) {
            return;
        }
        try {
            logger.log(Level.WARNING, "Uncaught exception in thread \""
                    + thread.getName() + "\" ", throwable);
        } catch (Error e) {
            // Desperation attempt to not lose log messages, in case of error
            // try to log it on sdterr.
            // This logging matches the default behaviour if no UncaughtExceptionHandler
            // is registered.
            if (!(e instanceof ThreadDeath)) {
                System.err.print("Exception when logging uncaught exception");
                e.printStackTrace(System.err);
            }
            System.err.print("Uncaught exception in thread \""
                    + thread.getName() + "\" ");
            throwable.printStackTrace(System.err);
            if (e instanceof ThreadDeath) {
                throw e;
            }
        }
    }

    /**
     * Install UncaughtExceptionHandler as the default uncaught exception handler.
     */
    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    }
}

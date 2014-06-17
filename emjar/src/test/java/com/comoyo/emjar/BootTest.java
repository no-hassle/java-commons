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

package com.comoyo.emjar;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class BootTest
{
    private boolean mainCalled = false;

    @Test
    public void testBoot()
        throws Exception
    {
        final LoggingClassLoader loader = new LoggingClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        final Properties props = new Properties();
        props.setProperty(Boot.EMJAR_MAIN_CLASS_PROP, BootTest.class.getName());
        final Class<?> bootClass = Class.forName(Boot.class.getName(), false, loader);
        final Method bootMain = bootClass.getDeclaredMethod("findMain", Properties.class);
        final Object result = bootMain.invoke(null, new Object[]{props});
        assertTrue("findMain did not return a Method object",
                   result instanceof Method);
        final Method realMain = (Method) result;
        realMain.invoke(this, new Object[]{new String[0]});
        assertTrue("invocation of findMain returned method did not succeed",
                   mainCalled);
        final List<String> loaded = loader.getLoaded();
        for (String name : loaded) {
            if (name.startsWith("java.util.logging")) {
                fail("Boot accessed logging packages during initialization: " + name);
            }
            if (!name.startsWith("java.")
                && !name.startsWith(BootTest.class.getPackage().getName()))
            {
                fail("Boot accessed non-standard packages during initialization: " + name);
            }
        }
    }

    public void main(String[] args)
        throws Exception
    {
        mainCalled = true;
    }

    static class LoggingClassLoader extends URLClassLoader
    {
        private final LinkedList<String> loaded = new LinkedList<>();

        public LoggingClassLoader()
            throws Exception
        {
            super(defaultClassPath(), null);
        }

        private static URL[] defaultClassPath()
            throws Exception
        {
            final String classPath = System.getProperties().getProperty("java.class.path");
            final ArrayList<URL> urls = new ArrayList<>();
            for (String elem : classPath.split(File.pathSeparator)) {
                final File file = new File(elem);
                urls.add(file.toURI().toURL());
            }
            return urls.toArray(new URL[0]);
        }

        @Override
        public Class<?> loadClass(String name)
            throws ClassNotFoundException
        {
            loaded.add(name);
            return super.loadClass(name);
        }

        public List<String> getLoaded()
        {
            return loaded;
        }
    }
}

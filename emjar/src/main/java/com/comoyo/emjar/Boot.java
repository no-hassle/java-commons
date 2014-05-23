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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Dispatching main class that allows for <strong><code>java -jar
 * bundle.jar</code></strong> invocation of bundled jars.  To use:
 *
 * <ul>
 * <li>Configure the bundle jar file's manifest attribute
 * <strong><code>Main-Class</code></strong> as
 * <strong><code>com.comoyo.emjar.Boot</code></strong>.</li>
 *
 * <li>Specify the actual main class using the
 * <strong><code>EmJar-Main-Class</code></strong> manifest attribute
 * or the <strong><code>emjar.main.class</code></strong>
 * property.</li>
 * </ul>
 *
 * <p/>
 * (This class relies on reflection and some slightly questionable
 * practices to hook the EmJar class loader into the system.  See also
 * {@link EmJarClassLoader} for an approach using explicit
 * configuration that avoids this.)
 *
 */
public class Boot
{
    public final static String EMJAR_MAIN_CLASS_PROP = "emjar.main.class";
    public final static String EMJAR_MAIN_CLASS_ATTR = "EmJar-Main-Class";
    public final static String EMJAR_SYSTEM_PROPS_PROP = "emjar.system.properties";
    public final static String EMJAR_SYSTEM_PROPS_ATTR = "EmJar-System-Properties";

    public static void main(String[] args)
        throws Exception
    {
        final EmJarClassLoader loader = new EmJarClassLoader(ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(loader);

        try {
            // Make a best-effort attempt to update the system class
            // loader.  This is required to allow e.g LogManager to
            // find classes inside embedded jars when parsing files
            // given using the java.util.logging.config.file property.
            final Field systemClassLoader = ClassLoader.class.getDeclaredField("scl");
            systemClassLoader.setAccessible(true);
            systemClassLoader.set(null, loader);
        }
        catch (NoSuchFieldException e) {
            // Unable to set system class loader; continuing anyway
        }

        final String systemPropsName
            = System.getProperty(EMJAR_SYSTEM_PROPS_PROP,
                                 getManifestAttribute(EMJAR_SYSTEM_PROPS_ATTR));
        if (systemPropsName != null) {
            final Properties props = System.getProperties();
            final InputStream is = loader.getResourceAsStream(systemPropsName);
            if (is != null) {
                props.load(new InputStreamReader(is));
                is.close();
            }
        }
        final String mainClassName
            = System.getProperty(EMJAR_MAIN_CLASS_PROP,
                                 getManifestAttribute(EMJAR_MAIN_CLASS_ATTR));
        if (mainClassName == null) {
            throw new RuntimeException(
                "No main class specified using "
                    + EMJAR_MAIN_CLASS_PROP + " or " + EMJAR_MAIN_CLASS_ATTR);
        }

        final Class<?> mainClass
            = Class.forName(mainClassName, false, loader);
        final Method mainMethod
            = mainClass.getDeclaredMethod("main", new Class[]{String[].class});
        mainMethod.invoke(null, new Object[]{args});
    }

    private static String getManifestAttribute(String key)
        throws IOException
    {
        final Enumeration<URL> manifests
            = Boot.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            final URL url = manifests.nextElement();
            final Manifest manifest = new Manifest(url.openStream());
            final Attributes attributes = manifest.getMainAttributes();
            final String value = attributes.getValue(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

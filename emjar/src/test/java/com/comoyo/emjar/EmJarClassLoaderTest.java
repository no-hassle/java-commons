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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.base.Joiner;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class EmJarClassLoaderTest extends EmJarTest
{
    private EmJarClassLoader testLoader()
        throws URISyntaxException
    {
        final FilenameFilter jarFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            };
        final File bundle = getResourceFile("bundle-s-large.jar");
        final File allJars[] = bundle.getParentFile().listFiles(jarFilter);

        final Properties props = new Properties();
        props.setProperty("java.class.path", Joiner.on(":").join(allJars));
        props.setProperty("path.separator", ":");
        props.setProperty("file.separator", "/");
        props.setProperty("user.dir", "/tmp");
        return new EmJarClassLoader(props);
    }

    @Test
    public void testClassPathQuoting()
        throws Exception
    {
        final EmJarClassLoader loader = testLoader();
        final InputStream is = loader.getResourceAsStream("entry-" + WEIRD + ".txt");
        final BufferedReader entry = new BufferedReader(new InputStreamReader(is));
        assertEquals("Contents mismatch for weird entry", WEIRD, entry.readLine());
    }

    @Test
    public void testOpenConnection()
        throws Exception
    {
        final EmJarClassLoader loader = testLoader();
        final URL urls[] = loader.getURLs();
        for (URL url : urls) {
            if ("jar".equals(url.getProtocol())) {
                final URLConnection conn = url.openConnection();
                assertTrue("Connection not of type JarURLConnection",
                           conn instanceof JarURLConnection);
                final JarURLConnection jarConn = (JarURLConnection) conn;
                final JarFile jarFile = jarConn.getJarFile();
                final Manifest mf = jarFile.getManifest();
                final String attr = mf.getMainAttributes().getValue("X-EmJar-Test");
                assertEquals("Invalid inner lib manifest structure",
                             "inner", attr);
                return;
            }
        }
        fail("Did not find any elements in classpath with protocol jar");
    }
}

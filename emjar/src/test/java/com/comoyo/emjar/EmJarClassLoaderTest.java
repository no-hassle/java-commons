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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.base.Joiner;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

@RunWith(JUnit4.class)
public class EmJarClassLoaderTest extends JarTest
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
        props.setProperty("java.class.path", Joiner.on(File.pathSeparator).join(allJars));
        return new EmJarClassLoader(props);
    }

    /**
     * In order to construct a URL, getResourceAsStream ultimately
     * calls sun.net.www.ParseUtil.encodePath().  This encodes the
     * name's char array to UTF-8 under the assumption that it is
     * UCS-2, not UTF-16.  Doing so causes bad encodings of non-BMP
     * entities.  Java 7 was able to process these wrongly encoded
     * strings and reconstruct the original char sequence, but Java 8
     * is not.  Thus the following, which more or less replicates the
     * functionality of getResourceAsStream in order to verify that at
     * least EmJar's handling of unicode is up to snuff.
     */
    private static InputStream getResourceAsStreamRobust(
            final URLClassLoader loader, final String searchName)
            throws IOException {
        for (final URL url : loader.getURLs()) {
            if (!"jar".equals(url.getProtocol())) {
                continue;
            }
            final URLConnection conn = url.openConnection();
            if (!(conn instanceof JarURLConnection)) {
                continue;
            }
            final JarURLConnection jarConn = (JarURLConnection) conn;
            final JarFile jarFile = jarConn.getJarFile();
            final Enumeration<JarEntry> it = jarFile.entries();
            while (it.hasMoreElements()) {
                final JarEntry je = it.nextElement();
                if (searchName.equals(je.getName())) {
                    return jarFile.getInputStream(je);
                }
            }
        }
        return null;
    }

    @Override
    public JarURLConnection getJarUrlConnection(File root, String jarName, String entryName)
        throws Exception
    {
        final Properties props = new Properties();
        props.setProperty("java.class.path", root.toString());
        final URLClassLoader loader = new EmJarClassLoader(props);
        final URL url = loader.findResource(entryName);
        assertNotNull("Entry " + entryName + " not found using " + root.toString(), url);
        final JarURLConnection conn = (JarURLConnection) url.openConnection();
        if (!jarName.equals("lib-signed.jar")) {
            assertEquals("Connection returned from load was not Ondemand",
                OndemandEmbeddedJar.Connection.class, conn.getClass());
        }
        return conn;
    }

    @Test
    public void testClassPathQuoting()
        throws Exception
    {
        assumeTrue("UTF-8".equals(System.getProperty("file.encoding")));
        final EmJarClassLoader loader = testLoader();
        final String searchName = "entry-" + WEIRD + ".txt";
        final InputStream is = getResourceAsStreamRobust(loader, searchName);
        assertNotNull("Did not find " + searchName + "  in classpath", is);
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

    @Test
    public void testBundleWithSignedContents()
        throws Exception
    {
        final JarURLConnection conn = getJarUrlConnection(
                getResourceFile("bundle-signed.jar"), "lib-signed.jar", "entry-signed.txt");
        final JarFile jar = conn.getJarFile();
        final JarEntry entry = jar.getJarEntry("entry-signed.txt");
        final Certificate[] certs = entry.getCertificates();
        assertNotNull("No certificates found for signed entry", certs);
        assertEquals("Unexpected number of certificates for signed entry",
                1, certs.length);
        assertEquals("Supplied certificate was of unexpected type",
                "X.509", certs[0].getType());
    }
}

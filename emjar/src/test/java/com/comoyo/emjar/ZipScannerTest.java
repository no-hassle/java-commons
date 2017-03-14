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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ZipScannerTest
    extends JarTest
{
    @Override
    public JarURLConnection getJarUrlConnection(File root, String jarName, String entryName)
        throws Exception
    {
        final ZipScanner scanner = new ZipScanner(root);
        final Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> desc
            = scanner.scan();
        final Map<String, OndemandEmbeddedJar.Descriptor> embedded
            = desc.get(jarName);
        assertNotNull("Descriptor entry for " + jarName + " was null", embedded);
        final URL rootUrl = new URL("jar:file:" + root.toString() + "!/");
        return new OndemandEmbeddedJar.Connection(rootUrl, root.toString(), embedded, entryName);
    }

    @Test
    public void testCompressedBundle()
        throws Exception
    {
        final File large = getResourceFile("bundle-Z-large.jar");
        final ZipScanner scanner = new ZipScanner(large);
        final Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> desc
            = scanner.scan();
        assertEquals("ZipScanner did not return empty set for compressed bundle",
                     0, desc.entrySet().size());
    }

    private void testBundle(String name)
        throws Exception
    {
        final File plain = getResourceFile("bundle-" + name + ".jar");
        final ZipScanner scanner = new ZipScanner(plain);
        final Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> desc
            = scanner.scan();
        final Map<String, OndemandEmbeddedJar.Descriptor> embedded
            = desc.get("lib-" + name + ".jar");
        assertNotNull("Descriptor entry for lib-" + name + ".jar was null", embedded);
        assertNotNull("File entry for entry-" + name + ".txt", embedded.get("entry-" + name + ".txt"));
    }

    @Test
    public void testPlainBundle()
        throws Exception
    {
        testBundle("s-large");
        testBundle("S-large");
    }
}

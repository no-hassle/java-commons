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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class OndemandEmbeddedJarTest
    extends JarTest
{
    public JarURLConnection getJarUrlConnection(File root, String jarName, String entryName)
        throws Exception
    {
        final ZipScanner scanner = new ZipScanner(root);
        final Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> desc
            = scanner.scan();
        assertNotNull("Descriptor returned from ZipScanner was null", desc);
        return new OndemandEmbeddedJar.Connection(
            new URI("jar:file", root.getPath() + "!/" + jarName + "!/", null).toURL(),
            root.getPath(),
            desc.get(jarName),
            entryName);
    }

    @Test
    public void testLargeBundle()
        throws Exception
    {
        for (String s : new String[]{"s", "S"}) {
            final JarFile jar = testJarBundle(s + "-large");
            final InputStream is = jar.getInputStream(new JarEntry("oversize"));
            assertNotNull("oversize entry was unexpectedly filtered out from " + s + " results", is);
        }
    }
}

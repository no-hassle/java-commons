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
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public abstract class JarTest extends EmJarTest
{
    public abstract JarURLConnection getJarUrlConnection(File root, String jarName, String entryName)
        throws Exception;

    protected JarFile testJarBundle(String name)
        throws Exception
    {
        final File bundle = getResourceFile("bundle-" + name + ".jar");
        final JarURLConnection conn = getJarUrlConnection(bundle, "lib-" + name + ".jar", "");
        final JarFile jar = conn.getJarFile();
        final BufferedReader entry
            = new BufferedReader(new InputStreamReader(
                                     jar.getInputStream(new JarEntry("entry-" + name + ".txt"))));
        assertEquals("Contents mismatch for " + name, name, entry.readLine());
        return jar;
    }

    @Test
    public void testAllBundles()
        throws Exception
    {
        for (String m : new String[]{"m", "M"}) {
            for (String s : new String[]{"s", "S"}) {
                for (String l : new String[]{"l", "L"}) {
                    for (String c : new String[]{"c", "C"}) {
                        if (this instanceof PreloadedEmbeddedJarTest
                            && s.equals("S") && l.equals("L"))
                        {
                            // The combination of "streamed" and "zip64" breaks JarInputStream
                            continue;
                        }

                        final String name = m + s + l + c;
                        testJarBundle(name);
                    }
                }
            }
        }
    }
}

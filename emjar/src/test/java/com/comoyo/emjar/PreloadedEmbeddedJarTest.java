package com.comoyo.emjar;

import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PreloadedEmbeddedJarTest
    extends JarTest
{
    public JarURLConnection getJarUrlConnection(File root, String jarName, String entryName)
        throws Exception
    {
        return new PreloadedEmbeddedJar.Connection(
            new URL("jar:file:" + root.getPath() + "!/" + jarName + "!/"),
            root.getPath(),
            jarName,
            entryName);
    }

    @Test
    public void testLargeBundle()
        throws Exception
    {
        for (String s : new String[]{"s", "S"}) {
            final JarFile jar = testJarBundle(s + "-large");
            final InputStream is = jar.getInputStream(new JarEntry("oversize"));
            assertNull("oversize entry was not filtered out from " + s + " results", is);
        }
    }
}

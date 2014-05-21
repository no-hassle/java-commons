package com.comoyo.emjar;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ZipScannerTest
    extends EmJarTest
{
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

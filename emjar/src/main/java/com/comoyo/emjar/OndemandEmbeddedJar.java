package com.comoyo.emjar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Representation of nested jar that uses on-demand loading of inner
 * jar contents.  The inner jar must be stored uncompressed inside the
 * outer bundle; the individual inner jar entries may use any
 * compression method.
 *
 */
public class OndemandEmbeddedJar
{
    public static class Descriptor
    {
        private final ByteBuffer map;
        private final String name;
        private final int offset;
        private final int size;

        public Descriptor(String name, ByteBuffer map, int offset, int size)
        {
            this.name = name;
            this.map = map;
            this.offset = offset;
            this.size = size;
        }

        public String getName()
        {
            return name;
        }

        public ByteBuffer getMap()
        {
            final ByteBuffer constrained = map.duplicate();
            constrained.position(offset);
            return constrained.slice();
        }

        public int getSize()
        {
            return size;
        }

        public String toString()
        {
            return "{name:" + name
                + ", map:" + map
                + ", offset:" + offset
                + ", size:" + size + "}";
        }
    }

    public static class Connection
        extends JarURLConnection
    {
        private final String root;
        private final Map<String, Descriptor> descriptors;
        private final String entry;
        private JarFile jarFile = null;

        public Connection(
            final URL url,
            final String root,
            final Map<String, Descriptor> descriptors,
            final String entry)
            throws MalformedURLException
        {
            super(url);
            this.root = root;
            this.descriptors = descriptors;
            this.entry = entry;
        }

        @Override
        public void connect()
        {
        }

        @Override
        public synchronized JarFile getJarFile()
            throws IOException
        {
            if (jarFile == null) {
                jarFile = new FileEntry(root, descriptors);
            }
            return jarFile;
        }

        @Override
        public InputStream getInputStream()
            throws IOException
        {
            final JarFile jarFile = getJarFile();
            return jarFile.getInputStream(new ZipEntry(entry));
        }
    }

    private static class FileEntry
        extends JarFile
    {
        private Manifest manifest = null;
        private final Map<String, JarEntry> entries;
        private final Map<String, Descriptor> descriptors;
        private final Map<String, byte[]> contents;

        public FileEntry(String root, Map<String, Descriptor> descriptors)
            throws IOException
        {
            super(root);
            this.descriptors = descriptors;
            entries = new HashMap<>(descriptors.size());
            contents = new HashMap<>(descriptors.size());

            for (Map.Entry<String, Descriptor> entry : descriptors.entrySet()) {
                entries.put(entry.getKey(), new JarEntry(entry.getValue().getName()));
            }
        }

        @Override
        public Enumeration<JarEntry> entries()
        {
            return new IteratorBackedEnumeration<JarEntry>(entries.values().iterator());
        }

        @Override
        public ZipEntry getEntry(String name)
        {
            return getJarEntry(name);
        }

        @Override
        public InputStream getInputStream(ZipEntry ze)
            throws IOException
        {
            final String name = ze.getName();
            byte[] cont = contents.get(name);
            if (cont == null) {
                synchronized (contents) {
                    cont = contents.get(name);
                    if (cont == null) {
                        final Descriptor desc = descriptors.get(name);
                        if (desc == null) {
                            throw new IOException("Entry does not exist");
                        }
                        final ByteBuffer map = desc.getMap();
                        final InputStream raw = new ByteBufferBackedInputStream(map);
                        final ZipInputStream unzipped = new ZipInputStream(raw);

                        unzipped.getNextEntry();
                        final int len = desc.getSize();
                        int read = 0;
                        cont = new byte[len];
                        while (read < len) {
                            read += unzipped.read(cont, read, len - read);
                        }
                        unzipped.close();
                        contents.put(name, cont);
                    }
                }
            }
            return new ByteArrayInputStream(cont);
        }

        @Override
        public JarEntry	getJarEntry(String name)
        {
            return entries.get(name);
        }

        public Manifest getManifest() {
            if (manifest == null) {
                try {
                    final InputStream is
                        = getInputStream(new ZipEntry("META-INF/MANIFEST.MF"));
                    manifest = new Manifest(is);
                }
                catch (IOException e) {
                    manifest = new Manifest();
                }
            }
            return manifest;
        }

        public int size()
        {
            return entries.size();
        }
    }
}

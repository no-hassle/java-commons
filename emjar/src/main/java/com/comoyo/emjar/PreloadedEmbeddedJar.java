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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Representation of nested jar that preloads all inner jar entries on
 * creation.
 *
 */
public class PreloadedEmbeddedJar
{
    public static class Connection
        extends JarURLConnection
    {
        private final String root;
        private final String nested;
        private final String entry;
        private JarFile jarFile = null;

        public Connection(
            final URL url,
            final String root,
            final String nested,
            final String entry)
            throws MalformedURLException
        {
            super(url);
            this.root = root;
            this.nested = nested;
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
                jarFile = new FileEntry(root, nested);
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
        private static final int CHUNK_SIZE = 1024 * 16;
        private static final int MAX_FILE_SIZE = 1024 * 1024 * 128;

        private final Manifest manifest;
        private final Map<String, JarEntry> entries;
        private final Map<String, byte[]> contents;

        public FileEntry(String root, String nested)
            throws IOException
        {
            super(root);
            final ZipEntry embedded = super.getEntry(nested);
            final InputStream stream = super.getInputStream(embedded);
            final JarInputStream jar = new JarInputStream(stream);

            entries = new HashMap<>();
            contents = new HashMap<>();
            manifest = jar.getManifest();

            byte[] buf = null;

        process_entries:
            while (true) {
                final JarEntry entry = jar.getNextJarEntry();
                if (entry == null) {
                    break;
                }
                final String name = entry.getName();
                final int len = (int) entry.getSize();
                if (len >= 0) {
                    // Size known in advance, we can allocate contents
                    // buffer directly.
                    if (len > MAX_FILE_SIZE) {
                        jar.closeEntry();
                        continue;
                    }
                    final byte[] cont = new byte[len];
                    int read = 0;
                    while (read < len) {
                        read += jar.read(cont, read, len - read);
                    }
                    contents.put(name, cont);
                }
                else {
                    int size = 0;
                    if (buf == null) {
                        buf = new byte[CHUNK_SIZE * 2];
                    }
                    while (true) {
                        if (size + CHUNK_SIZE > buf.length) {
                            buf = Arrays.copyOf(buf, buf.length * 2);
                        }
                        final int read = jar.read(buf, size, CHUNK_SIZE);
                        if (read <= 0) {
                            break;
                        }
                        size += read;
                        if (size > MAX_FILE_SIZE) {
                            jar.closeEntry();
                            continue process_entries;
                        }
                    }
                    contents.put(name, Arrays.copyOf(buf, size));
                }
                entries.put(name, entry);
                jar.closeEntry();
            }
            jar.close();
        }

        @Override
        public Enumeration<JarEntry> entries()
        {
            return new IteratorBackedEnumeration<>(entries.values().iterator());
        }

        @Override
        public ZipEntry getEntry(String name) {
            return getJarEntry(name);
        }

        @Override
        public InputStream getInputStream(ZipEntry ze)
            throws IOException
        {
            final byte[] cont = contents.get(ze.getName());
            return cont == null ? null : new ByteArrayInputStream(cont);
        }

        @Override
        public JarEntry	getJarEntry(String name) {
            final JarEntry entry = entries.get(name);
            return entry != null ? entry : entries.get(name + "/");
        }

        @Override
        public Manifest getManifest() {
            return manifest;
        }

        @Override
        public int size()
        {
            return entries.size();
        }
    }
}

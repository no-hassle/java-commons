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

import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

/**
 * Class loader able to use embedded jars as part of the classpath.
 * All jars specified in the original classpath are opened and
 * inspected, any jar files found within will be added to the
 * classpath.  If the embedded jar files are stored using the ZIP
 * archiving method <em>stored</em> (i.e no compression), they will be
 * mapped directly and individual classes/elements can be loaded
 * on-demand.  For compressed embedded jars, initial access will
 * preload the contents of all contained elements.
 *
 * <p/>
 * The EmJar class loader can be invoked by setting the system
 * property <strong><code>java.system.class.loader</code></strong> to
 * the value
 * <strong><code>com.comoyo.emjar.EmJarClassLoader</code></strong>,
 * e.g by using the <strong><code>-D</code></strong> flag to the
 * <strong><code>java</code></strong> executable.  (The emjar classes
 * must for obvious reasons be stored directly inside the bundle jar;
 * i.e not within an embedded jar.)
 *
 * <p/>
 * For a less manual approach that embeds all configuration in the
 * bundled jar, see {@link Boot}.
 *
 * <p/>
 * (To ensure that embedded jars are stored as-is and not compressed,
 * use e.g <strong><code>maven-assembly-plugin</code></strong> version
 * 2.4 or higher, and keep the
 * <strong><code>recompressZippedFiles</code></strong> configuration
 * option set to false (the default as of version 2.4)).
 *
 */
public class EmJarClassLoader
    extends URLClassLoader
{
    public final static String SEPARATOR = "!/";

    private final static Logger logger = Logger.getLogger(EmJarClassLoader.class.getName());
    private final static HandlerFactory factory = new HandlerFactory();
    private final static Handler handler = new Handler();

    static {
        try {
            ClassLoader.registerAsParallelCapable();
        }
        catch (Throwable ignored) {
        }
    }

    public EmJarClassLoader()
    {
        super(getClassPath(System.getProperties()), null, factory);
    }

    public EmJarClassLoader(ClassLoader parent)
    {
        super(getClassPath(System.getProperties()), parent, factory);
    }

    protected EmJarClassLoader(Properties props)
    {
        super(getClassPath(props), null, factory);
    }

    private static URL[] getClassPath(final Properties props)
    {
        final String classPath = props.getProperty("java.class.path");
        final String pathSep = props.getProperty("path.separator");
        final String fileSep = props.getProperty("file.separator");
        final String userDir = props.getProperty("user.dir");

        final ArrayList<URL> urls = new ArrayList<>();
        for (String elem : classPath.split(pathSep)) {
            if (!elem.endsWith(".jar")) {
                continue;
            }
            final String full = elem.startsWith(fileSep) ? elem : userDir + fileSep + elem;
            try {
                urls.add(new URI("file", full, null).toURL());
                final JarFile jar = new JarFile(elem);
                final Enumeration<JarEntry> embedded = jar.entries();
                while (embedded.hasMoreElements()) {
                    final JarEntry entry = embedded.nextElement();
                    if (entry.getName().endsWith(".jar")) {
                        final URL url = new URI("jar:file", full + SEPARATOR + entry.getName(), null).toURL();
                        urls.add(new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile(), handler));
                    }
                }
                jar.close();
            }
            catch (IOException|URISyntaxException e) {
                logger.log(Level.SEVERE, "Unable to process classpath entry " + elem, e);
                // Trying to get by on the classpath entries we can process.
            }
        }
        return urls.toArray(new URL[0]);
    }

    @Override
    public Class<?> loadClass(String name)
        throws ClassNotFoundException
    {
        return super.loadClass(name);
    }

    private static class HandlerFactory
        implements URLStreamHandlerFactory
    {
        @Override
        public URLStreamHandler createURLStreamHandler(String protocol)
        {
            return "jar".equals(protocol) ? handler : null;
        }
    }

    private static class Handler
        extends URLStreamHandler
    {
        private final Map<String, JarURLConnection> connections
            = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Map<String, OndemandEmbeddedJar.Descriptor>>> rootJars
            = new ConcurrentHashMap<>();

        @Override
        protected URLConnection openConnection(URL url)
            throws IOException
        {
            final URI bundle;
            final String path;
            try {
                final URI nested = url.toURI();
                if (!"jar".equals(nested.getScheme())) {
                    throw new IOException(
                        "Unexpected nested scheme passed to openConnection (expeced jar): "
                            + nested.getScheme());
                }
                bundle = new URI(nested.getRawSchemeSpecificPart());
                if ("jar".equals(bundle.getScheme())) {
                    final URI file = new URI(bundle.getRawSchemeSpecificPart());
                    if (!"file".equals(file.getScheme())) {
                        throw new IOException(
                            "Unexpected location scheme passed to openConnection (expected file): "
                                + file.getScheme());
                    }
                    path = file.getSchemeSpecificPart();
                }
                else {
                    if ("file".equals(bundle.getScheme())) {
                        path = bundle.getSchemeSpecificPart() + SEPARATOR;
                    }
                    else {
                        throw new IOException(
                            "Unexpected bundle scheme passed to openConnection (expected jar or file): "
                                + bundle.getScheme());
                    }
                }
            }
            catch (URISyntaxException e) {
                throw new IOException(e);
            }
            JarURLConnection conn = connections.get(path);
            if (conn == null) {
                synchronized (connections) {
                    conn = connections.get(path);
                    if (conn == null) {
                        final int i = path.indexOf(SEPARATOR);
                        final int j = path.indexOf(SEPARATOR, i + 1);
                        if (i < 0 || j < 0) {
                            throw new IOException("Unable to parse " + path);
                        }
                        final String root = path.substring(0, i);
                        final String nested = path.substring(i + SEPARATOR.length(), j);
                        final String entry = path.substring(j + SEPARATOR.length());

                        Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> rootJar
                            = rootJars.get(root);
                        if (rootJar == null) {
                            synchronized (rootJars) {
                                rootJar = rootJars.get(root);
                                if (rootJar == null) {
                                    final ZipScanner scanner
                                        = new ZipScanner(new File(root));
                                    rootJar = scanner.scan();
                                    rootJars.put(root, rootJar);
                                }
                            }
                        }
                        final Map<String, OndemandEmbeddedJar.Descriptor> descriptors
                            = rootJar.get(nested);
                        if (descriptors != null) {
                            conn = new OndemandEmbeddedJar.Connection(
                                bundle.toURL(), root, descriptors, entry);
                        }
                        else {
                            conn = new PreloadedEmbeddedJar.Connection(
                                bundle.toURL(), root, nested, entry);
                        }
                        connections.put(path, conn);
                    }
                }
            }
            return conn;
        }
    }
}

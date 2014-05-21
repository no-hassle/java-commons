package com.comoyo.emjar;

import java.net.JarURLConnection;
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
    private final static HandlerFactory factory = new HandlerFactory();

    static {
        try {
            ClassLoader.registerAsParallelCapable();
        }
        catch (Throwable ignored) {
        }
    }

    public EmJarClassLoader()
    {
        super(getClassPath(), null, factory);
    }

    public EmJarClassLoader(ClassLoader parent)
    {
        super(getClassPath(), parent, factory);
    }

    private static URL[] getClassPath()
    {
        final Properties props = System.getProperties();
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
                urls.add(new URL("file:" + full));
                final JarFile jar = new JarFile(elem);
                Enumeration<JarEntry> embedded = jar.entries();
                while (embedded.hasMoreElements()) {
                    final JarEntry entry = embedded.nextElement();
                    if (entry.getName().endsWith(".jar")) {
                        urls.add(new URL("jar:file:" + full + "!/" + entry.getName()));
                    }
                }
                jar.close();
            }
            catch (IOException e) {
                e.printStackTrace();
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
        private final Handler handler = new Handler();

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
            final String spec = url.getFile();
            if (!spec.startsWith("jar:file:")) {
                throw new IOException("Unable to handle " + spec);
            }
            JarURLConnection conn = connections.get(spec);
            if (conn == null) {
                synchronized (connections) {
                    conn = connections.get(spec);
                    if (conn == null) {
                        final int i = spec.indexOf("!/");
                        final int j = spec.indexOf("!/", i + 1);
                        if (i < 0 || j < 0) {
                            throw new IOException("Unable to parse " + spec);
                        }
                        final String root = spec.substring(9, i);
                        final String nested = spec.substring(i + 2, j);
                        final String entry = spec.substring(j + 2);

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
                                new URL(spec), root, descriptors, entry);
                        }
                        else {
                            conn = new PreloadedEmbeddedJar.Connection(
                                new URL(spec), root, nested, entry);
                        }
                        connections.put(spec, conn);
                    }
                }
            }
            return conn;
        }
    }
}

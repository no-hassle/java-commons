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

package com.comoyo.maven.plugins.emjar;

import com.comoyo.emjar.Boot;
import com.google.common.io.ByteStreams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import static java.util.jar.Attributes.Name;

/**
 * Maven Plugin Mojo for building dependencies-included bundle jars
 * that embed nested application and library jars.
 *
 * @goal run
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class EmJarMojo
    extends AbstractMojo
{
    /**
     * Plugin descriptor.
     *
     * @component role="org.apache.maven.plugin.descriptor.PluginDescriptor"
     * @required
     * @readonly
     */
    private PluginDescriptor pluginDescriptor;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component role="org.apache.maven.repository.RepositorySystem"
     * @required
     * @readonly
     */
    protected RepositorySystem repositorySystem;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter property="project.remoteArtifactRepositories"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * @parameter default-value="${project.artifacts}"
     * @required
     * @readonly
     */
    private Collection<Artifact> artifacts;


    /**
     * Jar containing main application code.
     *
     * @parameter
     *    property="mainJar"
     *    default-value="${project.build.directory}/${project.build.finalName}.jar"
     * @required
     */
    private File mainJar;

    /**
     * Output directory for generated jar file.
     *
     * @parameter
     *     property="outputDirectory"
     *     default-value="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * Final name for generated jar file.
     *
     * @parameter
     *     property="finalName"
     *     default-value="${project.artifactId}-${project.version}"
     */
    private String finalName;

    /**
     * Suffix for generated Java Archive file ("NONE" for none).
     *
     * @parameter
     *     property="bundleSuffix"
     *     default-value="-emjar"
     */
    private String bundleSuffix;

    /**
     * Set of explicit orderings for dependency artifacts that contain
     * conflicting entries.
     *
     * @parameter
     *     property="explicitOrderings"
     */
    private Ordering[] explicitOrderings;

    /**
     * Ignore JAR content conflicts.
     *
     * @parameter
     *     property="ignoreConflicts"
     *     default-value="false"
     */
    private boolean ignoreConflicts;

    /**
     * Consider (unresolved) JAR content conflicts fatal to the build process.
     *
     * @parameter
     *     property="conflictsFatal"
     *     default-value="false"
     */
    private boolean conflictsFatal;

    /**
     * Additional Manifest entries to include in top-level JAR.
     *
     * @parameter
     *     property="manifestEntries"
     */
    private Map<String, String> manifestEntries;


    private static final String CREATED_BY = "Created-By";
    private static final int CHUNK_SIZE = 16 * 1024;
    private static final String NONE = "NONE";

    private final Map<Artifact, Map<Artifact, Set<String>>> conflicts = new HashMap<>();
    private final Map<String, Set<Artifact>> seen = new HashMap<>();

    private static String desc(
        final Artifact artifact)
    {
        return artifact.getGroupId() + ":"
            + artifact.getArtifactId() + ":"
            + artifact.getVersion();
    }

    private static boolean matches(
        final String id,
        final Artifact artifact)
    {
        final String[] parts = id.split(":");
        return parts.length == 2
            && artifact.getGroupId().equals(parts[0])
            && artifact.getArtifactId().equals(parts[1]);
    }

    /**
     * Record that artifacts {@code one} and {@code two} both contain
     * the entry {@code name}.
     */
    protected void addConflict(
        final Artifact one,
        final Artifact two,
        final String name)
    {
        Map<Artifact, Set<String>> oneConflicts = conflicts.get(one);
        if (oneConflicts == null) {
            oneConflicts = new HashMap<>();
            conflicts.put(one, oneConflicts);
        }
        Set<String> files = oneConflicts.get(two);
        if (files == null) {
            files = new HashSet<>();
            oneConflicts.put(two, files);
        }
        files.add(name);
    }

    /**
     * Record that all artifacts in {@code list} contain the entry
     * {@code name}.
     */
    protected void addConflicts(
        final Artifact[] list,
        final String name)
    {
        for (int i = 0; i < list.length; i++) {
            for (int j = i+1; j < list.length; j++) {
                addConflict(list[i], list[j], name);
                addConflict(list[j], list[i], name);
            }
        }
    }

    /**
     * Return any entries common to artifacts {@code one} and {@code two}.
     */
    protected Set<String> getConflicts(
        final Artifact one,
        final Artifact two)
    {
        Map<Artifact, Set<String>> oneConflicts = conflicts.get(one);
        if (oneConflicts == null) {
            return null;
        }
        return oneConflicts.get(two);
    }

    /**
     * Warn user that artifacts {@code one} and {@code two} both
     * contain the entries {@code files}.
     */
    protected void reportConflict(
        final Artifact one,
        final Artifact two,
        final Set<String> files)
    {
        getLog().warn(
            desc(one) + " and " + desc(two) + " contain " + files.size()
                + " common entries, no explicit resolution was given");
        int shown = 0;
        for (String file : files) {
            if (shown >= 10) {
                getLog().warn("    ...");
                break;
            }
            getLog().warn("    " + file);
            shown++;
        }
    }

    /**
     * Get artifact containing the EmJar class loader itself.
     */
    private Artifact getEmJarArtifact()
        throws MojoExecutionException
    {
        final Artifact artifact
            = repositorySystem.createArtifact(
                pluginDescriptor.getGroupId(),
                "emjar",
                pluginDescriptor.getVersion(),
                "jar");

        getLog().info("Using emjar " + artifact);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(artifact)
            .setRemoteRepositories(remoteRepositories);
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        if (!result.isSuccess()) {
            throw new MojoExecutionException(
                "Unable to resolve dependency on EmJar loader artifact, sorry: "
                    + result.toString());
        }

        Set<Artifact> artifacts = result.getArtifacts();
        if (artifacts.size() != 1) {
            throw new MojoExecutionException(
                "Unexpected number of artifacts returned when resolving EmJar loader (" + artifacts.size() + ")");
        }
        return artifacts.iterator().next();
    }

    /**
     * Add the (jar) file {@code inner} to the {@code jar} archive,
     * under the directory {@code dirPrefix}.
     */
    private void addJarToJarStream(
        final JarOutputStream jar,
        final File inner,
        final String dirPrefix)
        throws IOException
    {
        final FileInputStream is = new FileInputStream(inner);
        final FileChannel ch = is.getChannel();
        final CRC32 crc = new CRC32();
        final byte[] buf = new byte[CHUNK_SIZE];
        while (true) {
            final int read = is.read(buf, 0, CHUNK_SIZE);
            if (read <= 0) {
                break;
            }
            crc.update(buf, 0, read);
        }

        final ZipEntry entry = new ZipEntry(dirPrefix + "/" + inner.getName());
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(ch.size());
        entry.setCompressedSize(ch.size());
        entry.setCrc(crc.getValue());

        ch.position(0);
        jar.putNextEntry(entry);
        ByteStreams.copy(is, jar);
        jar.closeEntry();
    }

    /**
     * Return version of {@code artifacts} ordered according to the
     * constraints given in {@code orderings}.  Report any conflicts
     * that are not unambiguously resolved by given ordering
     * constraints.
     */
    protected List<Artifact> orderArtifacts(
        final Collection<Artifact> artifacts,
        final Ordering[] orderings)
        throws MojoExecutionException
    {
        final LinkedList<Artifact> ordered = new LinkedList<>();
        final LinkedList<Artifact> toOrder = new LinkedList<>(artifacts);
        final LinkedList<Ordering> toApply = new LinkedList<>(Arrays.asList(orderings));
        boolean conflictsSeen = false;

        while (!toOrder.isEmpty()) {
            final Iterator<Artifact> remaining = toOrder.descendingIterator();
            boolean progress = false;
        artifact_insert:
            while (remaining.hasNext()) {
                final Artifact toPlace = remaining.next();
                int placeAt = 0;
                for (Ordering order : toApply) {
                    if (!matches(order.getOver(), toPlace)) {
                        continue;
                    }
                    int placeAfter = -1;
                    final ListIterator<Artifact> placed = ordered.listIterator();
                    while (placed.hasNext()) {
                        final Artifact candidate = placed.next();
                        if (matches(order.getPrefer(), candidate)) {
                            placeAfter = placed.previousIndex();
                            break;
                        }
                    }
                    if (placeAfter < 0) {
                        // preferred artifact not inserted yet
                        continue artifact_insert;
                    }
                    placeAt = Math.max(placeAfter + 1, placeAt);
                }
                ordered.add(placeAt, toPlace);
                final Iterator<Ordering> constraint = toApply.iterator();
                while (constraint.hasNext()) {
                    if (matches(constraint.next().getOver(), toPlace)) {
                        constraint.remove();
                    }
                }
                remaining.remove();
                progress = true;

                // Artifacts are placed as early as possible after constrains
                // have been applied; i.e there are no constraints regulating
                // the relationship between placed artifacts and others
                // occurring later in the list. Report all conflicts in this
                // space.
                if (!ignoreConflicts) {
                    final ListIterator<Artifact> allAfter
                        = ordered.listIterator(placeAt + 1);
                    while (allAfter.hasNext()) {
                        final Artifact after = allAfter.next();
                        Set<String> files = getConflicts(toPlace, after);
                        if (files != null && !files.isEmpty()) {
                            reportConflict(toPlace, after, files);
                            conflictsSeen = true;
                        }
                    }
                }
            }
            if (!progress) {
                String remains = "";
                for (Artifact artifact : toOrder) {
                    remains = remains + " " + desc(artifact);
                }
                throw new MojoExecutionException(
                    "Unable to order remaining artifacts:" + remains
                        + ", conflicting ordering directives?");
            }
        }
        if (!toApply.isEmpty()) {
            getLog().warn("Unused ordering directives:");
            for (Ordering ordering : toApply) {
                getLog().warn("    prefer " + ordering.getPrefer()
                              + " over " + ordering.getOver());
            }
        }
        if (conflictsSeen && conflictsFatal) {
            throw new MojoExecutionException(
                "Aborting due to jar content conflicts");
        }
        return ordered;
    }

    /**
     * Scan an artifact (jar) file for entries; record all entries
     * that conflict with the contents of already scanned artifacts.
     */
    private void scanArtifact(
        final Artifact artifact)
        throws IOException
    {
        final String type = artifact.getType();
        if (!"jar".equals(type)) {
            return;
        }
        final File file = artifact.getFile();
        final JarFile jar = new JarFile(file);
        final Enumeration<? extends JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String name = entry.getName();
            if (entry.isDirectory()
                || name.startsWith("META-INF")
                || !name.contains("/"))
            {
                continue;
            }
            Set<Artifact> others = seen.get(name);
            if (others == null) {
                others = new HashSet<>();
                seen.put(name, others);
            }
            else {
                for (Artifact other : others) {
                    addConflict(artifact, other, name);
                    addConflict(other, artifact, name);
                }
            }
            others.add(artifact);
        }
        jar.close();
    }

    /**
     * Plugin invocation point.
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        try {
            final String suffix = NONE.equals(bundleSuffix) ? "" : bundleSuffix;
            final File outFile = new File(outputDirectory, finalName + suffix + ".jar");
            getLog().info("Building jar: " + outFile.getPath());
            if (ignoreConflicts && conflictsFatal) {
                throw new MojoExecutionException(
                    "Invalid configuration; both ignoreConflicts and conflictsFatal set.");
            }
            final JarFile main = new JarFile(mainJar);
            final Attributes mainAttrs = main.getManifest().getMainAttributes();
            final Manifest manifest = new Manifest();
            final Attributes bootAttrs = manifest.getMainAttributes();
            bootAttrs.put(Name.MANIFEST_VERSION, "1.0");
            bootAttrs.putValue(CREATED_BY,
                               "EmJar Maven Plugin " + pluginDescriptor.getVersion());
            bootAttrs.put(Name.MAIN_CLASS, Boot.class.getName());
            if (mainAttrs.containsKey(Name.MAIN_CLASS)) {
                bootAttrs.putValue(Boot.EMJAR_MAIN_CLASS_ATTR,
                                   mainAttrs.getValue(Name.MAIN_CLASS));
            }
            main.close();
            if (manifestEntries != null) {
                for (Map.Entry<String, String> entry : manifestEntries.entrySet()) {
                    bootAttrs.putValue(entry.getKey(), entry.getValue());
                }
            }
            final JarOutputStream jar
                = new JarOutputStream(new FileOutputStream(outFile, false), manifest);

            final Artifact emjar = getEmJarArtifact();
            final JarFile loader = new JarFile(emjar.getFile());
            final Enumeration<? extends ZipEntry> entries = loader.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                final InputStream is = loader.getInputStream(entry);
                jar.putNextEntry(entry);
                ByteStreams.copy(is, jar);
                jar.closeEntry();
            }
            loader.close();

            addJarToJarStream(jar, mainJar, "main");
            seen.clear();
            if (!ignoreConflicts) {
                for (Artifact artifact : artifacts) {
                    scanArtifact(artifact);
                }
            }
            final List<Artifact> ordered = orderArtifacts(artifacts, explicitOrderings);
            for (Artifact artifact : ordered) {
                addJarToJarStream(jar, artifact.getFile(), "lib");
            }
            jar.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException("Unable to generate EmJar archive", e);
        }
    }
}

package com.comoyo.maven.plugins.protoc;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;


/**
 * Maven Plugin Mojo for compiling Protobuf schema files.  Protobuf
 * compiler binaries for various platforms and protobuf versions are
 * bundled with the plugin and used as required.
 *
 * @goal run
 * @phase generate-sources
 * @requiresDependencyResolution
 */

public class ProtocBundledMojo extends AbstractMojo
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
     * The Maven project.
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    private MavenProject project;

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
    protected List remoteRepositories;


    /**
     * Protobuf version to compile schema files for.  If omitted,
     * version is inferred from the project's depended-on
     * com.google.com:protobuf-java artifact, if any.  (If both are
     * present, the version must match.)
     *
     * @parameter property="protobufVersion"
     */
    private String protobufVersion;

    /**
     * Directories containing *.proto files to compile.
     *
     * @parameter
     *     property="inputDirectories"
     *     default-value="src/main/protobuf"
     */
    private File[] inputDirectories;

    /**
     * Output directory for generated Java class files.
     *
     * @parameter
     *     property="outputDirectory"
     *     default-value="${project.build.directory}/generated-sources/protobuf"
     */
    private File outputDirectory;

    /**
     * Path to existing protoc to use.  Overrides auto-detection and
     * use of bundled protoc.
     *
     * @parameter property="protocExec"
     */
    private File protocExec;


    private static final Map<String, String> osNamePrefix = new HashMap<String, String>();
    private static final Map<String, String> osArchCanon = new HashMap<String, String>();
    static {
        osNamePrefix.put("linux", "linux");
        osNamePrefix.put("mac os x", "mac_os_x");
        osNamePrefix.put("windows", "win32");

        osArchCanon.put("x86_64", "x86_64");
        osArchCanon.put("amd64", "x86_64");
        osArchCanon.put("x86", "x86");
        osArchCanon.put("i386", "x86");
        osArchCanon.put("i686", "x86");
    }

    /**
     * Dynamically determine name of protoc variant suitable for
     * current system.
     *
     */
    private String determineProtocForSystem()
        throws MojoExecutionException
    {
        final String osName = System.getProperty("os.name");
        if (osName == null) {
            throw new MojoExecutionException("Unable to determine OS platform");
        }

        String os = null;
        for (String prefix : osNamePrefix.keySet()) {
            if (osName.toLowerCase().startsWith(prefix)) {
                os = osNamePrefix.get(prefix);
                break;
            }
        }
        if (os == null) {
            throw new MojoExecutionException("Unable to determine OS class for " + osName);
        }

        final String archName = System.getProperty("os.arch");
        if (archName == null) {
            throw new MojoExecutionException("Unable to determine CPU architecture");
        }

        String arch = osArchCanon.get(archName.toLowerCase());
        if (arch == null) {
            throw new MojoExecutionException("Unable to determine CPU arch id for " + archName);
        }

        // Pre-built protoc binaries for Windows use the same image
        // for 32- and 64-bit systems.
        if ("win32".equals(os) && "x86_64".equals(arch)) {
            arch = "x86";
        }

        return protobufVersion + "-" + os + "-" + arch;
    }

    /**
     * Return reference to suitable protoc binary artifact, download
     * from remote repository if necessary.
     *
     * @param protocName   protoc specifier
     */
    private File resolveProtocArtifact(String protocName)
        throws MojoExecutionException
    {
        Artifact artifact
            = repositorySystem.createArtifactWithClassifier(
                pluginDescriptor.getGroupId(),
                pluginDescriptor.getArtifactId(),
                pluginDescriptor.getVersion(),
                "exe", protocName);

        getLog().info("Using protoc " + artifact);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(artifact)
            .setRemoteRepositories(remoteRepositories);
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        if (!result.isSuccess()) {
            throw new MojoExecutionException(
                "Unable to resolve dependency on protoc binary artifact, sorry: "
                    + result.toString());
        }

        Set<Artifact> artifacts = result.getArtifacts();
        if (artifacts.size() != 1) {
            throw new MojoExecutionException(
                "Unexpected number of artifacts returned when resolving protoc binary");
        }
        Artifact protocArtifact = artifacts.iterator().next();
        File file = protocArtifact.getFile();
        file.setExecutable(true, false);
        return file;
    }

    /**
     * Get version of specified artifact from the current project's
     * dependencies, if it exists.
     *
     * @param groupId
     * @param artifactId
     */
    private String getArtifactVersion(String groupId, String artifactId)
        throws MojoExecutionException
    {
        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (groupId.equals(artifact.getGroupId())
                && artifactId.equals(artifact.getArtifactId()))
            {
                return artifact.getVersion();
            }
        }
        return null;
    }

    /**
     * Ensure we have a suitable protoc binary available.  If
     * protocExec is explicitly given, use that.  Otherwise find and
     * extract suitable protoc from plugin bundle.
     *
     */
    private void ensureProtocBinaryPresent()
        throws MojoExecutionException
    {
        if (protocExec != null) {
            return;
        }

        final String protobufArtifactVersion
            = getArtifactVersion("com.google.protobuf", "protobuf-java");

        if (protobufVersion == null) {
            if (protobufArtifactVersion == null) {
                throw new MojoExecutionException(
                    "protobufVersion not specified and unable to derive version "
                        + "from protobuf-java dependency");
            }
            protobufVersion = protobufArtifactVersion;
        }
        else {
            if (protobufArtifactVersion != null
                && !protobufVersion.equals(protobufArtifactVersion))
            {
                throw new MojoExecutionException(
                    "Project includes protobuf-java artifact of version "
                        + protobufArtifactVersion
                        + " while protoc is set to compile for version "
                        + protobufVersion);
            }
        }

        final String protocName = determineProtocForSystem();
        protocExec = resolveProtocArtifact(protocName);
    }

    /**
     * Compile single protobuf schema file.
     *
     * @param dir   base dir for input file, used to resolve includes
     * @param input   input file to compile
     */
    private void compileFile(File dir, File input)
        throws MojoExecutionException
    {
        try {
            final Process proc
                = new ProcessBuilder(protocExec.toString(),
                                     "--proto_path=" + dir.getAbsolutePath(),
                                     "--java_out=" + outputDirectory,
                                     input.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
            final BufferedReader procOut
                = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while (true) {
                final String line = procOut.readLine();
                if (line == null) {
                    break;
                }
                getLog().info("[protoc] " + line);
            }
            final int status = proc.waitFor();
            procOut.close();
            if (status != 0) {
                throw new MojoExecutionException(
                    "Compilation failure signalled by protoc exit status: " + status);
            }
        }
        catch (Exception e) {
            throw new MojoExecutionException("Unable to compile " + input.toString(), e);
        }
    }

    /**
     * Compile all *.proto files found under inputDirectories.
     *
     */
    private void compileAllFiles()
        throws MojoExecutionException
    {
        final IOFileFilter filter = new SuffixFileFilter(".proto");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        for (File dir : inputDirectories) {
            Iterator<File> files
                = FileUtils.iterateFiles(dir, filter, TrueFileFilter.INSTANCE);
            while (files.hasNext()) {
                final File input = files.next();
                compileFile(dir, input);
            }
        }
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    }

    /**
     * Plugin invocation point.
     *
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        ensureProtocBinaryPresent();
        getLog().info("Input directories:");
        for (File input : inputDirectories){
            getLog().info("  " + input);
        }
        getLog().info("Compiling to " + outputDirectory);
        compileAllFiles();
    }
}

protoc-bundled-plugin
=====================

Maven Plugin Mojo for compiling Protobuf schema files. Protobuf compiler binaries for various platforms and protobuf versions are bundled with the plugin and used as required.

### Available parameters

* protobufVersion

    Protobuf version to compile schema files for. If omitted, version is inferred from the project's depended-on `com.google.com:protobuf-java` artifact, if any.  The inferred version must match the explicitly given version if both are present.

* inputDirectories

    Directories containing *.proto files to compile.  Defaults to `${project.basedir}/src/main/protobuf`.

* outputDirectory

    Output directory for generated Java class files.  Defaults to `${project.build.directory}/generated-sources/protobuf`.

* testInputDirectories

    Directories containing test *.proto files to compile.  Defaults to `${project.basedir}/src/test/protobuf`.

* testOutputDirectory

    Output directory for generated Java class test files.  Defaults to `${project.build.directory}/generated-test-sources/protobuf`.

* protocExec

    Path to existing protoc to use. Overrides auto-detection and use of bundled protoc.

### Minimal usage example

```xml
<plugins>
  ...
  <plugin>
    <groupId>no.hassle.maven.plugins</groupId>
    <artifactId>protoc-bundled-plugin</artifactId>
    <version>2.0.0</version>
    <executions>
      <execution>
        <goals>
          <goal>run</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
  ...
</plugins>
```

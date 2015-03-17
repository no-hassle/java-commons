emjar-maven-plugin
==================

Maven Plugin Mojo for building bundling jars that contain dependency artifact jars verbatim.  The bundling jars are built to include the [EmJar](../emjar) class loader for allowing use of the embedded jars as parts of the classpath for the main application code.

### Available parameters

* finalName

    Final name root for generated bundle jar file.  Defaults to `${project.artifactId}-${project.version}`.

* bundleSuffix

    Suffix appended to `finalName` when building output bundle name.
    Defaults to `-emjar`.  (Due to the way pom files are parsed and
    passed to plugins, an empty suffix cannot be directly configured.
    Use the value `NONE` to indicate that no suffix should be
    appended.)

* explicitOrderings

    Set of explicit orderings for dependency artifacts that contain conflicting entries.

*  mainJar

    Jar file containing main application code.  Defaults to `${project.build.directory}/${project.build.finalName}.jar`.

*  manifestEntries

    Additional Manifest entries to include in top-level jar.

* outputDirectory

    Output directory for generated jar file.  Defaults to `${project.build.directory}`.

* ignoreConflicts

    Ingore jar content conflicts.  Defaults to `false`.

* conflictsFatal

    Consider (unresolved) jar content conflicts fatal to the build process.  Defaults to `false`.


### Minimal usage example

```xml
<plugins>
  ...
  <plugin>
    <groupId>com.comoyo.commons</groupId>
    <artifactId>emjar-maven-plugin</artifactId>
    <version>1.4.46</version>
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

### With explicit ordering constraints

```xml
<plugins>
  ...
  <plugin>
    <groupId>com.comoyo.commons</groupId>
    <artifactId>emjar-maven-plugin</artifactId>
    <version>1.4.46</version>
    <executions>
      <execution>
        <goals>
          <goal>run</goal>
        </goals>
        <configuration>
          <finalName>${project.artifactId}-${project.version}-${git.commit.id.abbrev}</finalName>
          <bundleSuffix>-jar-with-dependencies</bundleSuffix>
          <conflictsFatal>true</conflictsFatal>
          <explicitOrderings>
            <explicitOrdering>
              <prefer>org.slf4j:log4j-over-slf4j</prefer>
              <over>log4j:log4j</over>
            </explicitOrdering>
          </explicitOrderings>
        </configuration>
      </execution>
    </executions>
  </plugin>
  ...
</plugins>
```

emjar-maven-plugin
==================

Maven Plugin Mojo for building bundling jars that contain dependency artifact jars verbatim.  The bundling jars are built to include the [EmJar](../emjar) class loader for allowing use of the embedded jars as parts of the classpath for the main application code.

### Available parameters

* finalName

    Final name for generated Java Archive file.

* explicitOrderings

    Set of explicit orderings for dependency artifacts that contain conflicting entries.

*  mainJar

    Jar file containing main application code.

*  manifestEntries

    Additional Manifest entries to include in top-level jar.

* outputDirectory

    Output directory for generated jar file.

* ignoreConflicts

    Ingore jar content conflicts.


### Minimal usage example

```xml
<plugins>
  ...
  <plugin>
    <groupId>com.comoyo.commons</groupId>
    <artifactId>emjar-maven-plugin</artifactId>
    <version>1.0.0</version>
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
    <version>1.0.0</version>
    <executions>
      <execution>
        <goals>
          <goal>run</goal>
        </goals>
        <configuration>
          <finalName>${project.artifactId}-${project.version}-${git.commit.id.abbrev}</finalName>
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

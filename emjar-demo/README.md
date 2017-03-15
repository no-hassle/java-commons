emjar-demo
==========

The motivation for writing EmJar was dissatifaction with the existing
approaches to distributing a Java project along with its dependencies
in a single file (aka "fat jars", "über jars", "jar-in-jars" or
similar). At the time, the prevailing alternatives were Maven Shade or
One-JAR. This mini project showcases some of the differences between
the different alternatives, and highlights some of our motivation for
writing our own tool for this. This biased and unscientific, and our
priorities will likely not be your priorities, so reader discretion is
advised. If nothing else, this is also an example of how to integrate
some of the other fat jar alternatives in a Maven flow (which is not
to say that they all represent best practices).

Refer to the [pom](pom.xml) and [source
code](src/main/java/com/comoyo/commons/emjar/demo/Demo.java) for some
comments about the peculiarites of the different alternatives.

Run `mvn clean verify -Dmaven.profile
-Pbare,emjar,shade,onejar,springboot,jarinjar` to exercise packaging
and test runs of all the different alternatives; a time usage summary
will be presented at the end of the run. (Be aware that some of the
packaging executions share dependencies, which will favour the
executions being run after the dependency has already been resolved.
For accurate timing results, profile one alternative at a time.)  To
run the integration tests under a non-default JRE, specify
`-Djava.executable=.../bin/java` to indicate a different Java runtime.

One of the challenges with multiple jars embedded in a single jar is
to make sure all references to per-jar META-INF information is handled
correctly -- pay attention to the jetty version string in the example
runs to see what happens when this fails.

Tool                 | Classpath scanning       | META-INF separation      | Signed embedded dependencies
---------------------|--------------------------|--------------------------|-----------------------------
Maven Shade          | :heavy_check_mark:       | :heavy_exclamation_mark: | :heavy_exclamation_mark:
One-JAR              | :heavy_exclamation_mark: | :heavy_check_mark:       | :heavy_check_mark:
Spring Boot          | :heavy_exclamation_mark: | :heavy_check_mark:       | :heavy_check_mark:
Eclipse Runnable Jar | :heavy_exclamation_mark: | :heavy_exclamation_mark: | :heavy_check_mark:
EmJar                | :heavy_check_mark:       | :heavy_check_mark:       | :heavy_check_mark:

With large dependency sets, packaging and execution time will also
tend to differ betweem the tools. The following is a rough sample of
what the timings look like for this simple demo project:

Tool                 | Package | Execute
---------------------|---------|---------
Bare (Filesystem)    | -       | 1.482s
Maven Shade          | 1.722s  | 1.477s
One-JAR              | 2.257s  | 1.985s
Spring Boot          | 0.305s  | 2.375s
Eclipse Runnable Jar | 1.888s  | 3.091s
EmJar                | 0.143s  | 1.578s

For a quick introduction in how to use EmJar, see the
[emjar-maven-plugin](../emjar-maven-plugin/) description.

The following is an example run showing executions of the jars created
by the different tools under different circumstances:

```
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-bare-1) @ emjar-demo ---
  bare [INFO] Running test (explicit registration)
  bare [INFO] BouncyCastle security provider loaded successfully
  bare [INFO] Logging initialized @695ms
  bare [INFO] jetty-9.3.15.v20161220
  bare [INFO] Started o.e.j.s.ServletContextHandler@56f521c6{/,null,AVAILABLE}
  bare [INFO] Started ServerConnector@3f9f71ff{HTTP/1.1,[http/1.1]}{0.0.0.0:45413}
  bare [INFO] Started @1175ms
  bare [INFO] Html: <html><body><h1>OK</h1></body></html>
  bare [INFO] Json: {"key":"value"}
  bare [INFO] Xml: <HashMap><key>value</key></HashMap>
  bare [INFO] Stopped ServerConnector@3f9f71ff{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  bare [INFO] Stopped o.e.j.s.ServletContextHandler@56f521c6{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-bare-2) @ emjar-demo ---
  bare [INFO] Running test (classpath scanning)
  bare [INFO] BouncyCastle security provider loaded successfully
  bare [INFO] Logging initialized @721ms
  bare [INFO] jetty-9.3.15.v20161220
  bare [INFO] Started o.e.j.s.ServletContextHandler@4422dd48{/,null,AVAILABLE}
  bare [INFO] Started ServerConnector@618ad2aa{HTTP/1.1,[http/1.1]}{0.0.0.0:33847}
  bare [INFO] Started @1220ms
  bare [INFO] Html: <html><body><h1>OK</h1></body></html>
  bare [INFO] Json: {"key":"value"}
  bare [INFO] Xml: <HashMap><key>value</key></HashMap>
  bare [INFO] Stopped ServerConnector@618ad2aa{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  bare [INFO] Stopped o.e.j.s.ServletContextHandler@4422dd48{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-emjar-1) @ emjar-demo ---
  emjar [INFO] Running test (explicit registration)
  emjar [INFO] BouncyCastle security provider loaded successfully
  emjar [INFO] Logging initialized @643ms
  emjar [INFO] jetty-9.3.15.v20161220
  emjar [INFO] Started o.e.j.s.ServletContextHandler@6c49835d{/,null,AVAILABLE}
  emjar [INFO] Started ServerConnector@71bbf57e{HTTP/1.1,[http/1.1]}{0.0.0.0:39521}
  emjar [INFO] Started @1149ms
  emjar [INFO] Html: <html><body><h1>OK</h1></body></html>
  emjar [INFO] Json: {"key":"value"}
  emjar [INFO] Xml: <HashMap><key>value</key></HashMap>
  emjar [INFO] Stopped ServerConnector@71bbf57e{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  emjar [INFO] Stopped o.e.j.s.ServletContextHandler@6c49835d{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-emjar-2) @ emjar-demo ---
  emjar [INFO] Running test (classpath scanning)
  emjar [INFO] BouncyCastle security provider loaded successfully
  emjar [INFO] Logging initialized @617ms
  emjar [INFO] jetty-9.3.15.v20161220
  emjar [INFO] Started o.e.j.s.ServletContextHandler@67205a84{/,null,AVAILABLE}
  emjar [INFO] Started ServerConnector@51cdd8a{HTTP/1.1,[http/1.1]}{0.0.0.0:32967}
  emjar [INFO] Started @1075ms
  emjar [INFO] Html: <html><body><h1>OK</h1></body></html>
  emjar [INFO] Json: {"key":"value"}
  emjar [INFO] Xml: <HashMap><key>value</key></HashMap>
  emjar [INFO] Stopped ServerConnector@51cdd8a{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  emjar [INFO] Stopped o.e.j.s.ServletContextHandler@67205a84{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-shade-1) @ emjar-demo ---
  shade [INFO] Running test (explicit registration)
java.security.NoSuchProviderException: JCE cannot authenticate the provider BC
        at javax.crypto.JceSecurity.getInstance(JceSecurity.java:100)
        at javax.crypto.KeyAgreement.getInstance(KeyAgreement.java:230)
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:36)
Caused by: java.util.jar.JarException: file:/home/argggh/telenor/src/commons/emjar-demo/target/emjar-demo-1.4-SNAPSHOT-shaded.jar has unsigned entries - jetty-logging.properties
        at javax.crypto.JarVerifier.verifySingleJar(JarVerifier.java:500)
        at javax.crypto.JarVerifier.verifyJars(JarVerifier.java:361)
        at javax.crypto.JarVerifier.verify(JarVerifier.java:289)
        at javax.crypto.JceSecurity.verifyProviderJar(JceSecurity.java:159)
        at javax.crypto.JceSecurity.getVerificationResult(JceSecurity.java:185)
        at javax.crypto.JceSecurity.getInstance(JceSecurity.java:97)
        ... 2 more
  shade [INFO] Logging initialized @574ms
  shade [INFO] jetty-9.3.z-SNAPSHOT
  shade [INFO] Started o.e.j.s.ServletContextHandler@3a48c398{/,null,AVAILABLE}
  shade [INFO] Started ServerConnector@5d22604e{HTTP/1.1,[http/1.1]}{0.0.0.0:35979}
  shade [INFO] Started @1059ms
  shade [INFO] Html: <html><body><h1>OK</h1></body></html>
  shade [INFO] Json: {"key":"value"}
  shade [INFO] Xml: <HashMap><key>value</key></HashMap>
  shade [INFO] Stopped ServerConnector@5d22604e{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  shade [INFO] Stopped o.e.j.s.ServletContextHandler@3a48c398{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-shade-2) @ emjar-demo ---
  shade [INFO] Running test (classpath scanning)
java.security.NoSuchProviderException: JCE cannot authenticate the provider BC
        at javax.crypto.JceSecurity.getInstance(JceSecurity.java:100)
        at javax.crypto.KeyAgreement.getInstance(KeyAgreement.java:230)
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:36)
Caused by: java.util.jar.JarException: file:/home/argggh/telenor/src/commons/emjar-demo/target/emjar-demo-1.4-SNAPSHOT-shaded.jar has unsigned entries - jetty-logging.properties
        at javax.crypto.JarVerifier.verifySingleJar(JarVerifier.java:500)
        at javax.crypto.JarVerifier.verifyJars(JarVerifier.java:361)
        at javax.crypto.JarVerifier.verify(JarVerifier.java:289)
        at javax.crypto.JceSecurity.verifyProviderJar(JceSecurity.java:159)
        at javax.crypto.JceSecurity.getVerificationResult(JceSecurity.java:185)
        at javax.crypto.JceSecurity.getInstance(JceSecurity.java:97)
        ... 2 more
  shade [INFO] Logging initialized @580ms
  shade [INFO] jetty-9.3.z-SNAPSHOT
  shade [INFO] Started o.e.j.s.ServletContextHandler@3f1d6a13{/,null,AVAILABLE}
  shade [INFO] Started ServerConnector@3f64d943{HTTP/1.1,[http/1.1]}{0.0.0.0:44305}
  shade [INFO] Started @1340ms
  shade [INFO] Html: <html><body><h1>OK</h1></body></html>
  shade [INFO] Json: {"key":"value"}
  shade [INFO] Xml: <HashMap><key>value</key></HashMap>
  shade [INFO] Stopped ServerConnector@3f64d943{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  shade [INFO] Stopped o.e.j.s.ServletContextHandler@3f1d6a13{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-onejar-1) @ emjar-demo ---
  onejar [INFO] Running test (explicit registration)
  onejar [INFO] BouncyCastle security provider loaded successfully
  onejar [INFO] Logging initialized @1114ms
  onejar [INFO] jetty-9.3.15.v20161220
  onejar [INFO] Started o.e.j.s.ServletContextHandler@3fcdcf{/,null,AVAILABLE}
  onejar [INFO] Started ServerConnector@1db0ec27{HTTP/1.1,[http/1.1]}{0.0.0.0:35963}
  onejar [INFO] Started @1536ms
  onejar [INFO] Html: <html><body><h1>OK</h1></body></html>
  onejar [INFO] Json: {"key":"value"}
  onejar [INFO] Xml: <HashMap><key>value</key></HashMap>
  onejar [INFO] Stopped ServerConnector@1db0ec27{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  onejar [INFO] Stopped o.e.j.s.ServletContextHandler@3fcdcf{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-onejar-2) @ emjar-demo ---
  onejar [INFO] Running test (classpath scanning)
  onejar [INFO] BouncyCastle security provider loaded successfully
  onejar [INFO] Logging initialized @1025ms
  onejar [INFO] jetty-9.3.15.v20161220
  onejar [INFO] Started o.e.j.s.ServletContextHandler@39fc6b2c{/,null,AVAILABLE}
  onejar [INFO] Started ServerConnector@126be319{HTTP/1.1,[http/1.1]}{0.0.0.0:40943}
  onejar [INFO] Started @1411ms
javax.ws.rs.NotFoundException: HTTP 404 Not Found
        at org.glassfish.jersey.client.JerseyInvocation.convertToException(JerseyInvocation.java:1020)
        at org.glassfish.jersey.client.JerseyInvocation.translate(JerseyInvocation.java:819)
        at org.glassfish.jersey.client.JerseyInvocation.access$700(JerseyInvocation.java:92)
        at org.glassfish.jersey.client.JerseyInvocation$2.call(JerseyInvocation.java:701)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:228)
        at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:444)
        at org.glassfish.jersey.client.JerseyInvocation.invoke(JerseyInvocation.java:697)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.method(JerseyInvocation.java:420)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.get(JerseyInvocation.java:316)
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:51)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at com.simontuffs.onejar.Boot.run(Boot.java:342)
        at com.simontuffs.onejar.Boot.main(Boot.java:168)
  onejar [INFO] Stopped ServerConnector@126be319{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  onejar [INFO] Stopped o.e.j.s.ServletContextHandler@39fc6b2c{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-springboot-1) @ emjar-demo ---
  springboot [INFO] Running test (explicit registration)
  springboot [INFO] BouncyCastle security provider loaded successfully
  springboot [INFO] Logging initialized @1304ms
  springboot [INFO] jetty-9.3.15.v20161220
  springboot [INFO] Started o.e.j.s.ServletContextHandler@6e6c3152{/,null,AVAILABLE}
  springboot [INFO] Started ServerConnector@6737fd8f{HTTP/1.1,[http/1.1]}{0.0.0.0:37601}
  springboot [INFO] Started @2040ms
  springboot [INFO] Html: <html><body><h1>OK</h1></body></html>
  springboot [INFO] Json: {"key":"value"}
  springboot [INFO] Xml: <HashMap><key>value</key></HashMap>
  springboot [INFO] Stopped ServerConnector@6737fd8f{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  springboot [INFO] Stopped o.e.j.s.ServletContextHandler@6e6c3152{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-springboot-2) @ emjar-demo ---
  springboot [INFO] Running test (classpath scanning)
Exception in thread "main" java.lang.reflect.InvocationTargetException
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:48)
        at org.springframework.boot.loader.Launcher.launch(Launcher.java:87)
        at org.springframework.boot.loader.Launcher.launch(Launcher.java:50)
        at org.springframework.boot.loader.JarLauncher.main(JarLauncher.java:51)
Caused by: org.glassfish.jersey.server.internal.scanning.ResourceFinderException: java.io.FileNotFoundException: /home/argggh/telenor/src/commons/emjar-demo/target/emjar-demo-1.4-SNAPSHOT-springboot.jar!/BOOT-INF/classes (No such file or directory)
        at org.glassfish.jersey.server.internal.scanning.JarZipSchemeResourceFinderFactory.create(JarZipSchemeResourceFinderFactory.java:89)
        at org.glassfish.jersey.server.internal.scanning.JarZipSchemeResourceFinderFactory.create(JarZipSchemeResourceFinderFactory.java:65)
        at org.glassfish.jersey.server.internal.scanning.PackageNamesScanner.addResourceFinder(PackageNamesScanner.java:282)
        at org.glassfish.jersey.server.internal.scanning.PackageNamesScanner.init(PackageNamesScanner.java:198)
        at org.glassfish.jersey.server.internal.scanning.PackageNamesScanner.<init>(PackageNamesScanner.java:154)
        at org.glassfish.jersey.server.internal.scanning.PackageNamesScanner.<init>(PackageNamesScanner.java:110)
        at org.glassfish.jersey.server.ResourceConfig.packages(ResourceConfig.java:680)
        at org.glassfish.jersey.server.ResourceConfig.packages(ResourceConfig.java:660)
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:27)
        ... 8 more
Caused by: java.io.FileNotFoundException: /home/argggh/telenor/src/commons/emjar-demo/target/emjar-demo-1.4-SNAPSHOT-springboot.jar!/BOOT-INF/classes (No such file or directory)
        at java.io.FileInputStream.open0(Native Method)
        at java.io.FileInputStream.open(FileInputStream.java:195)
        at java.io.FileInputStream.<init>(FileInputStream.java:138)
        at java.io.FileInputStream.<init>(FileInputStream.java:93)
        at sun.net.www.protocol.file.FileURLConnection.connect(FileURLConnection.java:90)
        at sun.net.www.protocol.file.FileURLConnection.getInputStream(FileURLConnection.java:188)
        at java.net.URL.openStream(URL.java:1045)
        at org.glassfish.jersey.server.internal.scanning.JarZipSchemeResourceFinderFactory.getInputStream(JarZipSchemeResourceFinderFactory.java:177)
        at org.glassfish.jersey.server.internal.scanning.JarZipSchemeResourceFinderFactory.create(JarZipSchemeResourceFinderFactory.java:87)
        ... 16 more
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-jarinjar-1) @ emjar-demo ---
  jarinjar [INFO] Running test (explicit registration)
  jarinjar [INFO] BouncyCastle security provider loaded successfully
  jarinjar [INFO] Logging initialized @880ms
  jarinjar [INFO] jetty-9.3.z-SNAPSHOT
  jarinjar [INFO] Started o.e.j.s.ServletContextHandler@39941489{/,null,AVAILABLE}
  jarinjar [INFO] Started ServerConnector@1e3df614{HTTP/1.1,[http/1.1]}{0.0.0.0:45865}
  jarinjar [INFO] Started @2027ms
  jarinjar [INFO] Html: <html><body><h1>OK</h1></body></html>
  jarinjar [INFO] Json: {"key":"value"}
  jarinjar [INFO] Xml: <HashMap><key>value</key></HashMap>
  jarinjar [INFO] Stopped ServerConnector@1e3df614{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  jarinjar [INFO] Stopped o.e.j.s.ServletContextHandler@39941489{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-jarinjar-2) @ emjar-demo ---
  jarinjar [INFO] Running test (classpath scanning)
  jarinjar [INFO] BouncyCastle security provider loaded successfully
  jarinjar [INFO] Logging initialized @908ms
  jarinjar [INFO] jetty-9.3.z-SNAPSHOT
  jarinjar [INFO] Started o.e.j.s.ServletContextHandler@673e76b3{/,null,AVAILABLE}
  jarinjar [INFO] Started ServerConnector@637791d{HTTP/1.1,[http/1.1]}{0.0.0.0:40075}
  jarinjar [INFO] Started @2077ms
javax.ws.rs.NotFoundException: HTTP 404 Not Found
        at org.glassfish.jersey.client.JerseyInvocation.convertToException(JerseyInvocation.java:1020)
        at org.glassfish.jersey.client.JerseyInvocation.translate(JerseyInvocation.java:819)
        at org.glassfish.jersey.client.JerseyInvocation.access$700(JerseyInvocation.java:92)
        at org.glassfish.jersey.client.JerseyInvocation$2.call(JerseyInvocation.java:701)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:228)
        at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:444)
        at org.glassfish.jersey.client.JerseyInvocation.invoke(JerseyInvocation.java:697)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.method(JerseyInvocation.java:420)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.get(JerseyInvocation.java:316)
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:51)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader.main(JarRsrcLoader.java:58)
  jarinjar [INFO] Stopped ServerConnector@637791d{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
  jarinjar [INFO] Stopped o.e.j.s.ServletContextHandler@673e76b3{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (fencepost) @ emjar-demo ---

com.comoyo.commons:emjar-demo:1.4-SNAPSHOT

  clean 89ms
    org.apache.maven.plugins:maven-clean-plugin:2.5 (default-clean) 85ms

  initialize 1s 276ms
    pl.project13.maven:git-commit-id-plugin:2.1.9 (default) 1s 276ms

  process-resources 113ms
    org.apache.maven.plugins:maven-resources-plugin:2.6 (default-resources) 111ms

  compile 1s 69ms
    org.apache.maven.plugins:maven-compiler-plugin:2.5.1 (default-compile) 1s 69ms

  process-test-resources 10ms
    org.apache.maven.plugins:maven-resources-plugin:2.6 (default-testResources) 10ms

  test-compile 6ms
    org.apache.maven.plugins:maven-compiler-plugin:2.5.1 (default-testCompile) 5ms

  test 131ms
    org.apache.maven.plugins:maven-surefire-plugin:2.12.4 (default-test) 131ms

  prepare-package 1s 161ms
    org.apache.maven.plugins:maven-jar-plugin:3.0.2 (default-jar) 348ms
    org.apache.maven.plugins:maven-dependency-plugin:2.8 (classpath-bare) 451ms
    org.apache.maven.plugins:maven-dependency-plugin:2.8 (classpath-jarinjar) 3ms
    com.googlecode.maven-download-plugin:download-maven-plugin:1.3.0 (fetch-onejar) 343ms
    com.googlecode.maven-download-plugin:download-maven-plugin:1.3.0 (fetch-jarinjar) 12ms

  initialize 943ms
    pl.project13.maven:git-commit-id-plugin:2.1.9 (default) 942ms

  package 7s 48ms
    org.apache.maven.plugins:maven-source-plugin:2.4 (attach-sources) 168ms
    org.apache.maven.plugins:maven-javadoc-plugin:2.10.3 (attach-javadocs) 1s 897ms
    com.comoyo.commons:emjar-maven-plugin:1.4-SNAPSHOT (package-emjar) 407ms
    org.apache.maven.plugins:maven-shade-plugin:2.4.3 (package-shade) 1s 851ms
    org.apache.maven.plugins:maven-assembly-plugin:2.4 (package-onejar) 1s 827ms
    org.apache.maven.plugins:maven-assembly-plugin:2.4 (package-jarinjar) 599ms
    org.springframework.boot:spring-boot-maven-plugin:1.4.3.RELEASE (package-springboot) 298ms

  integration-test 21s 478ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-bare-1) 1s 482ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-bare-2) 1s 512ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-emjar-1) 1s 512ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-emjar-2) 1s 508ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-shade-1) 1s 506ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-shade-2) 1s 505ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-onejar-1) 1s 905ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-onejar-2) 2s 9ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-springboot-1) 2s 208ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-springboot-2) 307ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-jarinjar-1) 3s 213ms
    org.codehaus.mojo:exec-maven-plugin:1.4.0 (execute-jarinjar-2) 2s 811ms

```

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
For accurate timing results, profile one alternative at a time.)

One of the challenges with multiple jars embedded in a single jar is
to make sure all references to per-jar META-INF information is handled
correctly -- pay attention to the jetty version string in the example
runs to see what happens when this fails.

Tool                 | Classpath scanning       | META-INF separation
---------------------|--------------------------|-------------------------
Maven Shade          | :heavy_check_mark:       | :heavy_exclamation_mark:
One-JAR              | :heavy_exclamation_mark: | :heavy_check_mark:
Spring Boot          | :heavy_exclamation_mark: | :heavy_check_mark:
Eclipse Runnable Jar | :heavy_exclamation_mark: | :heavy_exclamation_mark:
EmJar                | :heavy_check_mark:       | :heavy_check_mark:

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
jan 22, 2017 12:27:47 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (explicit registration)
2017-01-22 00:27:47.243:INFO::main: Logging initialized @219ms
2017-01-22 00:27:47.326:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:27:47.933:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@383dc82c{/,null,AVAILABLE}
2017-01-22 00:27:47.947:INFO:oejs.AbstractConnector:main: Started ServerConnector@222a59e6{HTTP/1.1,[http/1.1]}{0.0.0.0:43131}
2017-01-22 00:27:47.948:INFO:oejs.Server:main: Started @924ms
jan 22, 2017 12:27:48 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:48 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:48 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:48.356:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@222a59e6{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:48.360:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@383dc82c{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-bare-2) @ emjar-demo ---
jan 22, 2017 12:27:48 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (classpath scanning)
2017-01-22 00:27:48.659:INFO::main: Logging initialized @217ms
2017-01-22 00:27:48.738:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:27:49.421:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@4c2bb6e0{/,null,AVAILABLE}
2017-01-22 00:27:49.435:INFO:oejs.AbstractConnector:main: Started ServerConnector@51bf5add{HTTP/1.1,[http/1.1]}{0.0.0.0:40459}
2017-01-22 00:27:49.436:INFO:oejs.Server:main: Started @994ms
jan 22, 2017 12:27:49 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:49 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:49 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:49.809:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@51bf5add{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:49.815:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@4c2bb6e0{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-emjar-1) @ emjar-demo ---
jan 22, 2017 12:27:50 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (explicit registration)
2017-01-22 00:27:50.232:INFO::main: Logging initialized @276ms
2017-01-22 00:27:50.313:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:27:50.899:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@3632be31{/,null,AVAILABLE}
2017-01-22 00:27:50.911:INFO:oejs.AbstractConnector:main: Started ServerConnector@67784306{HTTP/1.1,[http/1.1]}{0.0.0.0:40613}
2017-01-22 00:27:50.912:INFO:oejs.Server:main: Started @956ms
jan 22, 2017 12:27:51 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:51 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:51 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:51.351:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@67784306{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:51.357:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@3632be31{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-emjar-2) @ emjar-demo ---
jan 22, 2017 12:27:51 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (classpath scanning)
2017-01-22 00:27:51.754:INFO::main: Logging initialized @293ms
2017-01-22 00:27:51.830:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:27:52.445:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@2286778{/,null,AVAILABLE}
2017-01-22 00:27:52.470:INFO:oejs.AbstractConnector:main: Started ServerConnector@210366b4{HTTP/1.1,[http/1.1]}{0.0.0.0:43725}
2017-01-22 00:27:52.470:INFO:oejs.Server:main: Started @1010ms
jan 22, 2017 12:27:52 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:52 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:52 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:52.880:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@210366b4{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:52.886:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@2286778{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-shade-1) @ emjar-demo ---
jan 22, 2017 12:27:53 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (explicit registration)
2017-01-22 00:27:53.351:INFO::main: Logging initialized @387ms
2017-01-22 00:27:53.412:INFO:oejs.Server:main: jetty-9.3.z-SNAPSHOT
2017-01-22 00:27:54.011:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@5745ca0e{/,null,AVAILABLE}
2017-01-22 00:27:54.023:INFO:oejs.AbstractConnector:main: Started ServerConnector@6a8658ff{HTTP/1.1,[http/1.1]}{0.0.0.0:39507}
2017-01-22 00:27:54.024:INFO:oejs.Server:main: Started @1060ms
jan 22, 2017 12:27:54 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:54 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:54 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:54.417:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@6a8658ff{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:54.421:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@5745ca0e{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-shade-2) @ emjar-demo ---
jan 22, 2017 12:27:54 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (classpath scanning)
2017-01-22 00:27:54.630:INFO::main: Logging initialized @161ms
2017-01-22 00:27:54.685:INFO:oejs.Server:main: jetty-9.3.z-SNAPSHOT
2017-01-22 00:27:55.558:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@333d4a8c{/,null,AVAILABLE}
2017-01-22 00:27:55.571:INFO:oejs.AbstractConnector:main: Started ServerConnector@516f41d9{HTTP/1.1,[http/1.1]}{0.0.0.0:35899}
2017-01-22 00:27:55.572:INFO:oejs.Server:main: Started @1103ms
jan 22, 2017 12:27:55 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:55 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:55 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:55.940:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@516f41d9{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:55.944:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@333d4a8c{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-onejar-1) @ emjar-demo ---
jan 22, 2017 12:27:56 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (explicit registration)
2017-01-22 00:27:56.873:INFO::main: Logging initialized @901ms
2017-01-22 00:27:56.925:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:27:57.475:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@67784306{/,null,AVAILABLE}
2017-01-22 00:27:57.487:INFO:oejs.AbstractConnector:main: Started ServerConnector@2b9627bc{HTTP/1.1,[http/1.1]}{0.0.0.0:46543}
2017-01-22 00:27:57.488:INFO:oejs.Server:main: Started @1515ms
jan 22, 2017 12:27:57 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:27:57 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:27:57 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:27:57.811:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@2b9627bc{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:57.815:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@67784306{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-onejar-2) @ emjar-demo ---
jan 22, 2017 12:27:58 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (classpath scanning)
2017-01-22 00:27:58.991:INFO::main: Logging initialized @1111ms
2017-01-22 00:27:59.039:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:27:59.645:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@527740a2{/,null,AVAILABLE}
2017-01-22 00:27:59.659:INFO:oejs.AbstractConnector:main: Started ServerConnector@6ddf90b0{HTTP/1.1,[http/1.1]}{0.0.0.0:35381}
2017-01-22 00:27:59.659:INFO:oejs.Server:main: Started @1779ms
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
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:36)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at com.simontuffs.onejar.Boot.run(Boot.java:342)
        at com.simontuffs.onejar.Boot.main(Boot.java:168)
2017-01-22 00:27:59.845:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@6ddf90b0{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:27:59.851:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@527740a2{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-springboot-1) @ emjar-demo ---
jan 22, 2017 12:28:00 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (explicit registration)
2017-01-22 00:28:00.276:INFO::main: Logging initialized @388ms
2017-01-22 00:28:00.400:INFO:oejs.Server:main: jetty-9.3.15.v20161220
2017-01-22 00:28:01.296:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@10bdf5e5{/,null,AVAILABLE}
2017-01-22 00:28:01.310:INFO:oejs.AbstractConnector:main: Started ServerConnector@78c03f1f{HTTP/1.1,[http/1.1]}{0.0.0.0:42741}
2017-01-22 00:28:01.311:INFO:oejs.Server:main: Started @1423ms
jan 22, 2017 12:28:01 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:28:01 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:28:01 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:28:01.980:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@78c03f1f{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:28:01.986:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@10bdf5e5{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-springboot-2) @ emjar-demo ---
jan 22, 2017 12:28:02 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (classpath scanning)
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
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:21)
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
jan 22, 2017 12:28:02 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (explicit registration)
2017-01-22 00:28:02.829:INFO::main: Logging initialized @426ms
2017-01-22 00:28:03.214:INFO:oejs.Server:main: jetty-9.3.z-SNAPSHOT
2017-01-22 00:28:04.574:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@6ef7623{/,null,AVAILABLE}
2017-01-22 00:28:04.601:INFO:oejs.AbstractConnector:main: Started ServerConnector@648ee871{HTTP/1.1,[http/1.1]}{0.0.0.0:38737}
2017-01-22 00:28:04.602:INFO:oejs.Server:main: Started @2200ms
jan 22, 2017 12:28:05 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Html: <html><body><h1>OK</h1></body></html>
jan 22, 2017 12:28:05 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Json: {"key":"value"}
jan 22, 2017 12:28:05 AM com.comoyo.commons.emjar.demo.Demo main
INFO: Xml: <HashMap><key>value</key></HashMap>
2017-01-22 00:28:05.554:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@648ee871{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:28:05.560:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@6ef7623{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (execute-jarinjar-2) @ emjar-demo ---
jan 22, 2017 12:28:05 AM com.comoyo.commons.emjar.demo.Demo main
INFO: === Running test (classpath scanning)
2017-01-22 00:28:05.977:INFO::main: Logging initialized @353ms
2017-01-22 00:28:06.219:INFO:oejs.Server:main: jetty-9.3.z-SNAPSHOT
2017-01-22 00:28:07.820:INFO:oejsh.ContextHandler:main: Started o.e.j.s.ServletContextHandler@43b0ade{/,null,AVAILABLE}
2017-01-22 00:28:07.834:INFO:oejs.AbstractConnector:main: Started ServerConnector@66434cc8{HTTP/1.1,[http/1.1]}{0.0.0.0:41235}
2017-01-22 00:28:07.835:INFO:oejs.Server:main: Started @2211ms
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
        at com.comoyo.commons.emjar.demo.Demo.main(Demo.java:36)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader.main(JarRsrcLoader.java:58)
2017-01-22 00:28:08.336:INFO:oejs.AbstractConnector:main: Stopped ServerConnector@66434cc8{HTTP/1.1,[http/1.1]}{0.0.0.0:0}
2017-01-22 00:28:08.342:INFO:oejsh.ContextHandler:main: Stopped o.e.j.s.ServletContextHandler@43b0ade{/,null,UNAVAILABLE}
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:exec (fencepost) @ emjar-demo ---
openjdk version "1.8.0_111"
OpenJDK Runtime Environment (build 1.8.0_111-b16)
OpenJDK 64-Bit Server VM (build 25.111-b16, mixed mode)

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

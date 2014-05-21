commons
=======

This is a collection of Java components that have proven useful across
different code repositories, initially in the Telenor Digital (née
Comoyo) Communications group

The list of components currently reads as follows:

* [amazonaws-utils](amazonaws-utils) AWS utilities not convered in the offical SDK
* [logging-context](logging-context) Structured application state/context for logging
* [logging-context-gelf](logging-context-gelf) Context-aware GELF log handler
* [logging-context-json](logging-context-json) Context-aware JSON/logstash log formatter
* [pb-json](pb-json) Hassle-free conversion from Protobuf to JSON and back
* [emjar](emjar) Class loader and supporting cast for using jar-in-jar embedded archives as part of classpath
* [emjar-maven-plugin](emjar-maven-plugin) Generate EmJar-enabled bundle archives from Maven

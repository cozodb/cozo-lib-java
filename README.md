# cozo-lib-java

[![Clojars Project](https://img.shields.io/clojars/v/com.github.zh217/cozo-lib-java.svg)](https://clojars.org/com.github.zh217/cozo-lib-java)

Java bindings for [CozoDb](https://github.com/cozodb/cozo).

## Install

Artefacts are on [Clojars](https://clojars.org/com.github.zh217/cozo-lib-java). 
For Gradle: 
```groovy
implementation("com.github.zh217:cozo-lib-java:0.1.0")
```
For Maven:
```xml
<dependency>
  <groupId>com.github.zh217</groupId>
  <artifactId>cozo-lib-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

You will need to add the maven repo `https://clojars.org/repo` to your package manager.

This project only uses Clojure for packaging, there is no Clojure dependency in code.
If you use clojure, please use [cozo-clj](https://github.com/cozodb/cozo-clj) instead.

## Usage

Import `org.cozodb.CozoJavaBridge` and
```java
// Create and open
CozoDb db = new CozoDb("_test_db");

// Query. Everything is string, 
// you need to use your own JSON library for parsing and encoding
System.out.println(db.query("?[] <- [['hello', 'world!']]", ""));
System.out.println(db.query("?[] <- [['hello', 'world', $name]]", 
                            "{\"name\":\"Java\"}"));
System.out.println(db.query("?[a] <- [[1, 2]]", ""));

// Close
db.close();
```
It is recommended to close the database object when you are done with it, otherwise
the native resources will not be released until your program quits.

## How it works

This library requires the native JNI dynamic library to be present in
`~/.cozo_java_native_lib/`. If not found, it will attempt to download one from
the [release page](https://github.com/cozodb/cozo/releases) for your system.
When this happens, you will get messages printed to `stderr` telling you what exactly
is happening. The download only happens once for each version.

In case a pre-compiled native library is not available for your system,
you will get an error when you import `CozoDb`.

## Compilation of the native library

You need to build the dynamic library for Java from the [Cozo main repo](https://github.com/cozodb/cozo/blob/main/BUILDING.md).
After that, copy the dynamic library to `~/.cozo_java_native_lib/` so that this package can find it.
You may need to rename your dynamic library: you can see what name is required from the message this package
prints when it attempts to download from the release page.
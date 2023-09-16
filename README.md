# cozo-lib-java

[![java](https://img.shields.io/maven-central/v/io.github.cozodb/cozo_java)](https://mvnrepository.com/artifact/io.github.cozodb/cozo_java)

Java bindings for [CozoDB](https://www.cozodb.org).

This document describes how to set up Cozo for use in Java.
To learn how to use CozoDB (CozoScript), read the [docs](https://docs.cozodb.org/en/latest/index.html).

If you are using Clojure, you should use [this](https://github.com/cozodb/cozo-clj), which provides a nicer wrapper in Clojure.

If you are on Android, [this](https://github.com/cozodb/cozo-lib-android) is what you want.

## Install

Artefacts are on [maven central](https://mvnrepository.com/artifact/io.github.cozodb/cozo_java):

```groovy
implementation 'io.github.cozodb:cozo_java:0.7.5'
```

## Usage

```java
import org.cozodb.CozoDb;

class Main {
    public static void main(String[] args) {
        try {
            CozoDb db = new CozoDb();
            System.out.println(db.query("?[] <- [['hello', 'world!']]"));
            db.close();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
```

For more details, see the JavaDocs for the `CozoDb` class.

## How it works

This library requires the native JNI dynamic library to be present in
`~/.cozo_java_native_lib/`. If not found, it will attempt to download one from
the [release page](https://github.com/cozodb/cozo/releases) for your system.
When this happens, you will get messages printed to `stderr` telling you what exactly
is happening. The download only happens once for each version.

In case a pre-compiled native library is not available for your system,
you will get an error when you import `CozoDb`.

This also means that you need to put the dynamic library in place manually
if you are running in an offline environment.

Why does it have to be this way? Because desktop Java does not allow easy packaging of 
platform-specific JAR files (but Android does with its AAR file format),
and packaging the native code for every platform together in the same JAR
is very wasteful.

## Compile the native library

See [here](https://github.com/cozodb/cozo/blob/main/cozo-lib-java).
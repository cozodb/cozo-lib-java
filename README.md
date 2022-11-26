# cozo-lib-java

Java bindings for [CozoDb](https://github.com/cozodb/cozo).

This document describes how to set up Cozo for use in Java.
To learn how to use CozoDB (CozoScript), follow
the [tutorial](https://nbviewer.org/github/cozodb/cozo-docs/blob/main/tutorial/tutorial.ipynb)
first and then read the [manual](https://cozodb.github.io/current/manual/). You can run all the queries
described in the tutorial with an in-browser DB [here](https://cozodb.github.io/wasm-demo/).

If you are using Clojure, you can use ..., which provides a nicer wrapper in Clojure.

## Install

Artefacts are on maven central.

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

You need to build the dynamic library for Java from
the [Cozo main repo](https://github.com/cozodb/cozo/blob/main/cozo-lib-java).
After that, copy the dynamic library to `~/.cozo_java_native_lib/` so that this package can find it.
You may need to rename your dynamic library: you can see what name is required from the message this package
prints when it attempts to download from the release page.
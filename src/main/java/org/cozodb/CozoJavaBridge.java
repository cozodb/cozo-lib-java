package org.cozodb;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class CozoJavaBridge {
    private final static String VERSION = "0.1.7";

    private static native int openDb(String kind, String path);

    private static native boolean closeDb(int id);

    private static native String runQuery(int id, String script, String params);

    private static native String exportRelations(int id, String rel);

    private static native String importRelations(int id, String data);

    private static native String backup(int id, String file);

    private static native String restore(int id, String file);

    private final int dbId;

    private static String getNativeLibFilename() {
        String os = "unknown";
        String arch = "unknown";
        String detectedOs = System.getProperty("os.name").toLowerCase();
        if (detectedOs.contains("mac")) {
            os = "mac";
        } else if (detectedOs.contains("windows")) {
            os = "windows";
        } else if (detectedOs.contains("linux")) {
            os = "linux";
        }
        String detectedArch = System.getProperty("os.arch").toLowerCase();
        if (detectedArch.contains("amd64") || detectedArch.contains("x86_64")) {
            arch = "x86_64";
        } else if (detectedArch.contains("aarch64")) {
            arch = "arm64";
        }
        String libExt = ".so";
        if (os.equals("mac")) {
            libExt = ".dylib";
        } else if (os.equals("windows")) {
            libExt = ".dll";
        }

        return "libcozo_java-" + CozoJavaBridge.VERSION + "-" + os + "-" + arch + libExt;
    }

    private static String getDownloadUrl() {
        return "https://github.com/cozodb/cozo/releases/download/v" + CozoJavaBridge.VERSION + "/" + getNativeLibFilename() + ".gz";
    }

    private static Path getLibFilePath() {
        String userHome = System.getProperty("user.home");
        Path libStorePath = Paths.get(userHome, ".cozo_java_native_lib");
        return Paths.get(libStorePath.toString(), CozoJavaBridge.getNativeLibFilename());
    }

    private static void createLibDir() throws IOException {
        String userHome = System.getProperty("user.home");
        Path libStorePath = Paths.get(userHome, ".cozo_java_native_lib");
        Files.createDirectories(libStorePath);
    }

    private static Path downloadNativeLib() throws IOException {
        createLibDir();

        Path libFilePath = CozoJavaBridge.getLibFilePath();

        if (Files.exists(libFilePath)) {
            return libFilePath;
        }

        String gzFilePath = libFilePath + ".gz";
        URL url = new URL(CozoJavaBridge.getDownloadUrl());

        System.err.println("Native lib not found, download from " + url);

        ReadableByteChannel rbc = Channels.newChannel(url.openStream());

        FileOutputStream fos = new FileOutputStream(gzFilePath);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        fos.close();
        rbc.close();

        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(gzFilePath))) {

            Files.copy(gis, libFilePath);
        }
        Files.delete(Paths.get(gzFilePath));

        System.err.println("Native library stored in " + libFilePath);
        return libFilePath;
    }

    static {
        try {
            Path path = downloadNativeLib();
            System.load(path.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CozoJavaBridge(String kind, String path) {
        int id = CozoJavaBridge.openDb(kind, path);
        if (id < 0) {
            throw new RuntimeException("cannot create database: error code " + id);
        }
        this.dbId = id;
    }

    public String query(String script, String params) {
        return CozoJavaBridge.runQuery(this.dbId, script, params);
    }

    public boolean close() {
        return CozoJavaBridge.closeDb(this.dbId);
    }

    public String exportRelations(String desc) {
        return CozoJavaBridge.exportRelations(this.dbId, desc);
    }

    public String exportRelation(String data) {
        return CozoJavaBridge.importRelations(this.dbId, data);
    }

    public String backup(String path) {
        return CozoJavaBridge.backup(this.dbId, path);
    }

    public String restore(String path) {
        return CozoJavaBridge.restore(this.dbId, path);
    }

    public static void main(String[] args) {
        CozoJavaBridge db = new CozoJavaBridge("mem", "");
        System.out.println(db);
        System.out.println(db.query("?[] <- [[1, 2, 3]]", ""));
        System.out.println(db.query("?[z] <- [[1, 2, 3]]", ""));
        System.out.println(db.close());
    }
}
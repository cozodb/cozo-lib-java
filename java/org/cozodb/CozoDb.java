package org.cozodb;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class CozoDb {
    private final static String VERSION = "0.1.4";

    private static native int openDb(String path);

    private static native boolean closeDb(int id);

    private static native String runQuery(int id, String script, String params);

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

        return "libcozo_java-" + CozoDb.VERSION + "-" + os + "-" + arch + libExt;
    }

    private static String getDownloadUrl() {
        return "https://github.com/cozodb/cozo/releases/download/v" + CozoDb.VERSION + "/" + getNativeLibFilename() + ".gz";
    }

    private static Path getLibFilePath() {
        String userHome = System.getProperty("user.home");
        Path libStorePath = Paths.get(userHome, ".cozo_java_native_lib");
        return Paths.get(libStorePath.toString(), CozoDb.getNativeLibFilename());
    }

    private static void createLibDir() throws IOException {
        String userHome = System.getProperty("user.home");
        Path libStorePath = Paths.get(userHome, ".cozo_java_native_lib");
        Files.createDirectories(libStorePath);
    }

    private static Path downloadNativeLib() throws IOException {
        createLibDir();

        Path libFilePath = CozoDb.getLibFilePath();

        if (Files.exists(libFilePath)) {
            return libFilePath;
        }

        String gzFilePath = libFilePath + ".gz";
        URL url = new URL(CozoDb.getDownloadUrl());

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

    public CozoDb(String path) {
        this.dbId = CozoDb.openDb(path);
    }

    public String query(String script, String params) {
        return CozoDb.runQuery(this.dbId, script, params);
    }

    public boolean close() {
        return CozoDb.closeDb(this.dbId);
    }

    public static void main(String[] args) {
        CozoDb db = new CozoDb("_test_db");
        System.out.println(db);
        System.out.println(db.query("?[] <- [[1, 2, 3]]", ""));
        System.out.println(db.query("?[z] <- [[1, 2, 3]]", ""));
        System.out.println(db.close());
    }
}
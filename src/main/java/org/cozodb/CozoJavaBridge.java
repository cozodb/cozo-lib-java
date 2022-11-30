/*
 * Copyright 2022, The Cozo Project Authors.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.cozodb;

import mjson.Json;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * The embedded CozoDb for Java, with everything in strings.
 * For what was expected of the arguments, see the corresponding methods in the CozoDb class
 * @see CozoDb
 */
public class CozoJavaBridge {
    private final static String VERSION = "0.2.1";

    private static native int openDb(String engine, String path, String options);

    private static native boolean closeDb(int id);

    private static native String runQuery(int id, String script, String params);

    private static native String exportRelations(int id, String rel);

    private static native String importRelations(int id, String data);

    private static native String backup(int id, String file);

    private static native String restore(int id, String file);

    private static native String importFromBackup(int id, String data);

    private final int dbId;

    private static String getNativeLibFilename() {
        String filename = "libcozo_java-" + VERSION;

        String detectedArch = System.getProperty("os.arch").toLowerCase();
        if (detectedArch.contains("amd64") || detectedArch.contains("x86_64")) {
            filename += "-x86_64";
        } else if (detectedArch.contains("aarch64")) {
            filename += "-aarch64";
        }

        String detectedOs = System.getProperty("os.name").toLowerCase();
        if (detectedOs.contains("mac")) {
            filename += "-apple-darwin.dylib";
        } else if (detectedOs.contains("windows")) {
            filename += "-pc-windows-msvc.dll";
        } else if (detectedOs.contains("linux")) {
            filename += "-unknown-linux-gnu.so";
        }

        return filename;
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

    /**
     * @see CozoDb
     * @param kind
     * @param path
     * @param options
     */
    public CozoJavaBridge(String kind, String path, String options) {
        int id = CozoJavaBridge.openDb(kind, path, options);
        if (id < 0) {
            throw new RuntimeException("cannot create database: error code " + id);
        }
        this.dbId = id;
    }

    /**
     * @see CozoDb#run(String, Json)
     * @param script
     * @param params
     * @return
     */
    public String query(String script, String params) {
        return CozoJavaBridge.runQuery(this.dbId, script, params);
    }

    /**
     * @see CozoDb#close()
     * @return
     */
    public boolean close() {
        return CozoJavaBridge.closeDb(this.dbId);
    }

    /**
     * @see CozoDb#exportRelations(List, boolean)
     * @param desc
     * @return
     */
    public String exportRelations(String desc) {
        return CozoJavaBridge.exportRelations(this.dbId, desc);
    }

    /**
     * @see CozoDb#importRelations(Json)
     * @param data
     * @return
     */
    public String importRelations(String data) {
        return CozoJavaBridge.importRelations(this.dbId, data);
    }

    /**
     * @see CozoDb#backup(String)
     * @param path
     * @return
     */
    public String backup(String path) {
        return CozoJavaBridge.backup(this.dbId, path);
    }

    /**
     * @see CozoDb#restore(String)
     * @param path
     * @return
     */
    public String restore(String path) {
        return CozoJavaBridge.restore(this.dbId, path);
    }

    /**
     * @see CozoDb#importRelationsFromBackup(String, List)
     * @param data
     * @return
     */
    public String importRelationsFromBackup(String data) {
        return CozoJavaBridge.importFromBackup(this.dbId, data);
    }

    /**
     * @hidden
     * @param args
     */
    public static void main(String[] args) {
        CozoJavaBridge db = new CozoJavaBridge("mem", "", "{}");
        System.out.println(db);
        System.out.println(db.query("?[] <- [[1, 2, 3]]", ""));
        System.out.println(db.query("?[z] <- [[1, 2, 3]]", ""));
        System.out.println(db.close());
    }
}
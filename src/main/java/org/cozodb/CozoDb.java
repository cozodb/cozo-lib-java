/*
 * Copyright 2022, The Cozo Project Authors.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.cozodb;

import mjson.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The embedded CozoDb for Java.
 * This class uses `mjson.Json` to process data to and from the database.
 * If you want something else, you should use the `CozoJavaBridge` class instead,
 * where everything is in strings.
 *
 * @see CozoJavaBridge
 */
public class CozoDb {
    /**
     * Exception class for CozoDb. Checked.
     */
    public static class CozoException extends Exception {
        /**
         * Contains the original error message
         */
        public Json details;

        CozoException(Json details) {
            this.details = details;
        }

        @Override
        public String toString() {
            Json display = this.details.at("display");
            if (display != null) {
                return display.asString();
            }
            Json message = this.details.at("message");
            if (message != null) {
                return message.asString();
            }
            return details.toString();
        }
    }

    /**
     * Represents headers of returned relations
     */
    public static class RelationHeader {
        /**
         * Header fields
         */
        public List<String> fields;
        Map<String, Integer> indices;

        RelationHeader(List<String> fields) {
            this.fields = fields;
            this.indices = new HashMap<>();
            if (fields != null) {
                for (int i = 0; i < fields.size(); i++) {
                    String field = fields.get(i);
                    this.indices.put(field, i);
                }
            }
        }
    }

    /**
     * Represents rows of returned relations
     */
    public static class RelationRow {
        /**
         * Headers for this row, may be null
         */
        public RelationHeader headers;
        /**
         * The data contained in this row
         */
        public List<Json> rows;

        RelationRow(RelationHeader headers, List<Json> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        /**
         * Get a field of this row
         *
         * @param i: integer index for the field
         * @return the field
         */
        public Json get(int i) {
            return this.rows.get(i);
        }

        /**
         * Get a field of this row
         *
         * @param key: string key for the field
         * @return the field
         */
        public Json get(String key) {
            int i = this.headers.indices.get(key);
            return this.rows.get(i);
        }

        @Override
        public String toString() {
            return this.rows.toString();
        }
    }

    CozoJavaBridge bridge;

    /**
     * Creates an in-memory, non-persistent DB.
     * The method `.close()` must be called when the DB is no longer needed,
     * unless you want it to live as long as your program.
     */
    public CozoDb() {
        this("mem", "", Json.object());
    }

    /**
     * Creates an DB.
     * The method `.close()` must be called when the DB is no longer needed,
     * unless you want it to live as long as your program.
     *
     * @param engine: may be "mem", "sqlite", "rocksdb" and others, depending on
     *                what was compiled in.
     * @param path:   the path to the storage file/directory, ignored by some engines
     */
    public CozoDb(String engine, String path) {
        this(engine, path, Json.object());
    }

    /**
     * Creates an DB.
     * The method `.close()` must be called when the DB is no longer needed,
     * unless you want it to live as long as your program.
     *
     * @param engine:  may be "mem", "sqlite", "rocksdb" and others, depending on
     *                 what was compiled in.
     * @param path:    the path to the storage file/directory, ignored by some engines
     * @param options: options for the storage engine, engine-dependent
     */
    public CozoDb(String engine, String path, Json options) {
        String opts_str = options.toString();
        this.bridge = new CozoJavaBridge(engine, path, opts_str);
    }

    /**
     * Close the DB and free any native resources associated with it.
     * If you fail to call this and the Java part was garbage-collected,
     * it is a resource-leak.
     */
    public void close() {
        bridge.close();
    }

    /**
     * Run CozoScript
     *
     * @param script: the CozoScript
     * @return a list of rows
     * @throws CozoException if any errors are encountered
     */
    public List<RelationRow> run(String script) throws CozoException {
        return this.run(script, Json.object());
    }

    /**
     * Run CozoScript
     *
     * @param script: the CozoScript
     * @param params: named parameters for the query
     * @return a list of rows
     * @throws CozoException if any errors are encountered
     */
    public List<RelationRow> run(String script, Json params) throws CozoException {
        String res = this.bridge.query(script, params.toString());
        Json retVal = Json.read(res);
        if (retVal.at("ok").asBoolean()) {
            Json headersJ = retVal.at("headers");
            List<String> fields = null;

            if (headersJ != null && !headersJ.isNull()) {
                fields = new ArrayList<>();
                for (Json header : headersJ.asJsonList()) {
                    fields.add(header.asString());
                }
            }

            RelationHeader headers = new RelationHeader(fields);

            List<Json> rows = retVal.at("rows").asJsonList();
            List<RelationRow> ret = new ArrayList<>();
            for (Json val : rows) {
                ret.add(new RelationRow(headers, val.asJsonList()));
            }

            return ret;
        } else {
            throw new CozoException(retVal);
        }
    }

    /**
     * Export several relations
     *
     * @param relations: names of the relations to export
     * @return the exported data in JSON
     * @throws CozoException if any errors are encountered
     */
    public Json exportRelations(List<String> relations) throws CozoException {
        Json rels = Json.array();
        for (String s : relations) {
            rels.add(s);
        }
        Json args = Json.object(
                "relations", rels
        );
        Json res = Json.read(bridge.exportRelations(args.toString()));
        if (res.at("ok").asBoolean()) {
            return res.at("data");
        } else {
            throw new CozoException(res);
        }
    }

    /**
     * Import data into the database. The relevant relations must exist
     * in the database beforehand.
     * Note that triggers are _not_ run for the relations, if any exists.
     * If you need to activate triggers, use queries with parameters.
     *
     * @param payload: in the format returned by `exportRelations`
     * @throws CozoException if any errors are encountered
     */
    public void importRelations(Json payload) throws CozoException {
        Json res = Json.read(bridge.importRelations(payload.toString()));
        if (res.at("ok").asBoolean()) {
            return;
        } else {
            throw new CozoException(res);
        }
    }

    /**
     * Backup the database
     *
     * @param path: path to the backup file
     * @throws CozoException if any errors are encountered, in particular if
     *                       the backup file exists and is not empty.
     */
    public void backup(String path) throws CozoException {
        Json res = Json.read(bridge.backup(path));
        if (res.at("ok").asBoolean()) {
            return;
        } else {
            throw new CozoException(res);
        }
    }

    /**
     * Restore the database from a backup
     *
     * @param path: path to the backup file
     * @throws CozoException if any errors are encountered, in particular if
     *                       the database contains any data.
     */
    public void restore(String path) throws CozoException {
        Json res = Json.read(bridge.restore(path));
        if (res.at("ok").asBoolean()) {
            return;
        } else {
            throw new CozoException(res);
        }
    }

    /**
     * Import data into the database, from a backup.
     * Note that triggers are _not_ run for the relations, if any exists.
     * If you need to activate triggers, use queries with parameters.
     *
     * @param path:      path to the backup file
     * @param relations: relations to import, these must exist in the database beforehand
     * @throws CozoException if any errors are encountered.
     */
    public void importRelationsFromBackup(String path, List<String> relations) throws CozoException {
        Json rels = Json.array();
        for (String s : relations) {
            rels.add(s);
        }
        Json args = Json.object(
                "relations", rels,
                "path", path
        );
        Json res = Json.read(bridge.importRelations(args.toString()));
        if (res.at("ok").asBoolean()) {
            return;
        } else {
            throw new CozoException(res);
        }
    }

    /**
     * @param args
     * @hidden
     */
    public static void main(String[] args) {
        CozoDb db = new CozoDb();

        try {
            System.out.println(db.run("?[] <- [[1,2,3]]"));

            db.run("?[a, b, c] <- [[1,2,3]]; :replace s {a, b, c}");

            List<String> l = new ArrayList<>();
            l.add("s");

            System.out.println(db.exportRelations(l));

            db.importRelations(Json.read("{\"s\": [{\"a\": 5, \"b\": 6, \"c\": 8}]}"));

            System.out.println(db.run("?[a, b, c] := *s[a, b, c]"));

//            db.backup("backup.db");
        } catch (CozoException e) {
            throw new RuntimeException(e);
        }
    }
}

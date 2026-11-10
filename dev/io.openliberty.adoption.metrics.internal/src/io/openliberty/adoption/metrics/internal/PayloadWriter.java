/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.adoption.metrics.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Writes the two files that IBM License Metric Tool's scanner reads:
 * <ul>
 * <li><code>iso-swid/&lt;product&gt;.ums.meta.json</code> - describes the metric and which properties to forward.</li>
 * <li><code>&lt;product&gt;.ums.data.json</code> - RFC 7464 JSON text sequence; each record starts with the ASCII
 * record separator (0x1E).</li>
 * </ul>
 * The data file is rotated at 100 KB into <code>&lt;product&gt;_N.ums.data.json</code>, keeping at most 10 rotated
 * files, as required by the LMT adoption metrics guide.
 */
public class PayloadWriter {

    public static final String MAPPING_ID = "liberty_adoption";
    public static final String METRIC_NAME = "INSTANCE_COUNT";
    public static final long MAX_FILE_BYTES = 100 * 1024;
    public static final int MAX_ROTATED_FILES = 10;
    private static final char RS = '\u001e';

    private final Path installDir;
    private final String filePrefix;

    public PayloadWriter(Path installDir, String filePrefix) {
        this.installDir = installDir;
        this.filePrefix = filePrefix;
    }

    /** The scanner only pairs data files with a metadata file in the same directory. */
    private Path swidDir() {
        return installDir.resolve("iso-swid");
    }

    Path dataFile() {
        return swidDir().resolve(filePrefix + ".ums.data.json");
    }

    Path metaFile() {
        return swidDir().resolve(filePrefix + ".ums.meta.json");
    }

    /** Writes the metadata file, only touching the disk if the content changed. */
    public void writeMetadata(String componentId, String componentName, String instancePath) throws IOException {
        String json = metadataJson(componentId, componentName, instancePath);
        Path meta = metaFile();
        if (Files.exists(meta) && json.equals(new String(Files.readAllBytes(meta), StandardCharsets.UTF_8))) {
            return;
        }
        Files.createDirectories(meta.getParent());
        writeAtomically(meta, json.getBytes(StandardCharsets.UTF_8));
    }

    /** Appends one record to the data file, rotating first if it is full. */
    public void appendRecord(Map<String, Object> properties, long timestampMillis) throws IOException {
        byte[] record = (RS + recordJson(properties, timestampMillis) + "\n").getBytes(StandardCharsets.UTF_8);
        Path data = dataFile();
        Files.createDirectories(data.getParent());
        if (Files.exists(data) && Files.size(data) + record.length > MAX_FILE_BYTES) {
            rotate();
        }
        Files.write(data, record, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Shifts _N to _N+1 (dropping the oldest), then moves the active file to _1. */
    void rotate() throws IOException {
        Files.deleteIfExists(rotated(MAX_ROTATED_FILES));
        for (int i = MAX_ROTATED_FILES - 1; i >= 1; i--) {
            Path from = rotated(i);
            if (Files.exists(from)) {
                Files.move(from, rotated(i + 1), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.move(dataFile(), rotated(1), StandardCopyOption.ATOMIC_MOVE);
    }

    private Path rotated(int n) {
        return swidDir().resolve(filePrefix + "_" + n + ".ums.data.json");
    }

    private static void writeAtomically(Path target, byte[] content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, content);
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    static String metadataJson(String componentId, String componentName, String instancePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"spec\": {\n");
        sb.append("    \"mappingId\": ").append(quote(MAPPING_ID)).append(",\n");
        sb.append("    \"processing\": \"LAST\",\n");
        sb.append("    \"repeat\": true,\n");
        sb.append("    \"breakdownDefs\": [\n      {\n");
        sb.append("        \"breakdownBy\": { \"name\": ").append(quote(METRIC_NAME)).append(" },\n");
        sb.append("        \"groupBy\": [");
        List<String> groupBy = AdoptionDataCollector.GROUP_BY;
        for (int i = 0; i < groupBy.size(); i++) {
            sb.append(i == 0 ? "\n          " : ",\n          ").append(quote(groupBy.get(i)));
        }
        sb.append("\n        ],\n");
        sb.append("        \"source\": \"metricQuantity\",\n");
        sb.append("        \"processing\": \"LAST\"\n      }\n    ],\n");
        sb.append("    \"data\": {\n");
        sb.append("      \"componentId\": ").append(quote(componentId)).append(",\n");
        sb.append("      \"componentName\": ").append(quote(componentName)).append(",\n");
        sb.append("      \"metricName\": ").append(quote(METRIC_NAME)).append(",\n");
        sb.append("      \"metricDescription\": \"Liberty runtime instance count and adoption telemetry\",\n");
        sb.append("      \"metricType\": \"ADOPTION\",\n");
        sb.append("      \"instanceId\": ").append(quote(instancePath)).append("\n");
        sb.append("    }\n  }\n}\n");
        return sb.toString();
    }

    static String recordJson(Map<String, Object> properties, long timestampMillis) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        StringBuilder sb = new StringBuilder();
        sb.append("[{\"mappingId\":").append(quote(MAPPING_ID));
        sb.append(",\"instances\":[{\"metrics\":[{");
        sb.append("\"measurementTimestamp\":").append(quote(fmt.format(new Date(timestampMillis))));
        sb.append(",\"metricQuantity\":1");
        for (Map.Entry<String, Object> e : properties.entrySet()) {
            sb.append(',').append(quote(e.getKey())).append(':');
            Object v = e.getValue();
            if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append(quote(String.valueOf(v)));
            }
        }
        sb.append("}]}]}]");
        return sb.toString();
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class PayloadWriterTest {

    private Path dir;
    private PayloadWriter writer;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("adoption-test");
        writer = new PayloadWriter(dir, "liberty");
    }

    @After
    public void tearDown() throws IOException {
        deleteRecursively(dir.toFile());
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteRecursively(k);
            }
        }
        f.delete();
    }

    private static Map<String, Object> props() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("version", "26.0.0.9");
        m.put("is_containerized", 0);
        m.put("features", "servlet-6.0,\"quoted\"");
        return m;
    }

    @Test
    public void dataFileStartsWithRecordSeparatorAndIsValidJson() throws Exception {
        writer.appendRecord(props(), 1700000000000L);
        byte[] bytes = Files.readAllBytes(dir.resolve("iso-swid").resolve("liberty.ums.data.json"));
        assertEquals(0x1e, bytes[0]);

        JSONArray arr = JSONArray.parse(new String(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8));
        JSONObject root = (JSONObject) arr.get(0);
        assertEquals("liberty_adoption", root.get("mappingId"));
        JSONObject metric = (JSONObject) ((JSONArray) ((JSONObject) ((JSONArray) root.get("instances")).get(0)).get("metrics")).get(0);
        assertEquals("2023-11-14T22:13:20.000Z", metric.get("measurementTimestamp"));
        assertEquals(1L, ((Number) metric.get("metricQuantity")).longValue());
        assertEquals("26.0.0.9", metric.get("version"));
        assertEquals(0L, ((Number) metric.get("is_containerized")).longValue());
        assertEquals("servlet-6.0,\"quoted\"", metric.get("features"));
    }

    @Test
    public void eachAppendAddsOneRecordSeparator() throws IOException {
        writer.appendRecord(props(), 1L);
        writer.appendRecord(props(), 2L);
        String s = new String(Files.readAllBytes(dir.resolve("iso-swid").resolve("liberty.ums.data.json")), StandardCharsets.UTF_8);
        assertEquals(2, s.length() - s.replace("\u001e", "").length());
    }

    @Test
    public void metadataGroupsByEveryCollectedPropertyAndMatchesMappingId() throws Exception {
        writer.writeMetadata("abc123", "Open Liberty", "/opt/ibm/wlp");
        String json = new String(Files.readAllBytes(dir.resolve("iso-swid").resolve("liberty.ums.meta.json")), StandardCharsets.UTF_8);
        JSONObject spec = (JSONObject) JSONObject.parse(json).get("spec");
        assertEquals(PayloadWriter.MAPPING_ID, spec.get("mappingId"));
        assertEquals(Boolean.TRUE, spec.get("repeat"));
        assertEquals("LAST", spec.get("processing"));

        JSONObject breakdown = (JSONObject) ((JSONArray) spec.get("breakdownDefs")).get(0);
        assertEquals("INSTANCE_COUNT", ((JSONObject) breakdown.get("breakdownBy")).get("name"));
        assertEquals(AdoptionDataCollector.GROUP_BY, breakdown.get("groupBy"));

        JSONObject data = (JSONObject) spec.get("data");
        assertEquals("abc123", data.get("componentId"));
        assertEquals("ADOPTION", data.get("metricType"));
    }

    @Test
    public void unchangedMetadataIsNotRewritten() throws Exception {
        writer.writeMetadata("abc123", "Open Liberty", "/opt/ibm/wlp");
        Path meta = dir.resolve("iso-swid").resolve("liberty.ums.meta.json");
        assertTrue(meta.toFile().setLastModified(1000L));
        writer.writeMetadata("abc123", "Open Liberty", "/opt/ibm/wlp");
        assertEquals(1000L, meta.toFile().lastModified());
    }

    @Test
    public void rotatesWhenActiveFileWouldExceedLimit() throws IOException {
        Map<String, Object> big = props();
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            pad.append('x');
        }
        big.put("features", pad.toString());

        for (int i = 0; i < 6; i++) {
            writer.appendRecord(big, i);
        }
        assertTrue(Files.exists(dir.resolve("iso-swid").resolve("liberty_1.ums.data.json")));
        assertTrue(Files.size(dir.resolve("iso-swid").resolve("liberty.ums.data.json")) <= PayloadWriter.MAX_FILE_BYTES);
        assertTrue(Files.size(dir.resolve("iso-swid").resolve("liberty_1.ums.data.json")) <= PayloadWriter.MAX_FILE_BYTES);
    }

    @Test
    public void keepsAtMostTenRotatedFiles() throws IOException {
        Map<String, Object> big = props();
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            pad.append('x');
        }
        big.put("features", pad.toString());

        for (int i = 0; i < 30; i++) {
            writer.appendRecord(big, i);
        }
        assertTrue(Files.exists(dir.resolve("iso-swid").resolve("liberty_10.ums.data.json")));
        assertFalse(Files.exists(dir.resolve("iso-swid").resolve("liberty_11.ums.data.json")));
    }

    @Test
    public void quoteEscapesControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\n\\u0001\"", PayloadWriter.quote("a\"b\\c\n\u0001"));
    }
}

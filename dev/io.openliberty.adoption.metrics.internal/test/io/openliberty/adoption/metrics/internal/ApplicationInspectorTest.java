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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ApplicationInspectorTest {

    private File dir;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("appinspect").toFile();
    }

    @After
    public void tearDown() {
        delete(dir);
    }

    private static void delete(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                delete(k);
            }
        }
        f.delete();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    /** Builds an archive in memory. */
    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue());
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private File write(String name, byte[] content) throws IOException {
        File f = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content);
        }
        return f;
    }

    private static Map<String, byte[]> entries() {
        return new LinkedHashMap<String, byte[]>();
    }

    @Test
    public void springBootJarIsRecognisedByItsManifest() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("META-INF/MANIFEST.MF", bytes("Manifest-Version: 1.0\nSpring-Boot-Version: 3.5.6\nMain-Class: org.x.JarLauncher\n"));
        e.put("BOOT-INF/classes/demo/App.class", bytes("x"));
        assertTrue(ApplicationInspector.inspect(write("demo.jar", zip(e))).springBoot);
    }

    @Test
    public void springBootWarIsRecognisedBySpringBootLibrary() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/lib/spring-boot-3.5.6.jar", bytes("x"));
        assertTrue(ApplicationInspector.inspect(write("app.war", zip(e))).springBoot);
    }

    @Test
    public void microProfileIsRecognisedByConfigFile() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/classes/META-INF/microprofile-config.properties", bytes("a=b"));
        ApplicationInspector.Result r = ApplicationInspector.inspect(write("mp.war", zip(e)));
        assertTrue(r.microProfile);
        assertFalse(r.springBoot);
    }

    @Test
    public void microProfileIsRecognisedByTypeNamesInAClassFile() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/classes/demo/Health.class", bytes("....Lorg/eclipse/microprofile/health/HealthCheck;...."));
        assertTrue(ApplicationInspector.inspect(write("mp.war", zip(e))).microProfile);
    }

    @Test
    public void microProfileIsRecognisedByLibraryName() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/lib/microprofile-config-api-3.1.jar", bytes("x"));
        assertTrue(ApplicationInspector.inspect(write("mp.war", zip(e))).microProfile);
    }

    @Test
    public void jakartaEeIsRecognisedByTypeNamesInAClassFile() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/classes/demo/Hello.class", bytes("....Ljakarta/servlet/http/HttpServlet;...."));
        ApplicationInspector.Result r = ApplicationInspector.inspect(write("web.war", zip(e)));
        assertTrue(r.jakartaEe);
        assertFalse(r.microProfile);
    }

    @Test
    public void javaxNamesAreAlsoJavaEe() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/classes/demo/Hello.class", bytes("....Ljavax/servlet/http/HttpServlet;...."));
        assertTrue(ApplicationInspector.inspect(write("web.war", zip(e))).jakartaEe);
    }

    @Test
    public void jakartaEeIsRecognisedByDescriptor() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/web.xml", bytes("<web-app/>"));
        assertTrue(ApplicationInspector.inspect(write("web.war", zip(e))).jakartaEe);
    }

    @Test
    public void plainJarWithNoFrameworkEvidenceFindsNothing() throws IOException {
        Map<String, byte[]> e = entries();
        e.put("demo/Plain.class", bytes("....Ljava/lang/String;...."));
        ApplicationInspector.Result r = ApplicationInspector.inspect(write("plain.jar", zip(e)));
        assertFalse(r.springBoot || r.microProfile || r.jakartaEe);
    }

    @Test
    public void warInsideAnEarIsInspected() throws IOException {
        Map<String, byte[]> war = entries();
        war.put("WEB-INF/classes/META-INF/microprofile-config.properties", bytes("a=b"));
        Map<String, byte[]> ear = entries();
        ear.put("META-INF/application.xml", bytes("<application/>"));
        ear.put("inner.war", zip(war));
        ApplicationInspector.Result r = ApplicationInspector.inspect(write("app.ear", zip(ear)));
        assertTrue(r.microProfile);
        assertTrue(r.jakartaEe);
    }

    @Test
    public void expandedDirectoryIsInspected() throws IOException {
        File web = new File(dir, "expanded/WEB-INF/classes/META-INF");
        assertTrue(web.mkdirs());
        Files.write(new File(web, "microprofile-config.properties").toPath(), bytes("a=b"));
        assertTrue(ApplicationInspector.inspect(new File(dir, "expanded")).microProfile);
    }

    @Test
    public void unreadableOrMissingApplicationYieldsNothing() throws IOException {
        File notAZip = write("broken.war", bytes("this is not a zip file"));
        ApplicationInspector.Result r = ApplicationInspector.inspect(notAZip);
        assertFalse(r.springBoot || r.microProfile || r.jakartaEe);

        r = ApplicationInspector.inspect(new File(dir, "does-not-exist.war"));
        assertFalse(r.springBoot || r.microProfile || r.jakartaEe);

        r = ApplicationInspector.inspect(null);
        assertFalse(r.springBoot || r.microProfile || r.jakartaEe);
    }

    @Test
    public void onlyTheStartOfALargeClassFileIsRead() throws IOException {
        // the marker sits well past MAX_READ, so it must not be seen
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ApplicationInspector.MAX_READ + 100; i++) {
            sb.append('a');
        }
        sb.append("Lorg/eclipse/microprofile/health/HealthCheck;");
        Map<String, byte[]> e = entries();
        e.put("WEB-INF/classes/demo/Big.class", bytes(sb.toString()));
        assertFalse(ApplicationInspector.inspect(write("big.war", zip(e))).microProfile);
    }

    @Test
    public void absoluteLocationIsFoundAndMissingOneIsNull() throws IOException {
        File app = write("a.war", zip(entries()));
        ApplicationInspector.Resolver same = new ApplicationInspector.Resolver() {
            @Override
            public String resolve(String value) {
                return value;
            }
        };
        assertEquals(app, ApplicationInspector.locate(app.getAbsolutePath(), same));
        assertNull(ApplicationInspector.locate(new File(dir, "nope.war").getAbsolutePath(), same));
        assertNull(ApplicationInspector.locate(null, same));
    }

    @Test
    public void relativeLocationIsLookedUpUnderTheServerAppsDirectory() throws IOException {
        File apps = new File(dir, "server/apps");
        assertTrue(apps.mkdirs());
        File app = new File(apps, "web.war");
        Files.write(app.toPath(), zip(entries()));

        final String serverDir = new File(dir, "server").getAbsolutePath();
        ApplicationInspector.Resolver resolver = new ApplicationInspector.Resolver() {
            @Override
            public String resolve(String value) {
                return value.replace("${server.config.dir}", serverDir).replace("${shared.app.dir}", serverDir + "/none");
            }
        };
        assertEquals(app, ApplicationInspector.locate("web.war", resolver));
    }
}

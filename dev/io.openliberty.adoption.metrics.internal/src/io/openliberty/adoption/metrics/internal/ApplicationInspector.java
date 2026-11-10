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

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Looks inside a deployed application (a war, ear, jar or expanded directory) to
 * find out which framework it is built on. The server configuration does not say,
 * so the application's own contents are the only evidence.
 *
 * <p>
 * The work is bounded so a large application cannot slow the server down: at most
 * {@link #MAX_ENTRIES} entry names are looked at, at most {@link #MAX_CLASS_FILES}
 * class files are opened, and only the first {@link #MAX_READ} bytes of any entry are
 * read (a class file's constant pool, which names every type it uses, is at the start).
 * Jars inside <code>WEB-INF/lib</code> are judged by file name only and never opened.
 * </p>
 *
 * <p>
 * Nothing here throws: an application that cannot be read simply yields no result.
 * </p>
 */
public final class ApplicationInspector {

    static final int MAX_ENTRIES = 20000;
    static final int MAX_CLASS_FILES = 200;
    static final int MAX_READ = 64 * 1024;
    private static final int MAX_NESTED_DEPTH = 2;

    private static final String MICROPROFILE_PACKAGE = "org/eclipse/microprofile/";

    /** Type names that only Jakarta EE (or Java EE) applications use. */
    private static final String[] JAKARTA_EE_PACKAGES = {
        "jakarta/servlet/", "jakarta/ws/rs/", "jakarta/enterprise/", "jakarta/persistence/", "jakarta/ejb/",
        "jakarta/faces/", "jakarta/inject/", "jakarta/json/", "jakarta/websocket/", "jakarta/jms/",
        "jakarta/transaction/", "jakarta/validation/", "jakarta/annotation/", "jakarta/mail/", "jakarta/security/",
        "jakarta/batch/", "jakarta/xml/", "jakarta/interceptor/", "jakarta/decorator/",
        "javax/servlet/", "javax/ws/rs/", "javax/enterprise/", "javax/persistence/", "javax/ejb/",
        "javax/faces/", "javax/inject/", "javax/json/", "javax/websocket/", "javax/jms/",
        "javax/transaction/", "javax/validation/", "javax/annotation/", "javax/mail/", "javax/interceptor/"
    };

    /** Deployment descriptors that only a Java EE / Jakarta EE module has. */
    private static final String[] JAKARTA_EE_DESCRIPTORS = {
        "web-inf/web.xml", "web-inf/beans.xml", "meta-inf/beans.xml", "meta-inf/persistence.xml",
        "web-inf/faces-config.xml", "meta-inf/application.xml", "meta-inf/ejb-jar.xml", "web-inf/ejb-jar.xml",
        "web-inf/classes/meta-inf/beans.xml", "web-inf/classes/meta-inf/persistence.xml"
    };

    private ApplicationInspector() {
    }

    /** What was found in one or more applications. */
    public static final class Result {
        public boolean springBoot;
        public boolean microProfile;
        public boolean jakartaEe;

        public void merge(Result other) {
            springBoot |= other.springBoot;
            microProfile |= other.microProfile;
            jakartaEe |= other.jakartaEe;
        }
    }

    /** Turns a configured application location into text with no variables, such as <code>${server.config.dir}</code>. */
    public interface Resolver {
        String resolve(String value);
    }

    /**
     * Finds the file for a configured application location. A relative location is looked
     * for under the server's apps directory and then the shared apps directory, as the
     * application manager does.
     *
     * @return the file, or null if it does not exist
     */
    public static File locate(String location, Resolver resolver) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        try {
            File direct = new File(resolver.resolve(location));
            if (direct.isAbsolute()) {
                return direct.exists() ? direct : null;
            }
            for (String base : new String[] { "${server.config.dir}/apps/", "${shared.app.dir}/" }) {
                File candidate = new File(resolver.resolve(base + location));
                if (candidate.exists()) {
                    return candidate;
                }
            }
        } catch (RuntimeException e) {
            // an unresolvable location is simply not inspected
        }
        return null;
    }

    /** Inspects one application. Never throws. */
    public static Result inspect(File app) {
        Scan scan = new Scan();
        try {
            if (app != null && app.isDirectory()) {
                scanDirectory(app, "", scan);
            } else if (app != null && app.isFile()) {
                scanArchive(app, scan);
            }
        } catch (IOException | RuntimeException e) {
            // not a readable archive; report whatever was found before the failure
        }
        return scan.result;
    }

    private static void scanArchive(File file, Scan scan) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements() && !scan.done()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                scan.onName(name);
                if (scan.wantsContent(name)) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        scan.onContent(name, read(in));
                    }
                } else if (isNestedModule(name)) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        scanStream(new ZipInputStream(in), scan, 1);
                    }
                }
            }
        }
    }

    /** A module inside an enterprise archive. Its own entries are read as a stream. */
    private static void scanStream(ZipInputStream zin, Scan scan, int depth) throws IOException {
        ZipEntry entry;
        while (!scan.done() && (entry = zin.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            scan.onName(name);
            if (scan.wantsContent(name)) {
                scan.onContent(name, read(zin));
            } else if (depth < MAX_NESTED_DEPTH && isNestedModule(name)) {
                scanStream(new ZipInputStream(new NoClose(zin)), scan, depth + 1);
            }
        }
    }

    private static void scanDirectory(File dir, String prefix, Scan scan) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (scan.done()) {
                return;
            }
            String name = prefix + child.getName();
            if (child.isDirectory()) {
                scanDirectory(child, name + "/", scan);
            } else {
                scan.onName(name);
                if (scan.wantsContent(name)) {
                    try (InputStream in = new java.io.FileInputStream(child)) {
                        scan.onContent(name, read(in));
                    }
                }
            }
        }
    }

    /** War files, and jars at the top level of an enterprise archive. Jars under lib/ are libraries. */
    private static boolean isNestedModule(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".war") || (lower.endsWith(".jar") && lower.indexOf('/') < 0);
    }

    private static byte[] read(InputStream in) throws IOException {
        byte[] buf = new byte[MAX_READ];
        int total = 0;
        int n;
        while (total < buf.length && (n = in.read(buf, total, buf.length - total)) > 0) {
            total += n;
        }
        byte[] result = new byte[total];
        System.arraycopy(buf, 0, result, 0, total);
        return result;
    }

    /** Collects evidence as entries are visited. */
    private static final class Scan {
        final Result result = new Result();
        private int entries;
        private int classFiles;

        boolean done() {
            return result.springBoot || entries >= MAX_ENTRIES;
        }

        void onName(String name) {
            entries++;
            String lower = name.toLowerCase(Locale.ROOT);

            if (lower.startsWith("boot-inf/") || lower.contains("/boot-inf/")) {
                result.springBoot = true;
            }
            if (lower.endsWith("meta-inf/microprofile-config.properties")) {
                result.microProfile = true;
            }
            for (String descriptor : JAKARTA_EE_DESCRIPTORS) {
                if (lower.equals(descriptor) || lower.endsWith("/" + descriptor)) {
                    result.jakartaEe = true;
                }
            }
            if (lower.startsWith("web-inf/lib/") && lower.endsWith(".jar")) {
                String jar = lower.substring(lower.lastIndexOf('/') + 1);
                if (jar.contains("microprofile") || jar.startsWith("smallrye-")) {
                    result.microProfile = true;
                }
                if (jar.startsWith("jakarta.") || jar.startsWith("javax.")) {
                    result.jakartaEe = true;
                }
                if (jar.startsWith("spring-boot-") || jar.startsWith("spring-boot.")) {
                    result.springBoot = true;
                }
            }
        }

        /** True if this entry's bytes can add to the evidence. */
        boolean wantsContent(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith("meta-inf/manifest.mf")) {
                return true;
            }
            // MicroProfile outranks Jakarta EE, so stop opening classes once it is found.
            return lower.endsWith(".class") && classFiles < MAX_CLASS_FILES && !result.microProfile;
        }

        void onContent(String name, byte[] data) {
            String lower = name.toLowerCase(Locale.ROOT);
            String text = new String(data, StandardCharsets.ISO_8859_1);
            if (lower.endsWith("meta-inf/manifest.mf")) {
                if (text.contains("Spring-Boot-Version")) {
                    result.springBoot = true;
                }
                return;
            }
            classFiles++;
            if (text.contains(MICROPROFILE_PACKAGE)) {
                result.microProfile = true;
            }
            if (!result.jakartaEe) {
                for (String pkg : JAKARTA_EE_PACKAGES) {
                    if (text.contains(pkg)) {
                        result.jakartaEe = true;
                        break;
                    }
                }
            }
        }
    }

    /** Stops the nested archive reader from closing the enterprise archive stream. */
    private static final class NoClose extends FilterInputStream {
        NoClose(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // the outer reader owns the stream
        }
    }
}

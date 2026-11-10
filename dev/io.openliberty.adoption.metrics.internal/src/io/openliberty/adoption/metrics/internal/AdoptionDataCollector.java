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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns what the server knows about itself into the flat set of properties
 * that are written to the adoption metrics payload.
 *
 * <p>
 * Every key returned by {@link #collect(Inputs)} must also be listed in
 * {@link #GROUP_BY}. IBM License Metric Tool only forwards properties that
 * appear in the metadata <code>groupBy</code> list, and everything in that list
 * becomes part of the aggregation key. Do not add per-report values (such as a
 * timestamp) here - they would split every report into its own bucket.
 * </p>
 */
public class AdoptionDataCollector {

    public static final String UNKNOWN = EnvironmentDetector.UNKNOWN;

    /** Property names, in the order they are written. */
    public static final List<String> GROUP_BY = Collections.unmodifiableList(Arrays.asList(
        "product_name",
        "product_edition",
        "version",
        "support_status",
        "java_version",
        "java_vendor",
        "jvm",
        "os_name",
        "os_version",
        "infrastructure_type",
        "is_containerized",
        "container_orchestrator",
        "hypervisor",
        "cloud_provider",
        "environment_type",
        "application_count",
        "features",
        "application_framework",
        "instance_id",
        "customer_id"));

    /** Values the collector cannot read from the JVM or environment. */
    public static class Inputs {
        public String productName;
        public String productVersion;
        public String productEdition;
        public boolean beta;
        public Set<String> features;
        public int applicationCount;
        /** True when at least one deployed application is a Spring Boot application. */
        public boolean springBootDeployed;
        /** True when the contents of a deployed application show MicroProfile APIs in use. */
        public boolean microProfileDeployed;
        /** True when the contents of a deployed application show Jakarta EE (or Java EE) APIs in use. */
        public boolean jakartaEeDeployed;
        public String environmentType;
        public String customerId;
        public String installDirectory;
        public String serverName;
        public String hostName;
    }

    private final EnvironmentDetector environment;
    private final Map<String, String> systemProperties;

    public AdoptionDataCollector(EnvironmentDetector environment, Map<String, String> systemProperties) {
        this.environment = environment;
        this.systemProperties = systemProperties;
    }

    public Map<String, Object> collect(Inputs in) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        boolean container = environment.isContainer();

        m.put("product_name", orUnknown(in.productName));
        m.put("product_edition", orUnknown(in.productEdition));
        m.put("version", orUnknown(in.productVersion));
        m.put("support_status", supportStatus(in.beta));
        m.put("java_version", orUnknown(systemProperties.get("java.version")));
        m.put("java_vendor", orUnknown(systemProperties.get("java.vendor")));
        m.put("jvm", EnvironmentDetector.jvm(systemProperties.get("java.vm.name")));
        m.put("os_name", orUnknown(systemProperties.get("os.name")));
        m.put("os_version", orUnknown(systemProperties.get("os.version")));
        m.put("infrastructure_type", environment.infrastructureType());
        m.put("is_containerized", container ? 1 : 0);
        m.put("container_orchestrator", environment.orchestrator());
        m.put("hypervisor", environment.hypervisor());
        m.put("cloud_provider", environment.cloudProvider());
        m.put("environment_type", normalizeEnvironmentType(in.environmentType));
        m.put("application_count", in.applicationCount);
        m.put("features", join(in.features));
        m.put("application_framework", applicationFramework(in.springBootDeployed, in.microProfileDeployed, in.jakartaEeDeployed));
        m.put("instance_id", instanceId(in.installDirectory, in.serverName, in.hostName));
        m.put("customer_id", in.customerId == null || in.customerId.trim().isEmpty() ? UNKNOWN : in.customerId.trim());
        return m;
    }

    /**
     * The server only knows that a build is a beta. It has no knowledge of
     * end-of-service dates, so every other build is reported as unknown rather
     * than guessing.
     */
    static String supportStatus(boolean beta) {
        return beta ? "unsupported" : UNKNOWN;
    }

    static String normalizeEnvironmentType(String configured) {
        if (configured == null) {
            return UNKNOWN;
        }
        String v = configured.trim().toLowerCase(Locale.ROOT);
        if (v.equals("production") || v.equals("non-production")) {
            return v;
        }
        return UNKNOWN;
    }

    /**
     * The framework comes from what the deployed applications contain, not from which
     * features are enabled. When an application uses several, the most specific one
     * wins: Spring Boot, then MicroProfile, then Jakarta EE.
     */
    static String applicationFramework(boolean springBoot, boolean microProfile, boolean jakartaEe) {
        if (springBoot) {
            return "spring-boot";
        }
        if (microProfile) {
            return "microprofile";
        }
        if (jakartaEe) {
            return "jakarta-ee";
        }
        return UNKNOWN;
    }

    /**
     * A stable, non-reversible identifier for this server. The host name is part of
     * the input, so containers that get a new host name on every start will get a
     * new id on every start.
     */
    static String instanceId(String installDirectory, String serverName, String hostName) {
        String input = nz(installDirectory) + '\n' + nz(serverName) + '\n' + nz(hostName);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return UNKNOWN;
        }
    }

    private static String join(Set<String> features) {
        if (features == null || features.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String f : new TreeSet<String>(features)) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(f);
        }
        return sb.toString();
    }

    private static String orUnknown(String s) {
        return s == null || s.isEmpty() ? UNKNOWN : s;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

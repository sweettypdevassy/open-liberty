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
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * Best-effort detection of where the server is running. Every method makes a
 * local decision from environment variables and a few well-known files. It
 * never makes a network call (for example to a cloud metadata endpoint).
 *
 * <p>
 * Results are heuristics. When nothing can be determined the value is
 * {@link #UNKNOWN}; when the check ran and found nothing the value is
 * {@link #NONE}.
 * </p>
 */
public class EnvironmentDetector {

    public static final String UNKNOWN = "unknown";
    public static final String NONE = "none";

    /** Reads a text file; returns null if it does not exist or cannot be read. */
    public interface FileSource {
        String read(String path);
    }

    private static final String DMI = "/sys/class/dmi/id/";

    private final Map<String, String> env;
    private final FileSource files;

    public EnvironmentDetector(Map<String, String> env, FileSource files) {
        this.env = env;
        this.files = files;
    }

    /** Detector backed by the real environment and file system. */
    public static EnvironmentDetector system() {
        return new EnvironmentDetector(System.getenv(), new FileSource() {
            @Override
            public String read(String path) {
                try {
                    Path p = Paths.get(path);
                    if (!Files.isRegularFile(p)) {
                        return null;
                    }
                    // /proc and /sys files report size 0, so read through a bounded stream
                    byte[] buf = new byte[16384];
                    int total = 0;
                    try (java.io.InputStream in = Files.newInputStream(p)) {
                        int n;
                        while (total < buf.length && (n = in.read(buf, total, buf.length - total)) > 0) {
                            total += n;
                        }
                    }
                    return new String(buf, 0, total, StandardCharsets.UTF_8);
                } catch (IOException | RuntimeException e) {
                    // includes SecurityException; treat as "not available"
                    return null;
                }
            }
        });
    }

    public boolean isContainer() {
        if (env.get("KUBERNETES_SERVICE_HOST") != null || env.get("container") != null) {
            return true;
        }
        if (files.read("/.dockerenv") != null || files.read("/run/.containerenv") != null) {
            return true;
        }
        String cgroup = lower(files.read("/proc/1/cgroup"));
        if (containsAny(cgroup, "docker", "kubepods", "containerd", "crio", "libpod")) {
            return true;
        }
        // cgroup v2 hosts show container paths in the mount table instead
        return hasContainerBindMounts(files.read("/proc/self/mountinfo"));
    }

    /**
     * A container runtime bind-mounts the container's own hostname, hosts and
     * resolv.conf files over /etc. A host that merely runs containers also lists
     * paths such as /var/lib/docker/containers/&lt;id&gt;/mounts/shm, so a path match
     * alone would report such a host as a container.
     */
    static boolean hasContainerBindMounts(String mountInfo) {
        if (mountInfo == null) {
            return false;
        }
        for (String line : mountInfo.split("\n")) {
            // fields: id parent major:minor root mountPoint options ...
            String[] f = line.trim().split(" ");
            if (f.length < 5) {
                continue;
            }
            String mountPoint = f[4];
            if (("/etc/hostname".equals(mountPoint) || "/etc/hosts".equals(mountPoint) || "/etc/resolv.conf".equals(mountPoint))
                && containsAny(f[3].toLowerCase(Locale.ROOT), "/docker/containers/", "/kubelet/pods/", "/containers/storage/",
                               "/overlay-containers/")) {
                return true;
            }
        }
        return false;
    }

    public String orchestrator() {
        if (env.get("KUBERNETES_SERVICE_HOST") != null) {
            for (String key : env.keySet()) {
                if (key.startsWith("OPENSHIFT_")) {
                    return "openshift";
                }
            }
            return "kubernetes";
        }
        if (env.get("ECS_CONTAINER_METADATA_URI") != null || env.get("ECS_CONTAINER_METADATA_URI_V4") != null) {
            return "ecs";
        }
        if (env.get("NOMAD_ALLOC_ID") != null) {
            return "nomad";
        }
        if (env.get("MESOS_TASK_ID") != null || env.get("MESOS_SANDBOX") != null) {
            return "mesos";
        }
        if (env.get("CF_INSTANCE_GUID") != null) {
            return "cloud-foundry";
        }
        return NONE;
    }

    public String hypervisor() {
        String vendor = lower(files.read(DMI + "sys_vendor"));
        String product = lower(files.read(DMI + "product_name"));
        String bios = lower(files.read(DMI + "bios_vendor"));
        String cpuinfo = lower(files.read("/proc/cpuinfo"));
        String sysinfo = lower(files.read("/proc/sysinfo"));
        String xenType = lower(files.read("/sys/hypervisor/type"));

        if (vendor == null && product == null && cpuinfo == null && sysinfo == null && xenType == null) {
            return UNKNOWN;
        }
        if (containsAny(vendor, "vmware") || containsAny(product, "vmware")) {
            return "vmware";
        }
        if (containsAny(vendor, "microsoft") && containsAny(product, "virtual machine")) {
            return "hyper-v";
        }
        if (containsAny(vendor, "innotek") || containsAny(product, "virtualbox")) {
            return "virtualbox";
        }
        if (containsAny(vendor, "parallels")) {
            return "parallels";
        }
        if (containsAny(vendor, "xen") || containsAny(bios, "xen") || containsAny(xenType, "xen")) {
            return "xen";
        }
        if (containsAny(vendor, "amazon") || containsAny(bios, "amazon")) {
            return "nitro";
        }
        if (containsAny(sysinfo, "z/vm")) {
            return "z/vm";
        }
        if (containsAny(vendor, "qemu") || containsAny(product, "kvm", "qemu") || containsAny(bios, "seabios")
            || containsAny(product, "google compute engine") || containsAny(sysinfo, "kvm/linux")) {
            return "kvm";
        }
        if (containsAny(cpuinfo, "powervm")) {
            return "powervm";
        }
        if (containsAny(sysinfo, "lpar number")) {
            return "pr/sm";
        }
        if (containsAny(cpuinfo, " hypervisor")) {
            return "unknown-hypervisor";
        }
        return NONE;
    }

    public String cloudProvider() {
        String vendor = lower(files.read(DMI + "sys_vendor"));
        String product = lower(files.read(DMI + "product_name"));
        String bios = lower(files.read(DMI + "bios_vendor"));
        String assetTag = lower(files.read(DMI + "chassis_asset_tag"));

        if (containsAny(vendor, "amazon") || containsAny(bios, "amazon")) {
            return "aws";
        }
        if (containsAny(product, "google compute engine")) {
            return "gcp";
        }
        if (containsAny(assetTag, "7783-7084-3265-9085-8269-3286-77")) {
            return "azure";
        }
        if (containsAny(assetTag, "oraclecloud.com")) {
            return "oracle";
        }
        if (containsAny(vendor, "digitalocean")) {
            return "digitalocean";
        }
        if (containsAny(vendor, "alibaba")) {
            return "alibaba";
        }
        // Platforms where a container cannot see the host DMI data
        if (env.get("AWS_EXECUTION_ENV") != null || env.get("AWS_REGION") != null) {
            return "aws";
        }
        if (env.get("K_SERVICE") != null || env.get("GOOGLE_CLOUD_PROJECT") != null) {
            return "gcp";
        }
        if (env.get("WEBSITE_SITE_NAME") != null || env.get("WEBSITE_INSTANCE_ID") != null) {
            return "azure";
        }
        if (env.get("CE_APP") != null || env.get("CE_SUBDOMAIN") != null) {
            return "ibm-cloud";
        }
        boolean sawDmi = vendor != null || product != null || bios != null;
        return sawDmi ? NONE : UNKNOWN;
    }

    /**
     * Describes the immediate runtime layer. A container wins over cloud, VM and
     * bare metal because the other layers are reported in their own fields.
     */
    public String infrastructureType() {
        if (isContainer()) {
            return "container";
        }
        String cloud = cloudProvider();
        if (!NONE.equals(cloud) && !UNKNOWN.equals(cloud)) {
            return "cloud";
        }
        String hv = hypervisor();
        if (UNKNOWN.equals(hv)) {
            return UNKNOWN;
        }
        return NONE.equals(hv) ? "bare-metal" : "vm";
    }

    /** Maps the java.vm.name system property to OpenJ9 / HotSpot. */
    public static String jvm(String vmName) {
        String n = lower(vmName);
        if (n == null) {
            return UNKNOWN;
        }
        if (n.contains("openj9")) {
            return "OpenJ9";
        }
        if (n.contains("j9")) {
            return "IBM J9";
        }
        if (n.contains("hotspot") || n.contains("openjdk") || n.contains("server vm") || n.contains("client vm")) {
            return "HotSpot";
        }
        return vmName;
    }

    private static String lower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null) {
            return false;
        }
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

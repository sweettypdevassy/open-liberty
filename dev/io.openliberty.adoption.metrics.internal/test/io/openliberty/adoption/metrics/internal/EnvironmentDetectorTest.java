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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EnvironmentDetectorTest {

    private static EnvironmentDetector detector(Map<String, String> env, final Map<String, String> files) {
        return new EnvironmentDetector(env, new EnvironmentDetector.FileSource() {
            @Override
            public String read(String path) {
                return files.get(path);
            }
        });
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    public void kubernetesEnvIsContainerAndKubernetes() {
        EnvironmentDetector d = detector(map("KUBERNETES_SERVICE_HOST", "10.0.0.1"), map());
        assertEquals(true, d.isContainer());
        assertEquals("kubernetes", d.orchestrator());
        assertEquals("container", d.infrastructureType());
    }

    @Test
    public void openshiftIsDistinguishedFromKubernetes() {
        EnvironmentDetector d = detector(map("KUBERNETES_SERVICE_HOST", "10.0.0.1", "OPENSHIFT_BUILD_NAME", "x"), map());
        assertEquals("openshift", d.orchestrator());
    }

    @Test
    public void dockerEnvFileMeansContainerWithNoOrchestrator() {
        EnvironmentDetector d = detector(map(), map("/.dockerenv", ""));
        assertEquals(true, d.isContainer());
        assertEquals("none", d.orchestrator());
    }

    @Test
    public void cgroupV1DetectsContainer() {
        EnvironmentDetector d = detector(map(), map("/proc/1/cgroup", "12:cpu:/docker/abc123"));
        assertEquals(true, d.isContainer());
    }

    @Test
    public void plainHostIsNotContainer() {
        EnvironmentDetector d = detector(map(), map("/proc/1/cgroup", "0::/init.scope"));
        assertEquals(false, d.isContainer());
    }

    @Test
    public void cgroupV2ContainerIsDetectedFromEtcBindMounts() {
        String mountInfo = "700 650 0:50 / / rw,relatime - overlay overlay rw\n"
                           + "701 700 8:1 /var/lib/docker/containers/abc/resolv.conf /etc/resolv.conf rw,relatime - ext4 /dev/sda1 rw\n";
        EnvironmentDetector d = detector(map(), map("/proc/self/mountinfo", mountInfo));
        assertEquals(true, d.isContainer());
    }

    @Test
    public void hostThatRunsDockerIsNotReportedAsContainer() {
        // The host's own mount table lists container paths, but not as bind mounts over /etc
        String mountInfo = "28 1 8:1 / / rw,relatime - ext4 /dev/sda1 rw\n"
                           + "2345 28 0:50 / /var/lib/docker/containers/abc/mounts/shm rw,nosuid - tmpfs shm rw,size=65536k\n"
                           + "2346 28 0:51 / /var/lib/kubelet/pods/p1/volumes/kubernetes.io~empty-dir/x rw - tmpfs tmpfs rw\n";
        EnvironmentDetector d = detector(map(), map("/proc/1/cgroup", "0::/init.scope", "/proc/self/mountinfo", mountInfo));
        assertEquals(false, d.isContainer());
    }

    @Test
    public void vmwareGuestIsVmWithVmwareHypervisor() {
        Map<String, String> files = map("/sys/class/dmi/id/sys_vendor", "VMware, Inc.\n",
                                        "/sys/class/dmi/id/product_name", "VMware Virtual Platform\n");
        EnvironmentDetector d = detector(map(), files);
        assertEquals("vmware", d.hypervisor());
        assertEquals("vm", d.infrastructureType());
        assertEquals("none", d.cloudProvider());
    }

    @Test
    public void awsNitroIsCloudAndNitro() {
        Map<String, String> files = map("/sys/class/dmi/id/sys_vendor", "Amazon EC2\n",
                                        "/sys/class/dmi/id/bios_vendor", "Amazon EC2\n");
        EnvironmentDetector d = detector(map(), files);
        assertEquals("aws", d.cloudProvider());
        assertEquals("nitro", d.hypervisor());
        assertEquals("cloud", d.infrastructureType());
    }

    @Test
    public void azureDetectedFromChassisAssetTag() {
        Map<String, String> files = map("/sys/class/dmi/id/sys_vendor", "Microsoft Corporation\n",
                                        "/sys/class/dmi/id/product_name", "Virtual Machine\n",
                                        "/sys/class/dmi/id/chassis_asset_tag", "7783-7084-3265-9085-8269-3286-77\n");
        EnvironmentDetector d = detector(map(), files);
        assertEquals("azure", d.cloudProvider());
        assertEquals("hyper-v", d.hypervisor());
    }

    @Test
    public void dmiPresentButNothingMatchesIsBareMetal() {
        Map<String, String> files = map("/sys/class/dmi/id/sys_vendor", "Dell Inc.\n",
                                        "/sys/class/dmi/id/product_name", "PowerEdge R750\n",
                                        "/proc/cpuinfo", "flags : fpu vme sse\n");
        EnvironmentDetector d = detector(map(), files);
        assertEquals("none", d.hypervisor());
        assertEquals("bare-metal", d.infrastructureType());
    }

    @Test
    public void nothingReadableIsUnknownNotBareMetal() {
        EnvironmentDetector d = detector(map(), map());
        assertEquals("unknown", d.hypervisor());
        assertEquals("unknown", d.cloudProvider());
        assertEquals("unknown", d.infrastructureType());
    }

    @Test
    public void hypervisorFlagInCpuInfoWithoutDmiIsUnknownHypervisor() {
        EnvironmentDetector d = detector(map(), map("/proc/cpuinfo", "flags : fpu hypervisor lm\n"));
        assertEquals("unknown-hypervisor", d.hypervisor());
    }

    @Test
    public void jvmNames() {
        assertEquals("OpenJ9", EnvironmentDetector.jvm("Eclipse OpenJ9 VM"));
        assertEquals("HotSpot", EnvironmentDetector.jvm("OpenJDK 64-Bit Server VM"));
        assertEquals("IBM J9", EnvironmentDetector.jvm("IBM J9 VM"));
        assertEquals("unknown", EnvironmentDetector.jvm(null));
    }
}

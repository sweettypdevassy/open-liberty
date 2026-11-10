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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class AdoptionDataCollectorTest {

    private static AdoptionDataCollector.Inputs inputs() {
        AdoptionDataCollector.Inputs in = new AdoptionDataCollector.Inputs();
        in.productName = "Open Liberty";
        in.productVersion = "26.0.0.9";
        in.productEdition = "Open";
        in.features = new HashSet<String>(Arrays.asList("servlet-6.0", "restfulWS-3.1"));
        in.applicationCount = 3;
        in.environmentType = "production";
        in.customerId = "cust-42";
        in.installDirectory = "/opt/ibm/wlp";
        in.serverName = "defaultServer";
        in.hostName = "host1";
        return in;
    }

    private static AdoptionDataCollector collector(EnvironmentDetector env) {
        Map<String, String> sys = new HashMap<String, String>();
        sys.put("java.version", "17.0.10");
        sys.put("java.vendor", "Eclipse Adoptium");
        sys.put("java.vm.name", "OpenJDK 64-Bit Server VM");
        sys.put("os.name", "Linux");
        sys.put("os.version", "5.14.0");
        return new AdoptionDataCollector(env, sys);
    }

    private static EnvironmentDetector plainHost() {
        return new EnvironmentDetector(new HashMap<String, String>(), new EnvironmentDetector.FileSource() {
            @Override
            public String read(String path) {
                return null;
            }
        });
    }

    @Test
    public void everyCollectedKeyIsInGroupByAndViceVersa() {
        Map<String, Object> data = collector(plainHost()).collect(inputs());
        assertEquals(AdoptionDataCollector.GROUP_BY, new java.util.ArrayList<String>(data.keySet()));
    }

    @Test
    public void collectsRequestedValues() {
        Map<String, Object> d = collector(plainHost()).collect(inputs());
        assertEquals("Open Liberty", d.get("product_name"));
        assertEquals("Open", d.get("product_edition"));
        assertEquals("26.0.0.9", d.get("version"));
        assertEquals("17.0.10", d.get("java_version"));
        assertEquals("Eclipse Adoptium", d.get("java_vendor"));
        assertEquals("HotSpot", d.get("jvm"));
        assertEquals("Linux", d.get("os_name"));
        assertEquals("5.14.0", d.get("os_version"));
        assertEquals(0, d.get("is_containerized"));
        assertEquals("production", d.get("environment_type"));
        assertEquals(3, d.get("application_count"));
        assertEquals("restfulWS-3.1,servlet-6.0", d.get("features"));
        assertEquals("cust-42", d.get("customer_id"));
    }

    @Test
    public void supportStatusIsUnknownUnlessBeta() {
        assertEquals("unknown", AdoptionDataCollector.supportStatus(false));
        assertEquals("unsupported", AdoptionDataCollector.supportStatus(true));
    }

    @Test
    public void environmentTypeOnlyAcceptsKnownValues() {
        assertEquals("production", AdoptionDataCollector.normalizeEnvironmentType(" Production "));
        assertEquals("non-production", AdoptionDataCollector.normalizeEnvironmentType("non-production"));
        assertEquals("unknown", AdoptionDataCollector.normalizeEnvironmentType("prod"));
        assertEquals("unknown", AdoptionDataCollector.normalizeEnvironmentType(null));
    }

    @Test
    public void frameworkIsTheMostSpecificOneFound() {
        assertEquals("spring-boot", AdoptionDataCollector.applicationFramework(true, true, true));
        assertEquals("microprofile", AdoptionDataCollector.applicationFramework(false, true, true));
        assertEquals("jakarta-ee", AdoptionDataCollector.applicationFramework(false, false, true));
        assertEquals("unknown", AdoptionDataCollector.applicationFramework(false, false, false));
    }

    @Test
    public void enabledFeaturesDoNotDecideTheFramework() {
        // inputs() enables features and deploys 3 applications, but nothing was found inside them
        AdoptionDataCollector.Inputs in = inputs();
        in.features = new HashSet<String>(Arrays.asList("springBoot-3.0", "microProfile-6.1", "jakartaee-10.0"));
        assertEquals("unknown", collector(plainHost()).collect(in).get("application_framework"));
    }

    @Test
    public void collectUsesWhatTheApplicationsContain() {
        AdoptionDataCollector.Inputs in = inputs();
        in.springBootDeployed = true;
        assertEquals("spring-boot", collector(plainHost()).collect(in).get("application_framework"));

        in = inputs();
        in.microProfileDeployed = true;
        assertEquals("microprofile", collector(plainHost()).collect(in).get("application_framework"));

        in = inputs();
        in.jakartaEeDeployed = true;
        assertEquals("jakarta-ee", collector(plainHost()).collect(in).get("application_framework"));
    }

    @Test
    public void instanceIdIsStableHexAndDependsOnInputs() {
        String a = AdoptionDataCollector.instanceId("/opt/wlp", "s1", "h1");
        assertEquals(a, AdoptionDataCollector.instanceId("/opt/wlp", "s1", "h1"));
        assertEquals(64, a.length());
        assertTrue(a.matches("[0-9a-f]{64}"));
        assertFalse(a.equals(AdoptionDataCollector.instanceId("/opt/wlp", "s2", "h1")));
        assertFalse(a.equals(AdoptionDataCollector.instanceId("/opt/wlp", "s1", "h2")));
    }

    @Test
    public void missingValuesBecomeUnknownNotNull() {
        Map<String, Object> d = collector(plainHost()).collect(new AdoptionDataCollector.Inputs());
        for (Map.Entry<String, Object> e : d.entrySet()) {
            assertTrue(e.getKey() + " must not be null", e.getValue() != null);
        }
        assertEquals("unknown", d.get("product_name"));
        assertEquals("unknown", d.get("customer_id"));
    }
}

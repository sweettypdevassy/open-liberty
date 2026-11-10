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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SwidTagTest {

    private Path dir;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("swid-test");
    }

    @After
    public void tearDown() {
        File[] kids = dir.toFile().listFiles();
        if (kids != null) {
            for (File k : kids) {
                k.delete();
            }
        }
        dir.toFile().delete();
    }

    private void tag(String file, String name, String id) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                     + "<SoftwareIdentity name=\"" + name + "\" tagId=\"ibm.com-" + id + "-26.0.0\" version=\"26.0.0\">\n"
                     + "    <Meta persistentId=\"" + id + "\"/>\n"
                     + "    <Entity name=\"IBM\" regid=\"ibm.com\"/>\n"
                     + "</SoftwareIdentity>\n";
        Files.write(dir.resolve(file), xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readsIdAndNameFromProductTag() throws IOException {
        tag("ibm.com_Open_Liberty-26.0.0.swidtag", "Open Liberty", "0ca5af184810448c851299b2c615ebb6");
        SwidTag t = SwidTag.find(dir.toFile());
        assertEquals("0ca5af184810448c851299b2c615ebb6", t.persistentId);
        assertEquals("Open Liberty", t.name);
    }

    @Test
    public void fixTagWithSameIdIsFine() throws IOException {
        tag("ibm.com_Open_Liberty-26.0.0.swidtag", "Open Liberty", "0ca5af184810448c851299b2c615ebb6");
        tag("ibm.com_Open_Liberty-26.0.0.9.swidtag", "Open Liberty", "0ca5af184810448c851299b2c615ebb6");
        assertEquals("0ca5af184810448c851299b2c615ebb6", SwidTag.find(dir.toFile()).persistentId);
    }

    @Test
    public void differentComponentsAreAmbiguous() throws IOException {
        tag("a.swidtag", "One", "0ca5af184810448c851299b2c615ebb6");
        tag("b.swidtag", "Two", "fbf6a96d49214c0abc6a3bc5da6e48cd");
        assertNull(SwidTag.find(dir.toFile()));
    }

    @Test
    public void missingDirectoryOrTagsGiveNull() {
        assertNull(SwidTag.find(new File(dir.toFile(), "nope")));
        assertNull(SwidTag.find(dir.toFile()));
    }
}

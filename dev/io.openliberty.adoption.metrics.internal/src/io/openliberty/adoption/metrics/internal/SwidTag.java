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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The component id and name that the installed product declares in its own
 * SWID tag (<code>lib/versions/tags/*.swidtag</code>). IBM License Metric Tool
 * matches adoption data to inventory by this id, so it has to come from the
 * same tag rather than from a copy of the value.
 */
public final class SwidTag {

    private static final Pattern PERSISTENT_ID = Pattern.compile("<Meta\\s+persistentId=\"([0-9a-fA-F]{32})\"");
    private static final Pattern NAME = Pattern.compile("<SoftwareIdentity\\b[^>]*?\\sname=\"([^\"]*)\"");

    public final String persistentId;
    public final String name;

    SwidTag(String persistentId, String name) {
        this.persistentId = persistentId;
        this.name = name;
    }

    /**
     * Reads the tags in the directory. Fix and release tags repeat the id of the
     * product tag, so several files are fine as long as they all agree.
     *
     * @return the tag, or null if there is none or the files name different components
     */
    public static SwidTag find(File tagsDir) {
        File[] files = tagsDir.listFiles();
        if (files == null) {
            return null;
        }
        Set<String> ids = new HashSet<String>();
        String id = null;
        String name = null;
        for (File f : files) {
            if (!f.isFile() || !f.getName().endsWith(".swidtag")) {
                continue;
            }
            String xml;
            try {
                xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                continue;
            }
            Matcher m = PERSISTENT_ID.matcher(xml);
            if (!m.find()) {
                continue;
            }
            String found = m.group(1).toLowerCase(Locale.ROOT);
            if (ids.add(found) && id == null) {
                id = found;
                Matcher n = NAME.matcher(xml);
                name = n.find() ? n.group(1) : null;
            }
        }
        return ids.size() == 1 ? new SwidTag(id, name) : null;
    }
}

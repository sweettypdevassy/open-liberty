/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.model;

import static com.ibm.ws.repository.transport.model.CopyUtils.copyCollection;

import java.util.Collection;

public class JavaSEVersionRequirements extends AbstractJSON {

    String minVersion;
    String maxVersion;
    String versionDisplayString;
    Collection<String> rawRequirements;

    public JavaSEVersionRequirements() {
    }

    /**
     * Copy constructor
     *
     * @param other the object to copy
     */
    public JavaSEVersionRequirements(JavaSEVersionRequirements other) {
        super();
        this.minVersion = other.minVersion;
        this.maxVersion = other.maxVersion;
        this.versionDisplayString = other.versionDisplayString;
        this.rawRequirements = copyCollection(other.rawRequirements);
    }

    public String getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    public String getMaxVersion() {
        return maxVersion;
    }

    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }

    public Collection<String> getRawRequirements() {
        return rawRequirements;
    }

    public void setRawRequirements(Collection<String> rawRequirements) {
        this.rawRequirements = rawRequirements;
    }

    public String getVersionDisplayString() {
        return versionDisplayString;
    }

    public void setVersionDisplayString(String versionDisplayString) {
        this.versionDisplayString = versionDisplayString;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JavaSEVersionRequirements other = (JavaSEVersionRequirements) obj;
        if (maxVersion == null) {
            if (other.maxVersion != null) {
                return false;
            }
        } else if (!maxVersion.equals(other.maxVersion)) {
            return false;
        }
        if (minVersion == null) {
            if (other.minVersion != null) {
                return false;
            }
        } else if (!minVersion.equals(other.minVersion)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((maxVersion == null) ? 0 : maxVersion.hashCode());
        result = prime * result + ((minVersion == null) ? 0 : minVersion.hashCode());
        return result;
    }

}

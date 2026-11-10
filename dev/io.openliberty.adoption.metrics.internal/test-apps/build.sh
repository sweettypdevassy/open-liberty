#!/bin/sh
#*******************************************************************************
# Copyright (c) 2026 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************
#
# Builds the three small test applications used to check application_framework:
#
#   out/jeeapp.war       Jakarta EE   (a servlet)            -> jakarta-ee
#   out/mpapp.war        MicroProfile (a health check)       -> microprofile
#   out/demo-0.0.1.jar   Spring Boot  (needs Maven + network) -> spring-boot
#
# Usage:  ./build.sh <wlp-dir>
#   <wlp-dir> is a built Liberty image, for example open-liberty/dev/build.image/wlp.
#   Its dev/api jars are used as the compile classpath, so no download is needed
#   for the two wars. Java 17 or later must be on the PATH.
#   The Spring Boot jar is skipped (with a message) when Maven is not installed.

set -e

if [ -z "$1" ] || [ ! -d "$1/dev/api" ]; then
  echo "Usage: $0 <wlp-dir>   (a built Liberty image that contains dev/api)" >&2
  exit 1
fi

HERE=$(cd "$(dirname "$0")" && pwd)
WLP=$(cd "$1" && pwd)
OUT="$HERE/out"
WORK="$HERE/out/work"

find_jar() {   # find_jar <dir> <pattern>
  ls "$WLP/dev/api/$1"/$2 2>/dev/null | head -1
}

SERVLET=$(find_jar spec 'io.openliberty.jakarta.servlet.6.0_*.jar')
RS=$(find_jar spec 'io.openliberty.jakarta.restfulWS.3.1_*.jar')
CDI=$(find_jar spec 'io.openliberty.jakarta.cdi.4.0_*.jar')
HEALTH=$(find_jar stable 'io.openliberty.org.eclipse.microprofile.health.4.0_*.jar')
[ -z "$HEALTH" ] && HEALTH=$(find_jar ibm 'io.openliberty.org.eclipse.microprofile.health.4.0_*.jar')

for j in "$SERVLET" "$RS" "$CDI" "$HEALTH"; do
  if [ -z "$j" ]; then
    echo "A required API jar was not found under $WLP/dev/api" >&2
    exit 1
  fi
done

rm -rf "$OUT"
mkdir -p "$WORK/jee/WEB-INF/classes" "$WORK/mp/WEB-INF/classes"

echo "Building jeeapp.war ..."
javac --release 17 -cp "$SERVLET" -d "$WORK/jee/WEB-INF/classes" "$HERE"/jee/src/*.java
jar --create --file "$OUT/jeeapp.war" -C "$WORK/jee" .

echo "Building mpapp.war ..."
javac --release 17 -cp "$RS:$CDI:$HEALTH" -d "$WORK/mp/WEB-INF/classes" "$HERE"/mp/src/*.java
jar --create --file "$OUT/mpapp.war" -C "$WORK/mp" .

if command -v mvn >/dev/null 2>&1; then
  echo "Building demo-0.0.1.jar (Spring Boot, downloads dependencies) ..."
  (cd "$HERE/spring" && mvn -q -DskipTests package)
  cp "$HERE/spring/target/demo-0.0.1.jar" "$OUT/"
else
  echo "Maven not found: skipping demo-0.0.1.jar. Install Maven or use any Spring Boot 3.x jar."
fi

rm -rf "$WORK"
echo "Done:"
ls -l "$OUT"

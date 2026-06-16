#!/usr/bin/env bash
# Build the Raspberry Pi runnable jar for ars-fx (solo J-Map / J-Sat).
#
# OpenJFX publishes no ARM-Linux jar to Maven, so this jar deliberately does NOT
# bundle JavaFX — it expects a Liberica Full JDK 21 on the Pi (which ships JavaFX).
# The desktop jar (mvn -q package -> ars-fx-linux.jar) DOES bundle JavaFX.
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -Ppi clean package -DskipTests

JAR="target/ars-fx-pi.jar"
echo
echo "built: $JAR  ($(du -h "$JAR" | cut -f1))"
echo
echo "On the Raspberry Pi (64-bit Pi OS):"
echo "  1. install a Liberica Full JDK 21 (includes JavaFX), e.g.:"
echo "       https://bell-sw.com/pages/downloads/  ->  JDK 21 / Full / ARM 64 / Linux"
echo "       (tarball, or:  sudo apt install bellsoft-java21-full   via the BellSoft apt repo)"
echo "  2. copy $JAR and a launch JSON (see launch/) to the Pi"
echo "  3. run:   java -jar ars-fx-pi.jar --config j-map-remote.json"

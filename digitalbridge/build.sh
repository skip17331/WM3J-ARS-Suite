#!/usr/bin/env bash
# Digital Bridge — Build script
# Part of the WM3j ARS Suite
# Requires: Java 21, Maven 3.x
set -e
cd "$(dirname "$0")"
echo "Building Digital Bridge with Maven..."
mvn clean package -DskipTests
echo ""
echo "Build complete: target/digital-bridge-1.0.0-shaded.jar"
echo "Run with: ./run.sh"

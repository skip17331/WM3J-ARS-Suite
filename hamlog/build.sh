#!/usr/bin/env bash
# HamLog — Build script
set -e
cd "$(dirname "$0")"
echo "Building HamLog with Maven..."
mvn clean package -DskipTests
echo ""
echo "Build complete: target/hamlog-1.0.0-shaded.jar"
echo "Run with: ./run.sh"

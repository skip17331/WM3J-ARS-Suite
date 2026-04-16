#!/usr/bin/env bash
# J-Log — Build script
set -e
cd "$(dirname "$0")"
echo "Building J-Log with Maven..."
mvn clean package -DskipTests
echo ""
echo "Build complete: target/j-log-1.0.0-shaded.jar"
echo "Run with: ./run.sh"

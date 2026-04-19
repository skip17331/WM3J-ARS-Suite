#!/usr/bin/env bash
# Build and install j-log-engine to local Maven repo.
# Run this before building j-log, j-digi, or j-bridge.
set -e
cd "$(dirname "$0")"
mvn clean install -DskipTests -q
echo "j-log-engine installed."

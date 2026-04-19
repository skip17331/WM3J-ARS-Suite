#!/bin/bash

# Directory of your project
PROJECT_DIR="/home/mike/ARS_Suite"

cd "$PROJECT_DIR" || exit 1

# Timestamp for commit message
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

# Check for changes
if git diff --quiet && git diff --cached --quiet; then
    echo "[$TIMESTAMP] No changes to commit."
    exit 0
fi

# Stage everything
git add -A

# Commit with timestamp
git commit -m "Auto-sync: $TIMESTAMP"

# Push to GitHub
git push origin main

# Log the sync
echo "[$TIMESTAMP] Synced to GitHub." >> "$PROJECT_DIR/git-sync.log"

echo "Sync complete at $TIMESTAMP"

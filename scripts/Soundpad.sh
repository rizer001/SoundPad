#!/bin/bash
cd "$(dirname "$0")"

# Use bundled JRE if present, otherwise system Java
if [ -f "runtime/bin/java" ]; then
    JAVA="runtime/bin/java"
else
    JAVA="java"
fi

echo "Starting Soundpad..."
"$JAVA" -Dfile.encoding=UTF-8 -jar Soundpad.jar

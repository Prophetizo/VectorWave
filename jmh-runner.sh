#!/bin/bash

# JMH Runner with proper classpath setup

echo "JMH Benchmark Runner"
echo "===================="
echo ""

# Default benchmark class if none specified
BENCHMARK_CLASS=${1:-".*"}
shift

# Compile first
echo "Compiling..."
mvn compile test-compile -q || exit 1

# Create a temporary file for the classpath
CP_FILE=$(mktemp)

# Get the test classpath from Maven
mvn dependency:build-classpath -Dmdep.outputFile="$CP_FILE" -DincludeScope=test -q

# Read the classpath
CP=$(cat "$CP_FILE")
rm "$CP_FILE"

# Add compiled classes
FULL_CP="target/test-classes:target/classes:$CP"

echo "Running benchmark: $BENCHMARK_CLASS"
echo ""

# Run JMH
java -cp "$FULL_CP" \
    org.openjdk.jmh.Main "$BENCHMARK_CLASS" "$@"
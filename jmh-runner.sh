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

# Check if Vector API module is available
VECTOR_MODULE=""
if java --list-modules 2>/dev/null | grep -q "jdk.incubator.vector"; then
    VECTOR_MODULE="--add-modules=jdk.incubator.vector"
    echo "Vector API module detected and will be enabled"
else
    echo "Vector API module not available - benchmarks will use scalar fallback"
fi

# Set default JVM options if not provided
DEFAULT_JAVA_OPTS="-Xmx1G $VECTOR_MODULE"

# Use custom JAVA_OPTS if provided, otherwise use defaults
JVM_OPTS="${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}"

echo "JVM Options: $JVM_OPTS"
echo ""

# Run JMH
java -cp "$FULL_CP" $JVM_OPTS \
    org.openjdk.jmh.Main "$BENCHMARK_CLASS" "$@"
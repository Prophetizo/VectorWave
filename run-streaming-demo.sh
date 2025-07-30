#!/bin/bash

# VectorWave Streaming Demo Runner with proper JVM flags

# JVM flags for Vector API
JVM_FLAGS="--add-modules=jdk.incubator.vector"

echo "VectorWave Streaming Financial Analysis Demo"
echo "==========================================="
echo ""

# Check if compiled
if [ ! -d "target/classes" ]; then
    echo "Compiling project..."
    mvn compile -q
fi

# Run the demo based on argument
case "${1:-simple}" in
    "simple")
        echo "Running Simple Streaming Analysis Demo..."
        echo "Press Ctrl+C to stop"
        echo ""
        java $JVM_FLAGS -cp target/classes ai.prophetizo.demo.StreamingFinancialDemo
        ;;
    "incremental")
        echo "Running Incremental Analysis Demo..."
        echo ""
        java $JVM_FLAGS -cp target/classes ai.prophetizo.demo.StreamingFinancialDemo --incremental
        ;;
    "compare")
        echo "Running Performance Comparison Demo..."
        echo ""
        java $JVM_FLAGS -cp target/classes ai.prophetizo.demo.StreamingFinancialDemo --compare
        ;;
    "trading")
        echo "Running Live Trading Simulation..."
        echo "Watch the automated trading bot in action!"
        echo ""
        java $JVM_FLAGS -cp target/classes ai.prophetizo.demo.LiveTradingSimulation
        ;;
    *)
        echo "Usage: $0 [simple|incremental|compare|trading]"
        echo ""
        echo "Options:"
        echo "  simple      - Real-time streaming dashboard (default)"
        echo "  incremental - Detailed incremental analysis with EMAs"
        echo "  compare     - Performance comparison vs batch processing"
        echo "  trading     - Interactive live trading simulation"
        exit 1
        ;;
esac
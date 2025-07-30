#!/bin/bash

# VectorWave Streaming Financial Analysis Demo Runner

echo "VectorWave Streaming Demo"
echo "========================"
echo ""
echo "Select a demo to run:"
echo "1) Simple Streaming Analysis (real-time dashboard)"
echo "2) Incremental Analysis (detailed metrics)"
echo "3) Performance Comparison (streaming vs batch)"
echo "4) Live Trading Simulation (interactive trading bot)"
echo ""
read -p "Enter your choice (1-4): " choice

# Compile if needed
if [ ! -d "target/classes" ]; then
    echo "Compiling project..."
    mvn compile -q
fi

case $choice in
    1)
        echo "Starting Simple Streaming Analysis..."
        java -cp target/classes ai.prophetizo.demo.StreamingFinancialDemo
        ;;
    2)
        echo "Starting Incremental Analysis Demo..."
        java -cp target/classes ai.prophetizo.demo.StreamingFinancialDemo --incremental
        ;;
    3)
        echo "Starting Performance Comparison..."
        java -cp target/classes ai.prophetizo.demo.StreamingFinancialDemo --compare
        ;;
    4)
        echo "Starting Live Trading Simulation..."
        java -cp target/classes ai.prophetizo.demo.LiveTradingSimulation
        ;;
    *)
        echo "Invalid choice. Please run again and select 1-4."
        exit 1
        ;;
esac
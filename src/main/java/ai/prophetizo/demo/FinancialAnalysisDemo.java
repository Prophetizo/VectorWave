package ai.prophetizo.demo;

import ai.prophetizo.financial.FinancialAnalysisConfig;
import ai.prophetizo.financial.FinancialAnalyzer;

import java.util.Arrays;
import java.util.Random;

/**
 * Demonstrates the financial analysis capabilities with configurable thresholds.
 */
public class FinancialAnalysisDemo {
    
    // Use a fixed seed for reproducible demo results
    private static final Random RANDOM = new Random(42);
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Financial Analysis Demo");
        System.out.println("====================================");
        System.out.println();
        
        // Demonstrate different configurations
        demonstrateDefaultConfiguration();
        demonstrateCustomConfiguration();
        demonstrateMarketScenarios();
    }
    
    private static void demonstrateDefaultConfiguration() {
        System.out.println("1. DEFAULT CONFIGURATION");
        System.out.println("========================");
        
        FinancialAnalyzer analyzer = FinancialAnalyzer.withDefaultConfig();
        FinancialAnalysisConfig config = analyzer.getConfig();
        
        System.out.println("Default thresholds:");
        System.out.printf("   Crash Asymmetry Threshold:     %.2f%n", config.getCrashAsymmetryThreshold());
        System.out.printf("   Volatility Low Threshold:      %.2f%n", config.getVolatilityLowThreshold());
        System.out.printf("   Volatility High Threshold:     %.2f%n", config.getVolatilityHighThreshold());
        System.out.printf("   Regime Trend Threshold:        %.3f%n", config.getRegimeTrendThreshold());
        System.out.printf("   Anomaly Detection Threshold:   %.2f%n", config.getAnomalyDetectionThreshold());
        System.out.printf("   Window Size:                   %d%n", config.getWindowSize());
        System.out.printf("   Confidence Level:              %.2f%n", config.getConfidenceLevel());
        System.out.println();
        
        // Test with stable market data
        double[] stablePrices = createStableMarketData();
        System.out.println("Analyzing stable market data:");
        System.out.println("   Sample prices: " + Arrays.toString(Arrays.copyOf(stablePrices, 8)) + "...");
        analyzeAndReport(analyzer, stablePrices, "Stable Market");
        System.out.println();
    }
    
    private static void demonstrateCustomConfiguration() {
        System.out.println("2. CUSTOM CONFIGURATION FOR HIGH-FREQUENCY TRADING");
        System.out.println("===================================================");
        
        // Create configuration optimized for high-frequency trading
        FinancialAnalysisConfig hftConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(5.0)       // Lower threshold for more sensitive crash detection
                .volatilityLowThreshold(0.1)        // Lower threshold for HFT volatility ranges
                .volatilityHighThreshold(0.8)       // Lower threshold for HFT
                .regimeTrendThreshold(0.005)        // More sensitive to regime changes
                .anomalyDetectionThreshold(2.5)     // More aggressive anomaly detection
                .windowSize(64)                     // Smaller window for faster analysis
                .confidenceLevel(0.99)              // Higher confidence required
                .build();
        
        FinancialAnalyzer hftAnalyzer = new FinancialAnalyzer(hftConfig);
        
        System.out.println("HFT-optimized thresholds:");
        System.out.printf("   Crash Asymmetry Threshold:     %.2f%n", hftConfig.getCrashAsymmetryThreshold());
        System.out.printf("   Volatility Low Threshold:      %.2f%n", hftConfig.getVolatilityLowThreshold());
        System.out.printf("   Volatility High Threshold:     %.2f%n", hftConfig.getVolatilityHighThreshold());
        System.out.printf("   Regime Trend Threshold:        %.3f%n", hftConfig.getRegimeTrendThreshold());
        System.out.printf("   Window Size:                   %d%n", hftConfig.getWindowSize());
        System.out.println();
        
        // Test with volatile market data
        double[] volatilePrices = createVolatileMarketData();
        System.out.println("Analyzing volatile market data with HFT config:");
        System.out.println("   Sample prices: " + Arrays.toString(Arrays.copyOf(volatilePrices, 8)) + "...");
        analyzeAndReport(hftAnalyzer, volatilePrices, "Volatile Market (HFT Config)");
        System.out.println();
    }
    
    private static void demonstrateMarketScenarios() {
        System.out.println("3. DIFFERENT MARKET SCENARIOS");
        System.out.println("==============================");
        
        FinancialAnalyzer analyzer = FinancialAnalyzer.withDefaultConfig();
        
        // Market crash scenario
        double[] crashPrices = createMarketCrashData();
        System.out.println("Market Crash Scenario:");
        System.out.println("   Sample prices: " + Arrays.toString(Arrays.copyOf(crashPrices, 8)) + "...");
        analyzeAndReport(analyzer, crashPrices, "Market Crash");
        System.out.println();
        
        // Bull market scenario
        double[] bullPrices = createBullMarketData();
        System.out.println("Bull Market Scenario:");
        System.out.println("   Sample prices: " + Arrays.toString(Arrays.copyOf(bullPrices, 8)) + "...");
        analyzeAndReport(analyzer, bullPrices, "Bull Market");
        System.out.println();
        
        // Regime shift scenario
        double[] regimePrices = createRegimeShiftData();
        System.out.println("Regime Shift Scenario:");
        System.out.println("   Sample prices: " + Arrays.toString(Arrays.copyOf(regimePrices, 8)) + "...");
        analyzeAndReport(analyzer, regimePrices, "Regime Shift");
        System.out.println();
    }
    
    private static void analyzeAndReport(FinancialAnalyzer analyzer, double[] prices, String scenario) {
        try {
            // Perform analysis
            double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
            double volatility = analyzer.analyzeVolatility(prices);
            double trendChange = analyzer.analyzeRegimeTrend(prices);
            boolean hasAnomalies = analyzer.detectAnomalies(prices);
            
            // Classifications and risk assessments
            FinancialAnalyzer.VolatilityClassification volClass = analyzer.classifyVolatility(volatility);
            boolean crashRisk = analyzer.isCrashRisk(asymmetry);
            boolean regimeShift = analyzer.isRegimeShift(trendChange);
            
            // Report results
            System.out.printf("   Results for %s:%n", scenario);
            System.out.printf("     Crash Asymmetry:     %.4f %s%n", asymmetry, 
                    crashRisk ? "(⚠️  CRASH RISK)" : "(✅ Normal)");
            System.out.printf("     Volatility:          %.4f (%s)%n", volatility, volClass);
            System.out.printf("     Regime Trend Change: %.4f %s%n", trendChange, 
                    regimeShift ? "(⚠️  REGIME SHIFT)" : "(✅ Stable)");
            System.out.printf("     Anomalies Detected:  %s%n", 
                    hasAnomalies ? "⚠️  YES" : "✅ None");
            
        } catch (Exception e) {
            System.out.printf("   Error analyzing %s: %s%n", scenario, e.getMessage());
        }
    }
    
    // Test data generators
    
    private static double[] createStableMarketData() {
        double[] prices = new double[64];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            // Small random fluctuations around the base price
            double change = (RANDOM.nextDouble() - 0.5) * 0.005; // ±0.25% changes
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
    
    private static double[] createVolatileMarketData() {
        double[] prices = new double[64];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            // Larger random fluctuations
            double change = (RANDOM.nextDouble() - 0.5) * 0.04; // ±2% changes
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
    
    private static double[] createMarketCrashData() {
        double[] prices = new double[64];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            if (i < 30) {
                // Normal market before crash
                double change = (RANDOM.nextDouble() - 0.5) * 0.01;
                prices[i] = prices[i-1] * (1 + change);
            } else {
                // Market crash - sharp downward movements with high volatility
                double change = -0.02 + (RANDOM.nextDouble() - 0.8) * 0.03; // Biased downward
                prices[i] = prices[i-1] * (1 + change);
            }
        }
        
        return prices;
    }
    
    private static double[] createBullMarketData() {
        double[] prices = new double[64];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            // Consistent upward trend with occasional small dips
            double trend = 0.005; // 0.5% average growth
            double noise = (RANDOM.nextDouble() - 0.5) * 0.015; // ±0.75% noise
            double change = trend + noise;
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
    
    private static double[] createRegimeShiftData() {
        double[] prices = new double[64];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            if (i < 32) {
                // First regime: low volatility, slight upward trend
                double change = 0.001 + (RANDOM.nextDouble() - 0.5) * 0.005;
                prices[i] = prices[i-1] * (1 + change);
            } else {
                // Second regime: higher volatility, different trend
                double change = -0.002 + (RANDOM.nextDouble() - 0.5) * 0.025;
                prices[i] = prices[i-1] * (1 + change);
            }
        }
        
        return prices;
    }
}
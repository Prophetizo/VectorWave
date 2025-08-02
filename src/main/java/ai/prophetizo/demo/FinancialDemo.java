package ai.prophetizo.demo;

import ai.prophetizo.financial.FinancialConfig;
import ai.prophetizo.financial.FinancialWaveletAnalyzer;

import java.util.Arrays;

/**
 * Demonstration of financial analysis using wavelets.
 * Shows Sharpe ratio calculation with configurable risk-free rates.
 */
public class FinancialDemo {
    public static void main(String[] args) {
        System.out.println("VectorWave - Financial Analysis Demo");
        System.out.println("====================================");
        
        // Sample monthly returns data (e.g., stock returns)
        double[] monthlyReturns = {
            0.05, 0.02, -0.01, 0.08, 0.04, 0.06, -0.02, 0.09
        }; // 8 data points (power of 2 for wavelet transform)
        
        System.out.println("Monthly Returns: " + Arrays.toString(monthlyReturns));
        System.out.println();
        
        demonstrateDefaultConfiguration(monthlyReturns);
        demonstrateCustomRiskFreeRates(monthlyReturns);
        demonstrateWaveletDenoising(monthlyReturns);
    }
    
    private static void demonstrateDefaultConfiguration(double[] returns) {
        System.out.println("1. STANDARD CONFIGURATION");
        System.out.println("==========================");
        
        // Create configuration with typical risk-free rate (e.g., 3% annual)
        FinancialConfig config = new FinancialConfig(0.03);
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        System.out.println("Using risk-free rate: " + 
                         String.format("%.3f%%", analyzer.getConfig().getRiskFreeRate() * 100));
        
        double sharpeRatio = analyzer.calculateSharpeRatio(returns);
        System.out.println("Sharpe Ratio: " + String.format("%.4f", sharpeRatio));
        System.out.println();
    }
    
    private static void demonstrateCustomRiskFreeRates(double[] returns) {
        System.out.println("2. VARIOUS RISK-FREE RATES");
        System.out.println("===========================");
        
        double[] riskFreeRates = {0.0, 0.02, 0.03, 0.05, 0.08};
        
        for (double rate : riskFreeRates) {
            FinancialConfig config = new FinancialConfig(rate);
            FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
            
            double sharpeRatio = analyzer.calculateSharpeRatio(returns);
            System.out.println(String.format("Risk-free rate: %5.1f%% → Sharpe Ratio: %7.4f", 
                             rate * 100, sharpeRatio));
        }
        System.out.println();
    }
    
    private static void demonstrateWaveletDenoising(double[] returns) {
        System.out.println("3. WAVELET-BASED DENOISING");
        System.out.println("===========================");
        
        // Define a separate array of noisy returns to demonstrate denoising
        double[] noisyReturns = {
            0.052, 0.018, -0.008, 0.084, 0.037, 0.063, -0.025, 0.091
        };
        
        System.out.println("Original Returns: " + Arrays.toString(returns));
        System.out.println("Noisy Returns:    " + Arrays.toString(noisyReturns));
        
        // Create analyzer with a typical risk-free rate
        FinancialConfig config = new FinancialConfig(0.03); // 3% annual
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        double regularSharpe = analyzer.calculateSharpeRatio(noisyReturns);
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(noisyReturns);
        
        System.out.println();
        System.out.println("Regular Sharpe Ratio (with noise):  " + String.format("%.4f", regularSharpe));
        System.out.println("Wavelet Sharpe Ratio (denoised):    " + String.format("%.4f", waveletSharpe));
        
        double improvement = Math.abs(waveletSharpe - regularSharpe);
        System.out.println("Absolute difference:                " + String.format("%.4f", improvement));
    }
}
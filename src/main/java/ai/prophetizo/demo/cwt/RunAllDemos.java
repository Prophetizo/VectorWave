package ai.prophetizo.demo.cwt;

/**
 * Convenience class to run all CWT demos in sequence.
 * 
 * <p>This provides an easy way to see all CWT capabilities demonstrated
 * in a single run, with clear separation between different demo sections.</p>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>
 * java --add-modules jdk.incubator.vector -cp target/classes ai.prophetizo.demo.cwt.RunAllDemos
 * </pre>
 */
public class RunAllDemos {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                    VectorWave CWT Comprehensive Demo Suite                    ");
        System.out.println("================================================================================");
        System.out.println();
        
        try {
            // Demo 1: CWT Basics
            System.out.println("🚀 Running CWT Basics Demo...");
            System.out.println("================================================================================");
            CWTBasicsDemo.main(args);
            waitForUser();
            
            // Demo 2: Financial Wavelets
            System.out.println("💰 Running Financial Wavelets Demo...");
            System.out.println("================================================================================");
            FinancialWaveletsDemo.main(args);
            waitForUser();
            
            // Demo 3: Gaussian Derivative Wavelets
            System.out.println("🔍 Running Gaussian Derivative Demo...");
            System.out.println("================================================================================");
            GaussianDerivativeDemo.main(args);
            waitForUser();
            
            // Demo 4: Performance Analysis
            System.out.println("⚡ Running Performance Demo...");
            System.out.println("================================================================================");
            CWTPerformanceDemo.main(args);
            waitForUser();
            
            // Demo 5: Comprehensive Showcase
            System.out.println("🎯 Running Comprehensive Demo...");
            System.out.println("================================================================================");
            ComprehensiveCWTDemo.main(args);
            
            System.out.println();
            System.out.println("================================================================================");
            System.out.println("                           Demo Suite Completed!                              ");
            System.out.println("================================================================================");
            System.out.println();
            System.out.println("🎉 All CWT demos completed successfully!");
            System.out.println();
            System.out.println("Key Features Demonstrated:");
            System.out.println("✅ FFT-accelerated CWT with O(n log n) complexity");
            System.out.println("✅ Financial wavelets for market analysis");
            System.out.println("✅ Gaussian derivative wavelets for feature detection");
            System.out.println("✅ Platform-adaptive cache optimization");
            System.out.println("✅ Multiple wavelet configurations and use cases");
            System.out.println();
            System.out.println("Next Steps:");
            System.out.println("- Run individual demos for focused exploration");
            System.out.println("- Modify signal generation for your use cases");
            System.out.println("- Experiment with different wavelet parameters");
            System.out.println("- Try the streaming CWT capabilities");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Error running demos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void waitForUser() {
        System.out.println();
        System.out.println("Press Enter to continue to the next demo...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
        System.out.println();
    }
}
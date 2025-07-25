package ai.prophetizo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.exception.WaveletTransformException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates comprehensive error handling best practices for the VectorWave library.
 * This demo shows various patterns for handling errors gracefully, validating inputs,
 * implementing recovery strategies, and providing meaningful feedback to users.
 */
public class ErrorHandlingDemo {
    
    private static final Logger logger = Logger.getLogger(ErrorHandlingDemo.class.getName());
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Error Handling Best Practices Demo");
        System.out.println("===============================================");
        System.out.println();
        
        ErrorHandlingDemo demo = new ErrorHandlingDemo();
        
        // Demonstrate different error handling patterns
        demo.demonstrateInputValidation();
        demo.demonstrateExceptionClassification();
        demo.demonstrateRecoveryStrategies();
        demo.demonstrateBatchProcessingWithErrors();
        demo.demonstrateProperErrorReporting();
        demo.demonstrateConfigurationValidation();
    }
    
    /**
     * Demonstrates input validation patterns and early failure detection.
     * Shows how to validate signals before attempting transforms.
     */
    private void demonstrateInputValidation() {
        System.out.println("1. INPUT VALIDATION PATTERNS");
        System.out.println("============================");
        
        // Test various invalid inputs
        double[][] testSignals = {
            null,                                    // Null signal
            {},                                      // Empty signal
            {1.0, 2.0, 3.0},                        // Not power of 2
            {1.0, 2.0, Double.NaN, 4.0},           // Contains NaN
            {1.0, 2.0, Double.POSITIVE_INFINITY, 4.0}, // Contains infinity
            {1.0, 2.0, 3.0, 4.0}                   // Valid signal
        };
        
        String[] testDescriptions = {
            "Null signal",
            "Empty signal", 
            "Non-power-of-2 length (3)",
            "Signal with NaN values",
            "Signal with infinity values",
            "Valid signal"
        };
        
        for (int i = 0; i < testSignals.length; i++) {
            System.out.printf("Testing: %s%n", testDescriptions[i]);
            ValidationResult result = validateSignal(testSignals[i]);
            
            if (result.isValid()) {
                System.out.println("   ✓ Signal is valid");
            } else {
                System.out.printf("   ✗ Validation failed: %s%n", result.getErrorMessage());
                if (result.getSuggestion() != null) {
                    System.out.printf("   💡 Suggestion: %s%n", result.getSuggestion());
                }
            }
            System.out.println();
        }
    }
    
    /**
     * Demonstrates how to classify and handle different types of exceptions appropriately.
     */
    private void demonstrateExceptionClassification() {
        System.out.println("2. EXCEPTION CLASSIFICATION AND HANDLING");
        System.out.println("========================================");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        // Test cases that will generate different types of exceptions
        Object[][] testCases = {
            {null, new Haar(), "Null signal"},
            {new double[]{}, new Haar(), "Empty signal"},
            {new double[]{1, 2, 3}, new Haar(), "Non-power-of-2 signal"},
            {new double[]{1, 2, Double.NaN, 4}, new Haar(), "Signal with NaN"},
            {new double[]{1, 2, 3, 4}, new Haar(), "Valid case"}
        };
        
        for (Object[] testCase : testCases) {
            double[] signal = (double[]) testCase[0];
            Wavelet wavelet = (Wavelet) testCase[1];
            String description = (String) testCase[2];
            
            System.out.printf("Testing: %s%n", description);
            classifyAndHandleException(signal, factory.create(wavelet));
            System.out.println();
        }
    }
    
    /**
     * Demonstrates recovery strategies for handling errors gracefully.
     */
    private void demonstrateRecoveryStrategies() {
        System.out.println("3. RECOVERY STRATEGIES");
        System.out.println("======================");
        
        // Test signal that's not power of 2
        double[] originalSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        System.out.println("Original signal: " + Arrays.toString(originalSignal));
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(new Haar());
        
        // Strategy 1: Pad to next power of 2
        System.out.println("\nStrategy 1: Pad signal to next power of 2");
        try {
            double[] paddedSignal = padToNextPowerOfTwo(originalSignal);
            System.out.println("Padded signal: " + Arrays.toString(paddedSignal));
            
            TransformResult result = transform.forward(paddedSignal);
            System.out.println("✓ Transform successful after padding");
            
        } catch (Exception e) {
            System.out.println("✗ Padding strategy failed: " + e.getMessage());
        }
        
        // Strategy 2: Truncate to largest power of 2
        System.out.println("\nStrategy 2: Truncate signal to largest power of 2");
        try {
            double[] truncatedSignal = truncateToLargestPowerOfTwo(originalSignal);
            System.out.println("Truncated signal: " + Arrays.toString(truncatedSignal));
            
            TransformResult result = transform.forward(truncatedSignal);
            System.out.println("✓ Transform successful after truncation");
            
        } catch (Exception e) {
            System.out.println("✗ Truncation strategy failed: " + e.getMessage());
        }
        
        // Strategy 3: Fallback to different wavelet or boundary mode
        System.out.println("\nStrategy 3: Try different wavelet types");
        Wavelet[] fallbackWavelets = {new Haar(), Daubechies.DB2, Symlet.SYM2};
        boolean success = false;
        
        for (Wavelet wavelet : fallbackWavelets) {
            try {
                // Use padded signal for this demo
                double[] paddedSignal = padToNextPowerOfTwo(originalSignal);
                WaveletTransform fallbackTransform = factory.create(wavelet);
                TransformResult result = fallbackTransform.forward(paddedSignal);
                
                System.out.printf("✓ Transform successful with %s wavelet%n", wavelet.name());
                success = true;
                break;
                
            } catch (Exception e) {
                System.out.printf("✗ Failed with %s wavelet: %s%n", wavelet.name(), e.getMessage());
            }
        }
        
        if (!success) {
            System.out.println("All fallback strategies failed");
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates batch processing with partial failure handling.
     */
    private void demonstrateBatchProcessingWithErrors() {
        System.out.println("4. BATCH PROCESSING WITH ERROR HANDLING");
        System.out.println("=======================================");
        
        // Create a batch of signals with various issues
        double[][] signalBatch = {
            {1.0, 2.0, 3.0, 4.0},           // Valid
            {1.0, 2.0, 3.0},                // Invalid length
            {1.0, 2.0, Double.NaN, 4.0},    // Contains NaN
            {5.0, 6.0, 7.0, 8.0},           // Valid
            null,                            // Null signal
            {9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0} // Valid, larger
        };
        
        String[] signalNames = {"Signal1", "Signal2", "Signal3", "Signal4", "Signal5", "Signal6"};
        
        BatchProcessingResult result = processBatch(signalBatch, signalNames, new Haar());
        
        System.out.printf("Batch processing complete:%n");
        System.out.printf("  Successful: %d/%d%n", result.getSuccessCount(), result.getTotalCount());
        System.out.printf("  Failed: %d/%d%n", result.getFailureCount(), result.getTotalCount());
        
        if (!result.getFailures().isEmpty()) {
            System.out.println("\nFailure details:");
            for (BatchFailure failure : result.getFailures()) {
                System.out.printf("  - %s: %s%n", failure.getSignalName(), failure.getErrorMessage());
            }
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates proper error reporting and logging practices.
     */
    private void demonstrateProperErrorReporting() {
        System.out.println("5. PROPER ERROR REPORTING AND LOGGING");
        System.out.println("=====================================");
        
        // Configure logging level for demonstration
        logger.setLevel(Level.ALL);
        
        double[] invalidSignal = {1.0, 2.0, 3.0}; // Not power of 2
        
        // Demonstrate different logging levels
        System.out.println("Demonstrating logging levels:");
        
        try {
            // Log the attempt
            logger.info("Attempting wavelet transform on signal with length: " + invalidSignal.length);
            
            WaveletTransform transform = new WaveletTransformFactory()
                    .withBoundaryMode(BoundaryMode.PERIODIC)
                    .create(new Haar());
            
            TransformResult result = transform.forward(invalidSignal);
            
        } catch (InvalidSignalException e) {
            // Log at appropriate levels with context
            logger.warning("Signal validation failed: " + e.getMessage());
            logger.fine("Signal contents: " + Arrays.toString(invalidSignal));
            
            // Provide structured error information
            ErrorInfo errorInfo = new ErrorInfo(
                "INVALID_SIGNAL_LENGTH",
                "Signal length must be a power of 2",
                "Consider padding the signal to length 4 or truncating to length 2",
                e.getMessage()
            );
            
            System.out.println("Structured error information:");
            System.out.println("  Error Code: " + errorInfo.getErrorCode());
            System.out.println("  User Message: " + errorInfo.getUserMessage());
            System.out.println("  Suggestion: " + errorInfo.getSuggestion());
            System.out.println("  Technical Details: " + errorInfo.getTechnicalDetails());
            
        } catch (WaveletTransformException e) {
            logger.severe("Unexpected wavelet transform error: " + e.getMessage());
            // Could also log stack trace for debugging
            logger.log(Level.FINE, "Full exception details", e);
            
        } catch (Exception e) {
            // Log unexpected errors at severe level
            logger.severe("Unexpected error during transform: " + e.getMessage());
            logger.log(Level.SEVERE, "Unexpected exception", e);
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates validation of wavelet and boundary mode configurations.
     */
    private void demonstrateConfigurationValidation() {
        System.out.println("6. CONFIGURATION VALIDATION");
        System.out.println("===========================");
        
        double[] testSignal = {1.0, 2.0, 3.0, 4.0};
        
        // Test different configuration combinations
        Wavelet[] wavelets = {new Haar(), Daubechies.DB4, new MorletWavelet()};
        BoundaryMode[] boundaryModes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING};
        
        for (Wavelet wavelet : wavelets) {
            for (BoundaryMode mode : boundaryModes) {
                System.out.printf("Testing: %s with %s boundary mode%n", 
                    wavelet.name(), mode.name());
                
                try {
                    WaveletTransform transform = new WaveletTransformFactory()
                            .withBoundaryMode(mode)
                            .create(wavelet);
                    
                    TransformResult result = transform.forward(testSignal);
                    double[] reconstructed = transform.inverse(result);
                    
                    // Calculate reconstruction error
                    double maxError = 0;
                    for (int i = 0; i < testSignal.length; i++) {
                        maxError = Math.max(maxError, Math.abs(testSignal[i] - reconstructed[i]));
                    }
                    
                    if (maxError < 1e-10) {
                        System.out.println("  ✓ Configuration valid - perfect reconstruction");
                    } else if (maxError < 1e-6) {
                        System.out.printf("  ✓ Configuration valid - good reconstruction (error: %.2e)%n", maxError);
                    } else {
                        System.out.printf("  ⚠ Configuration works but high reconstruction error: %.2e%n", maxError);
                    }
                    
                } catch (Exception e) {
                    System.out.printf("  ✗ Configuration failed: %s%n", e.getMessage());
                }
                
                System.out.println();
            }
        }
    }
    
    // Helper methods and classes
    
    /**
     * Validates a signal and returns detailed validation results.
     */
    private ValidationResult validateSignal(double[] signal) {
        try {
            // Check for null
            if (signal == null) {
                return ValidationResult.invalid("Signal cannot be null", 
                    "Provide a non-null double array");
            }
            
            // Check for empty
            if (signal.length == 0) {
                return ValidationResult.invalid("Signal cannot be empty", 
                    "Provide at least 2 samples");
            }
            
            // Check for power of 2 length
            if (!isPowerOfTwo(signal.length)) {
                int nextPower = nextPowerOfTwo(signal.length);
                return ValidationResult.invalid(
                    String.format("Signal length (%d) must be a power of 2", signal.length),
                    String.format("Consider padding to length %d or truncating to length %d", 
                        nextPower, largestPowerOfTwoLessOrEqual(signal.length))
                );
            }
            
            // Check for NaN or infinity values
            for (int i = 0; i < signal.length; i++) {
                if (Double.isNaN(signal[i])) {
                    return ValidationResult.invalid(
                        String.format("Signal contains NaN at index %d", i),
                        "Replace NaN values with valid numbers or interpolate"
                    );
                }
                if (Double.isInfinite(signal[i])) {
                    return ValidationResult.invalid(
                        String.format("Signal contains infinity at index %d", i),
                        "Replace infinite values with finite numbers"
                    );
                }
            }
            
            return ValidationResult.valid();
            
        } catch (Exception e) {
            return ValidationResult.invalid("Validation error: " + e.getMessage(), 
                "Check signal format and try again");
        }
    }
    
    /**
     * Classifies and handles exceptions with appropriate strategies.
     */
    private void classifyAndHandleException(double[] signal, WaveletTransform transform) {
        try {
            TransformResult result = transform.forward(signal);
            System.out.println("   ✓ Transform completed successfully");
            
        } catch (InvalidSignalException e) {
            System.out.println("   ✗ Invalid Signal Error: " + e.getMessage());
            System.out.println("   → This is a recoverable error - signal can be preprocessed");
            
        } catch (InvalidArgumentException e) {
            System.out.println("   ✗ Invalid Argument Error: " + e.getMessage());
            System.out.println("   → This is a programming error - check method parameters");
            
        } catch (WaveletTransformException e) {
            System.out.println("   ✗ Wavelet Transform Error: " + e.getMessage());
            System.out.println("   → This may indicate a configuration or algorithm issue");
            
        } catch (Exception e) {
            System.out.println("   ✗ Unexpected Error: " + e.getMessage());
            System.out.println("   → This is an unhandled error - may need investigation");
        }
    }
    
    /**
     * Processes a batch of signals with comprehensive error handling.
     */
    private BatchProcessingResult processBatch(double[][] signals, String[] names, Wavelet wavelet) {
        List<BatchFailure> failures = new ArrayList<>();
        int successCount = 0;
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(wavelet);
        
        for (int i = 0; i < signals.length; i++) {
            String signalName = (i < names.length) ? names[i] : "Signal" + (i + 1);
            
            try {
                double[] signal = signals[i];
                
                // Attempt preprocessing if signal is invalid
                ValidationResult validation = validateSignal(signal);
                if (!validation.isValid()) {
                    // Try to recover by padding
                    if (signal != null && signal.length > 0 && !isPowerOfTwo(signal.length)) {
                        signal = padToNextPowerOfTwo(signal);
                        System.out.printf("   Auto-padded %s to length %d%n", signalName, signal.length);
                    } else {
                        throw new InvalidSignalException(validation.getErrorMessage());
                    }
                }
                
                TransformResult result = transform.forward(signal);
                successCount++;
                System.out.printf("   ✓ %s processed successfully%n", signalName);
                
            } catch (Exception e) {
                failures.add(new BatchFailure(signalName, e.getMessage(), e.getClass().getSimpleName()));
                System.out.printf("   ✗ %s failed: %s%n", signalName, e.getMessage());
            }
        }
        
        return new BatchProcessingResult(signals.length, successCount, failures);
    }
    
    // Utility methods
    
    private double[] padToNextPowerOfTwo(double[] signal) {
        if (signal == null) throw new IllegalArgumentException("Signal cannot be null");
        
        int nextPower = nextPowerOfTwo(signal.length);
        double[] padded = new double[nextPower];
        System.arraycopy(signal, 0, padded, 0, signal.length);
        // Remaining elements are automatically initialized to 0.0
        return padded;
    }
    
    private double[] truncateToLargestPowerOfTwo(double[] signal) {
        if (signal == null) throw new IllegalArgumentException("Signal cannot be null");
        
        int largestPower = largestPowerOfTwoLessOrEqual(signal.length);
        if (largestPower == 0) largestPower = 1; // Minimum size
        
        double[] truncated = new double[largestPower];
        System.arraycopy(signal, 0, truncated, 0, largestPower);
        return truncated;
    }
    
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    private int nextPowerOfTwo(int n) {
        if (n <= 1) return 2;
        return Integer.highestOneBit(n - 1) << 1;
    }
    
    private int largestPowerOfTwoLessOrEqual(int n) {
        if (n <= 0) return 0;
        return Integer.highestOneBit(n);
    }
    
    // Helper classes for structured error handling
    
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String suggestion;
        
        private ValidationResult(boolean valid, String errorMessage, String suggestion) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.suggestion = suggestion;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }
        
        public static ValidationResult invalid(String errorMessage, String suggestion) {
            return new ValidationResult(false, errorMessage, suggestion);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getSuggestion() { return suggestion; }
    }
    
    private static class ErrorInfo {
        private final String errorCode;
        private final String userMessage;
        private final String suggestion;
        private final String technicalDetails;
        
        public ErrorInfo(String errorCode, String userMessage, String suggestion, String technicalDetails) {
            this.errorCode = errorCode;
            this.userMessage = userMessage;
            this.suggestion = suggestion;
            this.technicalDetails = technicalDetails;
        }
        
        public String getErrorCode() { return errorCode; }
        public String getUserMessage() { return userMessage; }
        public String getSuggestion() { return suggestion; }
        public String getTechnicalDetails() { return technicalDetails; }
    }
    
    private static class BatchProcessingResult {
        private final int totalCount;
        private final int successCount;
        private final List<BatchFailure> failures;
        
        public BatchProcessingResult(int totalCount, int successCount, List<BatchFailure> failures) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failures = failures;
        }
        
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return totalCount - successCount; }
        public List<BatchFailure> getFailures() { return failures; }
    }
    
    private static class BatchFailure {
        private final String signalName;
        private final String errorMessage;
        private final String errorType;
        
        public BatchFailure(String signalName, String errorMessage, String errorType) {
            this.signalName = signalName;
            this.errorMessage = errorMessage;
            this.errorType = errorType;
        }
        
        public String getSignalName() { return signalName; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorType() { return errorType; }
    }
}
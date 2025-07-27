package ai.prophetizo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the ErrorHandlingDemo class to ensure it runs without exceptions
 * and produces expected output.
 */
class ErrorHandlingDemoTest {

    @Test
    void errorHandlingDemoShouldRunWithoutExceptions() {
        // Capture output to verify the demo runs properly
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        
        try {
            System.setOut(new PrintStream(outputStream));
            
            // Run the demo - should not throw any exceptions
            assertDoesNotThrow(() -> ErrorHandlingDemo.main(new String[]{}));
            
            String output = outputStream.toString();
            
            // Verify key sections are present in output
            assertTrue(output.contains("VectorWave - Error Handling Best Practices Demo"));
            assertTrue(output.contains("INPUT VALIDATION PATTERNS"));
            assertTrue(output.contains("EXCEPTION CLASSIFICATION AND HANDLING"));
            assertTrue(output.contains("RECOVERY STRATEGIES"));
            assertTrue(output.contains("BATCH PROCESSING WITH ERROR HANDLING"));
            assertTrue(output.contains("PROPER ERROR REPORTING AND LOGGING"));
            assertTrue(output.contains("CONFIGURATION VALIDATION"));
            
            // Verify that validation results are shown
            assertTrue(output.contains("✓ Signal is valid"));
            assertTrue(output.contains("✗ Validation failed"));
            
            // Verify that recovery strategies are demonstrated
            assertTrue(output.contains("✓ Transform successful after padding"));
            assertTrue(output.contains("✓ Transform successful after truncation"));
            
            // Verify batch processing results are shown
            assertTrue(output.contains("Batch processing complete:"));
            assertTrue(output.contains("Successful:"));
            assertTrue(output.contains("Failed:"));
            
        } finally {
            // Restore original output stream
            System.setOut(originalOut);
        }
    }
    
    @Test
    void mainShouldReferenceErrorHandlingDemo() {
        // Capture output to verify the main demo references the error handling demo
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        
        try {
            System.setOut(new PrintStream(outputStream));
            
            // Run the main demo
            assertDoesNotThrow(() -> Main.main(new String[]{}));
            
            String output = outputStream.toString();
            
            // Verify that the main demo references the error handling demo
            assertTrue(output.contains("For comprehensive error handling examples, run:"));
            assertTrue(output.contains("java -cp target/classes ai.prophetizo.ErrorHandlingDemo"));
            
        } finally {
            // Restore original output stream
            System.setOut(originalOut);
        }
    }
}
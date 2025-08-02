package ai.prophetizo;

import ai.prophetizo.demo.BasicUsageDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for BasicUsageDemo class to ensure demo code coverage.
 */
@DisplayName("Basic Usage Demo")
class MainTest {
    
    @Test
    @DisplayName("BasicUsageDemo main method runs without errors")
    void testMainMethod() {
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            // Run main method
            BasicUsageDemo.main(new String[]{});
            
            // Verify output contains expected content
            String output = outputStream.toString();
            assertTrue(output.contains("VectorWave Basic Usage Demo"));
            assertTrue(output.contains("Basic Transform"));
            assertTrue(output.contains("Factory Pattern"));
            assertTrue(output.contains("Signal Lengths"));
            
            // Verify no exceptions were thrown
            // Note: Main.demonstrateStreamingDenoiser() prints "Error in streaming denoiser demo:" in catch block
            assertFalse(output.contains("Exception"));
            
        } finally {
            // Restore original output
            System.setOut(originalOut);
        }
    }
    
    @Test
    @DisplayName("BasicUsageDemo method with null args")
    void testMainMethodWithNullArgs() {
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            // Should handle null args gracefully
            assertDoesNotThrow(() -> BasicUsageDemo.main(null));
            
        } finally {
            // Restore original output
            System.setOut(originalOut);
        }
    }
}
package ai.prophetizo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Main class to ensure demo code coverage.
 */
@DisplayName("Main Demo")
class MainTest {
    
    @Test
    @DisplayName("Main method runs without errors")
    void testMainMethod() {
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            // Run main method
            Main.main(new String[]{});
            
            // Verify output contains expected content
            String output = outputStream.toString();
            assertTrue(output.contains("VectorWave - Fast Wavelet Transform Demo"));
            assertTrue(output.contains("ORTHOGONAL WAVELETS"));
            assertTrue(output.contains("BIORTHOGONAL WAVELETS")); 
            assertTrue(output.contains("CONTINUOUS WAVELETS"));
            assertTrue(output.contains("WAVELET REGISTRY"));
            assertTrue(output.contains("MULTI-LEVEL DECOMPOSITION"));
            assertTrue(output.contains("STREAMING DENOISER"));
            
            // Verify no exceptions were thrown
            // Note: Main.demonstrateStreamingDenoiser() prints "Error in streaming denoiser demo:" in catch block
            assertFalse(output.contains("Exception"));
            
        } finally {
            // Restore original output
            System.setOut(originalOut);
        }
    }
    
    @Test
    @DisplayName("Main method with null args")
    void testMainMethodWithNullArgs() {
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            // Should handle null args gracefully
            assertDoesNotThrow(() -> Main.main(null));
            
        } finally {
            // Restore original output
            System.setOut(originalOut);
        }
    }
}
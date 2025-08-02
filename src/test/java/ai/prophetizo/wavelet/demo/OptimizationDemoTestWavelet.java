package ai.prophetizo.wavelet.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizationDemoTestWavelet {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }
    
    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }
    
    @Test
    public void testMainMethod() {
        OptimizationDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify header
        assertTrue(output.contains("=== VectorWave Optimization Demo ==="));
        
        // Verify platform information is displayed
        assertTrue(output.contains("Platform Information:"));
        assertTrue(output.contains("OS:"));
        assertTrue(output.contains("Architecture:"));
        assertTrue(output.contains("Processors:"));
        
        // Verify optimization checks
        assertTrue(output.contains("Available Optimizations:"));
        assertTrue(output.contains("Vector API:"));
        assertTrue(output.contains("Apple Silicon:"));
        
        // Verify performance comparison runs
        assertTrue(output.contains("Signal Size:"));
        assertTrue(output.contains("Wavelet:"));
        assertTrue(output.contains("Scalar:"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
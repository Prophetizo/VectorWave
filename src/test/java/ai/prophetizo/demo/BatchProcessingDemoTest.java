package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class BatchProcessingDemoTest {
    
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
    public void testMainMethod() throws Exception {
        BatchProcessingDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify header
        assertTrue(output.contains("VectorWave Batch Processing Demo"));
        
        // Verify platform information is shown
        assertTrue(output.contains("Platform Information:"));
        assertTrue(output.contains("SIMD vector length:"));
        
        // Verify all demo sections are executed
        assertTrue(output.contains("Basic Batch Transform"));
        assertTrue(output.contains("Optimized Transform Engine"));
        assertTrue(output.contains("Memory-Aligned Batch Processing"));
        assertTrue(output.contains("Multi-Channel Audio Processing"));
        assertTrue(output.contains("Financial Time Series Analysis"));
        assertTrue(output.contains("Performance Comparison"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
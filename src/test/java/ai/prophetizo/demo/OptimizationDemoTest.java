package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizationDemoTest {
    
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
        OptimizationDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify optimization demonstration
        assertTrue(output.contains("VectorWave - Optimization Demo"));
        
        // Verify optimization sections
        assertTrue(output.contains("Wavelet Selection Optimization"));
        
        // Check for wavelet comparisons
        assertTrue(output.contains("Haar"));
        assertTrue(output.contains("Reconstruction error"));
        assertTrue(output.contains("Transform time"));
    }
}
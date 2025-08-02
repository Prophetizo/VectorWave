package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class DenoisingDemoTest {
    
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
        DenoisingDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify demo execution
        assertTrue(output.contains("=== VectorWave Denoising Demo ==="));
        
        // Verify denoising methods are demonstrated
        assertTrue(output.contains("Threshold Method Comparison"));
        assertTrue(output.contains("Soft vs Hard Thresholding"));
        assertTrue(output.contains("Multi-level Denoising"));
        
        // Verify metrics are calculated
        assertTrue(output.contains("SNR"));
        assertTrue(output.contains("MSE"));
    }
}
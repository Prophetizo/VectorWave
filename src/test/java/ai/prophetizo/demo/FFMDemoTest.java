package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class FFMDemoTest {
    
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
        // Run the demo - should complete successfully
        FFMDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify FFM demo output
        assertTrue(output.contains("FFM") || output.contains("Foreign Function") || 
                   output.contains("Memory") || output.contains("VectorWave"),
                   "Expected FFM-related output");
        
        // Verify demo sections were executed
        assertTrue(output.contains("Basic FFM Usage") || 
                   output.contains("Memory Pool Management") ||
                   output.contains("Zero-Copy Processing") ||
                   output.contains("Streaming with FFM") ||
                   output.contains("Performance Comparison") ||
                   output.contains("Advanced Memory Segment Operations"),
                   "Expected demo section headers");
    }
}
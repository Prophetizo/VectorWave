package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryPoolLifecycleDemoTest {
    
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
        MemoryPoolLifecycleDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify memory pool lifecycle is demonstrated
        assertTrue(output.contains("Memory") || output.contains("memory"));
        assertTrue(output.contains("Pool") || output.contains("pool"));
        
        // Check for lifecycle stages
        assertTrue(output.contains("allocat") || output.contains("Allocat") ||
                   output.contains("reuse") || output.contains("Reuse") ||
                   output.contains("clean") || output.contains("Clean"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
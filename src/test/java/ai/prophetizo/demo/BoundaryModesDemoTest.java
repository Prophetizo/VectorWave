package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class BoundaryModesDemoTest {
    
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
        BoundaryModesDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify boundary modes are demonstrated
        assertTrue(output.contains("Boundary") || output.contains("boundary"));
        
        // Check for different boundary mode types
        assertTrue(output.contains("PERIODIC") || output.contains("periodic") ||
                   output.contains("ZERO") || output.contains("zero") ||
                   output.contains("SYMMETRIC") || output.contains("symmetric"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
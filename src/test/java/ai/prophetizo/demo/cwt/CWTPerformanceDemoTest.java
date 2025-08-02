package ai.prophetizo.demo.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class CWTPerformanceDemoTest {
    
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
        CWTPerformanceDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify CWT performance demonstration
        assertTrue(output.contains("Performance") || output.contains("performance") ||
                   output.contains("CWT"));
        
        // Check for performance metrics
        assertTrue(output.contains("ms") || output.contains("time") || 
                   output.contains("FFT") || output.contains("optimization"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
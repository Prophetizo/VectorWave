package ai.prophetizo.demo.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class ComplexCWTDemoTest {
    
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
        ComplexCWTDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify complex CWT demonstration
        assertTrue(output.contains("Complex CWT") || output.contains("Complex Continuous"));
        
        // Verify complex-specific features
        assertTrue(output.contains("magnitude") || output.contains("phase") || 
                   output.contains("real") || output.contains("imaginary"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
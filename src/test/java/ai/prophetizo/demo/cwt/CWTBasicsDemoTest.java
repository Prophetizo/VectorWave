package ai.prophetizo.demo.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class CWTBasicsDemoTest {
    
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
        CWTBasicsDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify CWT basics are demonstrated
        assertTrue(output.contains("CWT Basics Demo") || output.contains("Continuous Wavelet Transform"));
        
        // Verify key CWT concepts are covered
        assertTrue(output.contains("Morlet") || output.contains("scales") || 
                   output.contains("coefficients"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
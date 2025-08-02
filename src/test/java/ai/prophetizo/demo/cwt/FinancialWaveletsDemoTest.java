package ai.prophetizo.demo.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialWaveletsDemoTest {
    
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
        FinancialWaveletsDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify financial wavelets demonstration
        assertTrue(output.contains("Financial") || output.contains("financial"));
        
        // Check for financial wavelet types
        assertTrue(output.contains("Paul") || output.contains("paul") ||
                   output.contains("Shannon") || output.contains("shannon") ||
                   output.contains("DOG") || output.contains("Gaussian"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
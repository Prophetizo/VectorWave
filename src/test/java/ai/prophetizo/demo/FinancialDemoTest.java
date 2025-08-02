package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialDemoTest {
    
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
        FinancialDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify financial demo runs
        assertTrue(output.contains("VectorWave - Financial Analysis Demo"));
        
        // Verify financial metrics are calculated
        assertTrue(output.contains("Sharpe Ratio"));
        assertTrue(output.contains("Returns"));
        assertTrue(output.contains("STANDARD CONFIGURATION"));
        assertTrue(output.contains("WAVELET-BASED DENOISING"));
    }
}
package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class BasicUsageDemoTest {
    
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
        BasicUsageDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify all demo sections are executed
        assertTrue(output.contains("=== VectorWave Basic Usage Demo ==="));
        assertTrue(output.contains("1. Basic Transform Example"));
        assertTrue(output.contains("2. Factory Pattern Usage"));
        assertTrue(output.contains("3. Different Signal Lengths"));
        assertTrue(output.contains("4. Error Handling Best Practices"));
        assertTrue(output.contains("5. Coefficient Analysis"));
    }
}
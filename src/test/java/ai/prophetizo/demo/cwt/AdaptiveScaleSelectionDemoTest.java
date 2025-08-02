package ai.prophetizo.demo.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class AdaptiveScaleSelectionDemoTest {
    
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
        AdaptiveScaleSelectionDemo.main(new String[]{});
        
        String output = outContent.toString();
        
        // Verify adaptive scale selection is demonstrated
        assertTrue(output.contains("Adaptive") || output.contains("adaptive") ||
                   output.contains("Scale") || output.contains("scale"));
        
        // Verify no unexpected errors
        assertFalse(output.toLowerCase().contains("exception"));
    }
}
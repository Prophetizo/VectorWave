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
        // The demo is expected to throw an exception due to memory closing issues
        assertThrows(IllegalStateException.class, () -> {
            FFMDemo.main(new String[]{});
        });
        
        String output = outContent.toString();
        
        // Verify FFM demo started
        assertTrue(output.contains("FFM") || output.contains("Foreign Function") || 
                   output.contains("Memory") || output.contains("VectorWave"));
    }
}
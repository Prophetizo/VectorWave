package ai.prophetizo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

public class PluginArchitectureDemoTest {
    
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
        // The demo is expected to throw an exception when looking up a non-existent wavelet
        assertThrows(ai.prophetizo.wavelet.exception.InvalidArgumentException.class, () -> {
            PluginArchitectureDemo.main(new String[]{});
        });
        
        String output = outContent.toString();
        
        // Verify plugin architecture demonstration started
        assertTrue(output.contains("Plugin") || output.contains("plugin") ||
                   output.contains("Provider") || output.contains("provider") ||
                   output.contains("VectorWave"));
        
        // Check for wavelet providers if they were printed before the exception
        if (output.length() > 100) {
            assertTrue(output.contains("Orthogonal") || output.contains("Biorthogonal") ||
                       output.contains("Continuous") || output.contains("wavelet"));
        }
    }
}
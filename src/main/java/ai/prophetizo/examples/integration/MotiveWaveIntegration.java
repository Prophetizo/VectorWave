package ai.prophetizo.examples.integration;

import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletName;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.modwt.*;
import java.util.*;

/**
 * MotiveWave integration example - shows how to use VectorWave in trading platforms.
 * 
 * Run: mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.integration.MotiveWaveIntegration"
 */
public class MotiveWaveIntegration {
    
    public static void main(String[] args) {
        // That's it. No initialization needed. Just use it.
        
        // Get available wavelets for dropdown menu
        List<WaveletName> wavelets = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Available wavelets for dropdown: " + wavelets);
        
        // User selects DB4 from dropdown
        WaveletName selected = WaveletName.DB4;
        
        // Get the wavelet and use it
        if (WaveletRegistry.hasWavelet(selected)) {
            Wavelet wavelet = WaveletRegistry.getWavelet(selected);
            
            // Create transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            // Use it on price data
            double[] prices = {100, 102, 101, 103, 105, 104, 106, 108};
            MODWTResult result = transform.forward(prices);
            
            System.out.println("Transform complete!");
        }
    }
    
    /**
     * Example of what the MotiveWave study would look like.
     */
    public static class MotiveWaveStudy {
        
        public WaveletName[] getWaveletChoices() {
            // Get wavelets for settings dropdown
            return WaveletRegistry.getOrthogonalWavelets().toArray(new WaveletName[0]);
        }
        
        public void calculate(WaveletName waveletName, double[] data) {
            // Get selected wavelet
            Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
            
            // Do transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(data);
            
            // Use results...
        }
    }
}
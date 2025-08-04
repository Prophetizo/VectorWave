package ai.prophetizo.wavelet.modwt.streaming;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the threshold multiplier functionality works correctly
 * in MODWTStreamingDenoiser.
 */
class MODWTStreamingDenoiserThresholdMultiplierTest {
    
    @Test
    void testThresholdMultiplierEffect() {
        // Create test signal with known noise pattern
        double[] noisySignal = new double[256];
        for (int i = 0; i < noisySignal.length; i++) {
            // Clean signal + noise
            noisySignal[i] = Math.sin(i * 0.1) + 0.1 * (Math.random() - 0.5);
        }
        
        // Test with different threshold multipliers
        double[] multipliers = {0.5, 1.0, 2.0};
        double[][] results = new double[multipliers.length][];
        
        for (int i = 0; i < multipliers.length; i++) {
            MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .boundaryMode(BoundaryMode.PERIODIC)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .thresholdType(ThresholdType.SOFT)
                .thresholdMultiplier(multipliers[i])
                .noiseWindowSize(100)
                .build();
            
            // Process a few chunks first to establish noise estimation
            double[] warmupSignal = new double[64];
            System.arraycopy(noisySignal, 0, warmupSignal, 0, 64);
            denoiser.denoise(warmupSignal);  // First chunk to establish noise estimation
            
            // Now test with the full signal
            results[i] = denoiser.denoise(noisySignal.clone());
            denoiser.close();
        }
        
        // Verify that different multipliers produce different results
        // Lower multiplier (0.5) should preserve more signal details
        // Higher multiplier (2.0) should remove more noise but may over-smooth
        
        // Calculate energy of the denoised signals
        double[] energies = new double[multipliers.length];
        for (int i = 0; i < multipliers.length; i++) {
            double energy = 0.0;
            for (double val : results[i]) {
                energy += val * val;
            }
            energies[i] = energy;
        }
        
        // With soft thresholding, higher multipliers should generally result in lower energy
        // (more aggressive denoising removes more coefficients)
        assertTrue(energies[0] >= energies[1], 
            "Lower multiplier (0.5) should preserve more energy than standard (1.0)");
        assertTrue(energies[1] >= energies[2], 
            "Standard multiplier (1.0) should preserve more energy than higher (2.0)");
        
        // Results should be different (not identical)
        assertNotEquals(java.util.Arrays.toString(results[0]), 
                       java.util.Arrays.toString(results[1]),
                       "Results with different multipliers should be different");
        assertNotEquals(java.util.Arrays.toString(results[1]), 
                       java.util.Arrays.toString(results[2]),
                       "Results with different multipliers should be different");
    }
    
    @Test
    void testThresholdMultiplierWithDifferentMethods() {
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.05) + 0.05 * Math.random();
        }
        
        ThresholdMethod[] methods = {
            ThresholdMethod.UNIVERSAL, 
            ThresholdMethod.SURE, 
            ThresholdMethod.MINIMAX
        };
        
        // Test that threshold multiplier works with different threshold methods
        for (ThresholdMethod method : methods) {
            MODWTStreamingDenoiser denoiserDefault = new MODWTStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .boundaryMode(BoundaryMode.PERIODIC)
                .thresholdMethod(method)
                .thresholdMultiplier(0.8)  // Use clearly different values
                .build();
            
            MODWTStreamingDenoiser denoiserAggressive = new MODWTStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .boundaryMode(BoundaryMode.PERIODIC)
                .thresholdMethod(method)
                .thresholdMultiplier(1.5)
                .build();
            
            // Process warmup data to establish noise estimation
            double[] warmup = new double[32];
            System.arraycopy(signal, 0, warmup, 0, 32);
            denoiserDefault.denoise(warmup.clone());
            denoiserAggressive.denoise(warmup.clone());
            
            double[] resultDefault = denoiserDefault.denoise(signal.clone());
            double[] resultAggressive = denoiserAggressive.denoise(signal.clone());
            
            // Results should be different
            boolean isDifferent = false;
            for (int i = 0; i < resultDefault.length; i++) {
                if (Math.abs(resultDefault[i] - resultAggressive[i]) > 1e-10) {
                    isDifferent = true;
                    break;
                }
            }
            assertTrue(isDifferent, 
                "Threshold multiplier should produce different results for method: " + method);
            
            denoiserDefault.close();
            denoiserAggressive.close();
        }
    }
    
    @Test
    void testThresholdMultiplierWithNoiseEstimation() {
        double[] signal = new double[200];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(i * 0.02) + 0.08 * (Math.random() - 0.5);
        }
        
        // Test with different noise estimation methods
        MODWTStreamingDenoiser.NoiseEstimation[] noiseTypes = {
            MODWTStreamingDenoiser.NoiseEstimation.MAD,
            MODWTStreamingDenoiser.NoiseEstimation.STD
        };
        
        for (MODWTStreamingDenoiser.NoiseEstimation noiseType : noiseTypes) {
            MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .boundaryMode(BoundaryMode.PERIODIC)
                .thresholdMultiplier(1.2)
                .noiseEstimation(noiseType)
                .noiseWindowSize(50)
                .build();
            
            // Process multiple chunks to build up noise estimation
            for (int chunk = 0; chunk < 3; chunk++) {
                double[] chunkSignal = new double[64];
                System.arraycopy(signal, chunk * 64, chunkSignal, 0, 64);
                
                double[] result = denoiser.denoise(chunkSignal);
                assertNotNull(result, "Denoising result should not be null");
                assertEquals(64, result.length, "Result should have same length as input");
                
                // After first chunk, noise level should be estimated
                if (chunk > 0) {
                    assertTrue(denoiser.getEstimatedNoiseLevel() > 0, 
                        "Noise level should be estimated after processing chunks");
                }
            }
            
            denoiser.close();
        }
    }
}
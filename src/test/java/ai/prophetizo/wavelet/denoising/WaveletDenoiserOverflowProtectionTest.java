package ai.prophetizo.wavelet.denoising;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify overflow protection in WaveletDenoiser bit shift operations.
 */
class WaveletDenoiserOverflowProtectionTest {
    
    @Test
    void testBitShiftOverflowProtection() {
        // Create a mock result with an extremely high level count
        MultiLevelMODWTResult mockResult = new MultiLevelMODWTResult() {
            @Override
            public int getSignalLength() {
                return 256;
            }
            
            @Override
            public int getLevels() {
                return 32; // This would cause overflow: 1 << 31
            }
            
            @Override
            public double[] getApproximationCoeffs() {
                return new double[256];
            }
            
            @Override
            public double[] getDetailCoeffsAtLevel(int level) {
                return new double[256];
            }
            
            @Override
            public double getDetailEnergyAtLevel(int level) {
                return 1.0;
            }
            
            @Override
            public double getApproximationEnergy() {
                return 1.0;
            }
            
            @Override
            public double getTotalEnergy() {
                return 33.0;
            }
            
            @Override
            public double[] getRelativeEnergyDistribution() {
                return new double[33];
            }
            
            @Override
            public boolean isValid() {
                return true;
            }
            
            @Override
            public MultiLevelMODWTResult copy() {
                return this;
            }
        };
        
        WaveletDenoiser denoiser = new WaveletDenoiser(new Haar(), BoundaryMode.PERIODIC);
        
        // Creating the DenoisedMultiLevelResult should throw an exception due to overflow protection
        assertThrows(IllegalArgumentException.class, () -> {
            // This will attempt to create a DenoisedMultiLevelResult with level 32
            denoiser.new DenoisedMultiLevelResult(
                mockResult, 
                1.0, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                WaveletDenoiser.ThresholdType.SOFT
            );
        }, "Should throw IllegalArgumentException for level that would cause overflow");
    }
    
    @Test
    void testSafeLevelsWork() {
        // Create a mock result with safe level count
        MultiLevelMODWTResult mockResult = new MultiLevelMODWTResult() {
            private final double[][] details = new double[10][256];
            
            @Override
            public int getSignalLength() {
                return 256;
            }
            
            @Override
            public int getLevels() {
                return 10; // Safe level count
            }
            
            @Override
            public double[] getApproximationCoeffs() {
                return new double[256];
            }
            
            @Override
            public double[] getDetailCoeffsAtLevel(int level) {
                if (level < 1 || level > 10) {
                    throw new IllegalArgumentException("Invalid level");
                }
                return details[level - 1];
            }
            
            @Override
            public double getDetailEnergyAtLevel(int level) {
                return 1.0;
            }
            
            @Override
            public double getApproximationEnergy() {
                return 1.0;
            }
            
            @Override
            public double getTotalEnergy() {
                return 11.0;
            }
            
            @Override
            public double[] getRelativeEnergyDistribution() {
                return new double[11];
            }
            
            @Override
            public boolean isValid() {
                return true;
            }
            
            @Override
            public MultiLevelMODWTResult copy() {
                return this;
            }
        };
        
        WaveletDenoiser denoiser = new WaveletDenoiser(new Haar(), BoundaryMode.PERIODIC);
        
        // Creating the DenoisedMultiLevelResult should work fine with safe levels
        assertDoesNotThrow(() -> {
            denoiser.new DenoisedMultiLevelResult(
                mockResult, 
                1.0, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                WaveletDenoiser.ThresholdType.SOFT
            );
        }, "Should work fine with safe level count");
    }
    
    @Test
    void testBoundaryLevel31() {
        // Test the boundary case where level = 31 (should work)
        // 1 << 30 is the largest safe shift
        int level = 31;
        int shiftAmount = level - 1; // = 30
        
        // This should not overflow
        assertDoesNotThrow(() -> {
            int powerOf2 = 1 << shiftAmount;
            assertTrue(powerOf2 > 0, "Should be positive");
            assertEquals(1073741824, powerOf2); // 2^30
            
            double levelScale = Math.sqrt(powerOf2);
            assertTrue(Double.isFinite(levelScale));
            assertEquals(32768.0, levelScale, 0.01);
        });
    }
}
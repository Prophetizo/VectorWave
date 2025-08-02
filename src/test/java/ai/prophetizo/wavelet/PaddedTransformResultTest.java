package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaddedTransformResultTest {

    @Test
    void testConstructorWithDelegate_ValidInputs() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        TransformResult delegate = new TransformResultImpl(approx, detail);
        
        PaddedTransformResult result = new PaddedTransformResult(delegate, 6);
        
        assertEquals(6, result.originalLength());
        assertArrayEquals(approx, result.approximationCoeffs());
        assertArrayEquals(detail, result.detailCoeffs());
    }

    @Test
    void testConstructorWithDelegate_NullDelegate_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> new PaddedTransformResult(null, 10)
        );
        assertEquals("Delegate result cannot be null", exception.getMessage());
    }

    @Test
    void testConstructorWithDelegate_ZeroOriginalLength_ThrowsException() {
        TransformResult delegate = new TransformResultImpl(new double[4], new double[4]);
        
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> new PaddedTransformResult(delegate, 0)
        );
        assertEquals("Original length must be positive", exception.getMessage());
    }

    @Test
    void testConstructorWithDelegate_NegativeOriginalLength_ThrowsException() {
        TransformResult delegate = new TransformResultImpl(new double[4], new double[4]);
        
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> new PaddedTransformResult(delegate, -5)
        );
        assertEquals("Original length must be positive", exception.getMessage());
    }

    @Test
    void testConstructorWithArrays_ValidInputs() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 6);
        
        assertEquals(6, result.originalLength());
        assertArrayEquals(approx, result.approximationCoeffs());
        assertArrayEquals(detail, result.detailCoeffs());
    }

    @Test
    void testPaddedLength() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 6);
        
        assertEquals(8, result.paddedLength()); // 4 + 4
    }

    @Test
    void testPaddedLength_DifferentSizes() {
        // Test various coefficient sizes
        PaddedTransformResult result1 = new PaddedTransformResult(
                new double[8], new double[8], 15
        );
        assertEquals(16, result1.paddedLength());
        
        PaddedTransformResult result2 = new PaddedTransformResult(
                new double[16], new double[16], 30
        );
        assertEquals(32, result2.paddedLength());
        
        PaddedTransformResult result3 = new PaddedTransformResult(
                new double[2], new double[2], 3
        );
        assertEquals(4, result3.paddedLength());
    }

    @Test
    void testOriginalLengthLessThanPaddedLength() {
        // Common case: original signal was not power of 2
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 7);
        
        assertEquals(7, result.originalLength());
        assertEquals(8, result.paddedLength());
        assertTrue(result.originalLength() < result.paddedLength());
    }

    @Test
    void testOriginalLengthEqualsPaddedLength() {
        // Case where original was already power of 2
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 8);
        
        assertEquals(8, result.originalLength());
        assertEquals(8, result.paddedLength());
    }

    @Test
    void testToString() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 6);
        String str = result.toString();
        
        assertTrue(str.contains("PaddedTransformResult"));
        assertTrue(str.contains("originalLength=6"));
        assertTrue(str.contains("paddedLength=8"));
        assertTrue(str.contains("approxCoeffs=4"));
        assertTrue(str.contains("detailCoeffs=4"));
    }

    @Test
    void testDefensiveCopies() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {5.0, 6.0, 7.0, 8.0};
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 6);
        
        // Get coefficients
        double[] approxCopy = result.approximationCoeffs();
        double[] detailCopy = result.detailCoeffs();
        
        // Modify the copies
        approxCopy[0] = 999.0;
        detailCopy[0] = 888.0;
        
        // Original should be unchanged
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0}, result.approximationCoeffs());
        assertArrayEquals(new double[]{5.0, 6.0, 7.0, 8.0}, result.detailCoeffs());
    }

    @Test
    void testImplementsTransformResult() {
        PaddedTransformResult result = new PaddedTransformResult(
                new double[4], new double[4], 6
        );
        
        // Verify it properly implements TransformResult interface
        assertTrue(result instanceof TransformResult);
        
        // Can be used as TransformResult
        TransformResult asInterface = result;
        assertNotNull(asInterface.approximationCoeffs());
        assertNotNull(asInterface.detailCoeffs());
    }

    @Test
    void testEdgeCases() {
        // Minimum valid original length
        PaddedTransformResult result1 = new PaddedTransformResult(
                new double[1], new double[1], 1
        );
        assertEquals(1, result1.originalLength());
        assertEquals(2, result1.paddedLength());
        
        // Large original length
        PaddedTransformResult result2 = new PaddedTransformResult(
                new double[512], new double[512], 1000
        );
        assertEquals(1000, result2.originalLength());
        assertEquals(1024, result2.paddedLength());
    }

    @Test
    void testChainedConstruction() {
        // Test creating from another TransformResult implementation
        ImmutableTransformResult immutable = new ImmutableTransformResult(
                new double[]{1.0, 2.0}, new double[]{3.0, 4.0}
        );
        
        // Convert to TransformResult first since ImmutableTransformResult doesn't implement it
        TransformResult transformResult = immutable.toTransformResult();
        PaddedTransformResult padded = new PaddedTransformResult(transformResult, 3);
        
        assertEquals(3, padded.originalLength());
        assertEquals(4, padded.paddedLength());
        assertArrayEquals(new double[]{1.0, 2.0}, padded.approximationCoeffs());
        assertArrayEquals(new double[]{3.0, 4.0}, padded.detailCoeffs());
    }

    @Test
    void testConsistency() {
        double[] approx = new double[16];
        double[] detail = new double[16];
        
        for (int i = 0; i < 16; i++) {
            approx[i] = i;
            detail[i] = i + 16;
        }
        
        PaddedTransformResult result = new PaddedTransformResult(approx, detail, 30);
        
        // Multiple calls should return same values
        assertEquals(30, result.originalLength());
        assertEquals(30, result.originalLength());
        assertEquals(32, result.paddedLength());
        assertEquals(32, result.paddedLength());
        
        // Coefficient arrays should be consistent
        assertArrayEquals(result.approximationCoeffs(), result.approximationCoeffs());
        assertArrayEquals(result.detailCoeffs(), result.detailCoeffs());
    }
}
package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.nio.DoubleBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ImmutableTransformResultTest {

    @Test
    void testConstructor_WithValidArrays() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        
        assertArrayEquals(approx, result.approximationCoeffs());
        assertArrayEquals(detail, result.detailCoeffs());
    }

    @Test
    void testConstructor_WithNullApproximationArray_ThrowsException() {
        double[] detail = {1.0, 2.0};
        
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> new ImmutableTransformResult(null, detail)
        );
        assertEquals("Coefficient arrays cannot be null", exception.getMessage());
    }

    @Test
    void testConstructor_WithNullDetailArray_ThrowsException() {
        double[] approx = {1.0, 2.0};
        
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> new ImmutableTransformResult(approx, null)
        );
        assertEquals("Coefficient arrays cannot be null", exception.getMessage());
    }

    @Test
    void testConstructor_WithMismatchedArrayLengths_ThrowsException() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0};
        
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> new ImmutableTransformResult(approx, detail)
        );
        assertEquals("Coefficient arrays must have equal length", exception.getMessage());
    }

    @Test
    void testFromWorkspace_WithValidWorkspace() {
        double[] workspace = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        int coeffLength = 3;
        
        ImmutableTransformResult result = ImmutableTransformResult.fromWorkspace(workspace, coeffLength);
        
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result.approximationCoeffs());
        assertArrayEquals(new double[]{4.0, 5.0, 6.0}, result.detailCoeffs());
    }

    @Test
    void testFromWorkspace_WithInvalidWorkspaceSize_ThrowsException() {
        double[] workspace = {1.0, 2.0, 3.0};
        int coeffLength = 3; // Would need workspace of size 6
        
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> ImmutableTransformResult.fromWorkspace(workspace, coeffLength)
        );
        assertEquals("Workspace size mismatch", exception.getMessage());
    }

    @Test
    void testApproximationCoeffs_ReturnsDefensiveCopy() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        
        double[] returned1 = result.approximationCoeffs();
        double[] returned2 = result.approximationCoeffs();
        
        // Should be different array instances (defensive copies)
        assertNotSame(returned1, returned2);
        assertArrayEquals(returned1, returned2);
        
        // Modifying returned array shouldn't affect internal state
        returned1[0] = 999.0;
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result.approximationCoeffs());
    }

    @Test
    void testDetailCoeffs_ReturnsDefensiveCopy() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        
        double[] returned1 = result.detailCoeffs();
        double[] returned2 = result.detailCoeffs();
        
        // Should be different array instances (defensive copies)
        assertNotSame(returned1, returned2);
        assertArrayEquals(returned1, returned2);
        
        // Modifying returned array shouldn't affect internal state
        returned1[0] = 999.0;
        assertArrayEquals(new double[]{4.0, 5.0, 6.0}, result.detailCoeffs());
    }

    @Test
    void testApproximationCoeffsView_ReturnsReadOnlyBuffer() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        DoubleBuffer buffer = result.approximationCoeffsView();
        
        // Verify content
        assertEquals(3, buffer.remaining());
        assertEquals(1.0, buffer.get(0));
        assertEquals(2.0, buffer.get(1));
        assertEquals(3.0, buffer.get(2));
        
        // Verify it's read-only
        assertTrue(buffer.isReadOnly());
        assertThrows(Exception.class, () -> buffer.put(0, 999.0));
    }

    @Test
    void testDetailCoeffsView_ReturnsReadOnlyBuffer() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        DoubleBuffer buffer = result.detailCoeffsView();
        
        // Verify content
        assertEquals(3, buffer.remaining());
        // Buffer is positioned at the detail coefficients
        assertEquals(4.0, buffer.get());
        assertEquals(5.0, buffer.get());
        assertEquals(6.0, buffer.get());
        
        // Verify it's read-only
        assertTrue(buffer.isReadOnly());
        assertThrows(Exception.class, () -> buffer.put(0, 999.0));
    }

    @Test
    void testGetCoefficientsArray_PackagePrivateAccess() {
        double[] approx = {1.0, 2.0};
        double[] detail = {3.0, 4.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        
        // Test package-private access
        double[] internal = result.getCoefficientsArray();
        assertEquals(4, internal.length);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0}, internal);
    }

    @Test
    void testGetSplitPoint_PackagePrivateAccess() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        assertEquals(3, result.getSplitPoint());
    }

    @Test
    void testToTransformResult() {
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {4.0, 5.0, 6.0};
        
        ImmutableTransformResult immutable = new ImmutableTransformResult(approx, detail);
        TransformResult standard = immutable.toTransformResult();
        
        assertNotNull(standard);
        assertArrayEquals(approx, standard.approximationCoeffs());
        assertArrayEquals(detail, standard.detailCoeffs());
        
        // Verify it's a TransformResult (ImmutableTransformResult doesn't implement TransformResult)
        assertTrue(standard instanceof TransformResult);
        // We know it's not the same instance as the original
        assertNotSame(immutable, standard);
    }

    @Test
    void testEmptyArrays() {
        double[] empty = new double[0];
        
        ImmutableTransformResult result = new ImmutableTransformResult(empty, empty);
        
        assertEquals(0, result.approximationCoeffs().length);
        assertEquals(0, result.detailCoeffs().length);
        assertEquals(0, result.approximationCoeffsView().remaining());
        assertEquals(0, result.detailCoeffsView().remaining());
    }

    @Test
    void testLargeArrays() {
        int size = 1000;
        double[] approx = new double[size];
        double[] detail = new double[size];
        
        for (int i = 0; i < size; i++) {
            approx[i] = i;
            detail[i] = i + size;
        }
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        
        assertEquals(size, result.approximationCoeffs().length);
        assertEquals(size, result.detailCoeffs().length);
        assertEquals(0.0, result.approximationCoeffs()[0]);
        assertEquals(999.0, result.approximationCoeffs()[size - 1]);
        assertEquals(1000.0, result.detailCoeffs()[0]);
        assertEquals(1999.0, result.detailCoeffs()[size - 1]);
    }

    @Test
    void testWorkspaceModification() {
        double[] workspace = {1.0, 2.0, 3.0, 4.0};
        ImmutableTransformResult result = ImmutableTransformResult.fromWorkspace(workspace, 2);
        
        // Modify original workspace
        workspace[0] = 999.0;
        
        // Internal state should be affected (no defensive copy in fromWorkspace)
        assertEquals(999.0, result.getCoefficientsArray()[0]);
        
        // But public API should still return defensive copies
        double[] approxCopy = result.approximationCoeffs();
        assertEquals(999.0, approxCopy[0]);
        
        // Further modifications to the copy shouldn't affect anything
        approxCopy[0] = 111.0;
        assertEquals(999.0, result.approximationCoeffs()[0]);
    }
}
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;

public class TestSymmetricDebug {
    public static void main(String[] args) {
        // Simple test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        
        // Create transform with symmetric boundary
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.SYMMETRIC);
        
        // Forward transform
        MODWTResult result = transform.forward(signal);
        
        // Get coefficients
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        System.out.println("Original signal:");
        printArray(signal);
        
        System.out.println("\nApproximation coefficients:");
        printArray(approx);
        
        System.out.println("\nDetail coefficients:");
        printArray(detail);
        
        // Inverse transform
        double[] reconstructed = transform.inverse(result);
        
        System.out.println("\nReconstructed signal:");
        printArray(reconstructed);
        
        System.out.println("\nReconstruction errors:");
        for (int i = 0; i < signal.length; i++) {
            double error = signal[i] - reconstructed[i];
            System.out.printf("  [%d]: expected=%.1f, got=%.15f, error=%.15f\n", 
                i, signal[i], reconstructed[i], error);
        }
        
        // Now let's manually trace the symmetric extension
        System.out.println("\nSymmetric extension pattern (forward):");
        System.out.println("For forward transform, signal is extended as: ");
        System.out.println("  ... 3 2 1 | 1 2 3 4 5 6 | 6 5 4 3 2 1 | 1 2 3 ...");
        System.out.println("  Indices map as: signal[i] with symmetric mirroring at boundaries");
    }
    
    private static void printArray(double[] arr) {
        System.out.print("  [");
        for (int i = 0; i < arr.length; i++) {
            System.out.printf("%.15f", arr[i]);
            if (i < arr.length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }
}
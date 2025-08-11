public class TestSymmetricDetailed {
    public static void main(String[] args) {
        // Test the symmetric indexing
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        int N = signal.length;
        int period = 2 * N;
        
        System.out.println("Signal: [1, 2, 3, 4, 5, 6]");
        System.out.println("N = " + N + ", period = " + period);
        System.out.println();
        
        // Show how symmetric extension works
        System.out.println("Symmetric extension (conceptual):");
        System.out.println("Index:  ... -3 -2 -1 | 0  1  2  3  4  5 | 6  7  8  9  10 11 | 12 13 14 ...");
        System.out.println("Value:  ... 4  3  2  | 1  2  3  4  5  6 | 6  5  4  3  2  1  | 1  2  3  ...");
        System.out.println();
        
        // Test forward indexing pattern (t - l)
        System.out.println("Forward transform indexing (t - l) with symmetric boundary:");
        for (int t = 0; t < N; t++) {
            System.out.printf("t=%d: ", t);
            for (int l = 0; l < 2; l++) { // Haar filter length = 2
                int idx = t - l;
                int mod = idx % period;
                if (mod < 0) {
                    mod += period;
                }
                int signalIndex = mod < N ? mod : period - mod - 1;
                System.out.printf("l=%d -> idx=%d, mod=%d, signalIndex=%d (val=%.0f); ", 
                    l, idx, mod, signalIndex, signal[signalIndex]);
            }
            System.out.println();
        }
        System.out.println();
        
        // Test inverse indexing pattern (t + l)
        System.out.println("Inverse transform indexing (t + l) with current implementation:");
        for (int t = 0; t < N; t++) {
            System.out.printf("t=%d: ", t);
            for (int l = 0; l < 2; l++) { // Haar filter length = 2
                int idx = t + l;
                int coeffIndex;
                if (idx < N) {
                    coeffIndex = idx;
                } else {
                    int overflow = idx - N;
                    coeffIndex = N - 1 - (overflow % N);
                }
                System.out.printf("l=%d -> idx=%d, coeffIndex=%d; ", 
                    l, idx, coeffIndex);
            }
            System.out.println();
        }
        System.out.println();
        
        // What should the inverse indexing be?
        System.out.println("Correct inverse indexing (t + l) with symmetric boundary:");
        for (int t = 0; t < N; t++) {
            System.out.printf("t=%d: ", t);
            for (int l = 0; l < 2; l++) {
                int idx = t + l;
                int mod = idx % period;
                int coeffIndex = mod < N ? mod : period - mod - 1;
                System.out.printf("l=%d -> idx=%d, mod=%d, coeffIndex=%d; ", 
                    l, idx, mod, coeffIndex);
            }
            System.out.println();
        }
    }
}
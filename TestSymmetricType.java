public class TestSymmetricType {
    public static void main(String[] args) {
        // Test what type of symmetric extension we have
        double[] signal = {1, 2, 3, 4, 5, 6};
        int N = signal.length;
        
        System.out.println("Signal: [1, 2, 3, 4, 5, 6]");
        System.out.println();
        
        // Whole-sample symmetric (WS): reflects including the boundary
        // ... 3 2 1 | 1 2 3 4 5 6 | 6 5 4 3 2 1 | 1 2 3 ...
        System.out.println("Whole-sample symmetric (reflects including boundary):");
        System.out.println("... 3 2 1 | 1 2 3 4 5 6 | 6 5 4 3 2 1 | 1 2 3 ...");
        System.out.println("Period = 2N = 12");
        System.out.println();
        
        // Half-sample symmetric (HS): reflects excluding the boundary  
        // ... 2 1 | 1 2 3 4 5 6 | 5 4 3 2 1 | 1 2 3 4 5 ...
        System.out.println("Half-sample symmetric (reflects excluding boundary):");
        System.out.println("... 2 1 | 1 2 3 4 5 6 | 5 4 3 2 1 | 1 2 3 4 5 ...");
        System.out.println("Period = 2N-2 = 10");
        System.out.println();
        
        // Our implementation uses whole-sample symmetric
        // Let's verify the expected coefficients
        System.out.println("Our implementation mapping (whole-sample):");
        int period = 2 * N;
        for (int idx = -3; idx <= 14; idx++) {
            int mod = idx % period;
            if (mod < 0) mod += period;
            int signalIndex = mod < N ? mod : period - mod - 1;
            System.out.printf("idx=%2d -> mod=%2d -> signalIndex=%d (value=%d)\n", 
                idx, mod, signalIndex, (int)signal[signalIndex]);
        }
    }
}
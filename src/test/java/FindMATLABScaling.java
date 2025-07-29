public class FindMATLABScaling {
    public static void main(String[] args) {
        // MATLAB mexihat appears to use a width parameter
        // ψ(t) = C * (1 - (t/a)²) * exp(-(t/a)²/2)
        
        double C = 2.0 / (Math.sqrt(3.0) * Math.pow(Math.PI, 0.25));
        
        // Use the fact that at t=-1, MATLAB gives -0.3520653268
        // and our standard formula gives C * (1 - 1) * exp(-1/2) = C * 0 * exp(-0.5)
        // But that's 0! So MATLAB must be scaling t
        
        // Let's solve for 'a' using t=-1
        double matlabAt1 = -0.3520653268;
        
        // We need: C * (1 - (1/a)²) * exp(-(1/a)²/2) = -0.3520653268
        // This is negative, so (1 - (1/a)²) < 0, meaning (1/a)² > 1, so a < 1
        
        // Binary search for 'a'
        double aLow = 0.1;
        double aHigh = 1.0;
        double tolerance = 1e-10;
        
        while (aHigh - aLow > tolerance) {
            double aMid = (aLow + aHigh) / 2;
            double t = -1.0;
            double value = C * (1 - (t/aMid)*(t/aMid)) * Math.exp(-(t/aMid)*(t/aMid)/2);
            
            if (Math.abs(value - matlabAt1) < tolerance) {
                System.out.println("Found a = " + aMid);
                break;
            }
            
            // Since value is monotonic in this range, we can do binary search
            if (value < matlabAt1) {
                aLow = aMid;
            } else {
                aHigh = aMid;
            }
        }
        
        double a = (aLow + aHigh) / 2;
        System.out.println("\nFinal a = " + a);
        
        // Verify with all points
        System.out.println("\nVerification:");
        double[][] testPoints = {
            {-5.0, -0.0000888178},
            {-3.0, -0.0131550316},
            {-2.0, -0.1711006461},
            {-1.0, -0.3520653268},
            {0.0,   0.8673250706},
            {1.0,  -0.3520653268},
            {2.0,  -0.1711006461},
            {3.0,  -0.0131550316},
            {5.0,  -0.0000888178}
        };
        
        for (double[] point : testPoints) {
            double t = point[0];
            double matlab = point[1];
            double calc = C * (1 - (t/a)*(t/a)) * Math.exp(-(t/a)*(t/a)/2);
            System.out.printf("t=%5.1f: MATLAB=%13.10f, Calc=%13.10f, Error=%.2e\n", 
                t, matlab, calc, Math.abs(matlab - calc));
        }
        
        // The scaling factor relative to our implementation
        System.out.println("\nScaling factor to apply: " + (1.0 / a));
    }
}
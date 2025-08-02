public class MATLABMexihatReverse {
    public static void main(String[] args) {
        // MATLAB mexihat function documentation says:
        // Mexican hat wavelet on the interval [-5,5]
        // Let's check if they use a scaling that maps [-5,5] to effective [-a,a]
        
        // From MATLAB documentation, mexihat uses:
        // psi(x) = (2/(sqrt(3)*pi^(1/4))) * (1 - x^2) * exp(-x^2/2)
        // on the interval LB = -5 and UB = 5
        
        // But they might scale x internally
        // Looking at the values, at t=0 it matches exactly
        // But at other points it doesn't
        
        // Let's check the documented MATLAB formula
        double[][] matlabRef = {
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
        
        // Try with effective support scaling
        // MATLAB mexihat has effective support in [-5, 5]
        // which should correspond to about [-3.5σ, 3.5σ] for a Gaussian
        double effectiveSigma = 5.0 / 3.5; // approximately 1.43
        
        System.out.println("Testing with effective σ = " + effectiveSigma);
        double C = 2.0 / (Math.sqrt(3.0) * Math.pow(Math.PI, 0.25));
        
        for (double[] point : matlabRef) {
            double t = point[0];
            double matlab = point[1];
            double x = t / effectiveSigma;
            double calc = C * (1 - x*x) * Math.exp(-x*x/2) / effectiveSigma;
            System.out.printf("t=%5.1f: MATLAB=%13.10f, Calc=%13.10f, Error=%.2e\n", 
                t, matlab, calc, Math.abs(matlab - calc));
        }
        
        // Actually, let me check MATLAB's exact source
        // They use this exact scaling
        double LB = -8.0;
        double UB = 8.0;
        double N = 10; // They typically use more points, but principle is same
        
        System.out.println("\nMATLAB mexihat scaling factor:");
        System.out.println("If LB=-8, UB=8, then scaling is different");
        
        // From reverse engineering, MATLAB uses LB=-8, UB=8
        double scaleFactor = 8.0 / 5.0; // 1.6
        System.out.println("Scale factor: " + scaleFactor);
        
        System.out.println("\nTesting with scale factor " + scaleFactor + ":");
        for (double[] point : matlabRef) {
            double t = point[0];
            double matlab = point[1];
            double x = t / scaleFactor;
            double calc = C * (1 - x*x) * Math.exp(-x*x/2) / scaleFactor;
            System.out.printf("t=%5.1f: MATLAB=%13.10f, Calc=%13.10f, Error=%.2e\n", 
                t, matlab, calc, Math.abs(matlab - calc));
        }
    }
}
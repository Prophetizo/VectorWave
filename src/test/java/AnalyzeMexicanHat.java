public class AnalyzeMexicanHat {
    public static void main(String[] args) {
        // MATLAB mexihat function uses a specific parameterization
        // Let's reverse engineer it
        
        // Standard Mexican Hat: ψ(t) = (2/(√3 * π^(1/4))) * (1 - t²) * exp(-t²/2)
        // MATLAB might use: ψ(t) = C * (1 - (t/σ)²) * exp(-(t/σ)²/2) / σ
        
        double C = 2.0 / (Math.sqrt(3.0) * Math.pow(Math.PI, 0.25));
        
        // Try different sigma values
        System.out.println("Testing different sigma values:");
        
        for (double sigma = 1.0; sigma <= 3.0; sigma += 0.1) {
            double t = -1.0;
            double value = C * (1 - (t/sigma)*(t/sigma)) * Math.exp(-(t/sigma)*(t/sigma)/2) / sigma;
            
            if (Math.abs(value - (-0.3520653268)) < 1e-6) {
                System.out.printf("Found match at σ=%.2f: value=%.10f\n", sigma, value);
            }
        }
        
        // Let's check if MATLAB uses a factor of sqrt(5/4)
        double testSigma = Math.sqrt(5.0/4.0);
        System.out.println("\nTesting with σ = √(5/4) = " + testSigma);
        
        double[][] testPoints = {
            {-5.0, -0.0000888178},
            {-3.0, -0.0131550316},
            {-2.0, -0.1711006461},
            {-1.0, -0.3520653268},
            {0.0,   0.8673250706}
        };
        
        for (double[] point : testPoints) {
            double t = point[0];
            double matlab = point[1];
            double calc = C * (1 - (t/testSigma)*(t/testSigma)) * 
                         Math.exp(-(t/testSigma)*(t/testSigma)/2) / testSigma;
            System.out.printf("t=%.1f: MATLAB=%.10f, Calc=%.10f, Diff=%.2e\n", 
                t, matlab, calc, Math.abs(matlab - calc));
        }
        
        // Actually, MATLAB mexihat might use LB = -5, UB = 5 range
        // And scale the input accordingly
        System.out.println("\nChecking if MATLAB scales input to [-1, 1] range:");
        double LB = -5.0;
        double UB = 5.0;
        
        for (double[] point : testPoints) {
            double t = point[0];
            double matlab = point[1];
            
            // Map t from [-5,5] to standard range
            double scaledT = t / 2.425; // approximately sqrt(5.88)
            double calc = C * (1 - scaledT*scaledT) * Math.exp(-scaledT*scaledT/2);
            System.out.printf("t=%.1f (scaled=%.3f): MATLAB=%.10f, Calc=%.10f, Diff=%.2e\n", 
                t, scaledT, matlab, calc, Math.abs(matlab - calc));
        }
    }
}
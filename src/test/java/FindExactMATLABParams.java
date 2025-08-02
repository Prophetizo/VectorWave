public class FindExactMATLABParams {
    public static void main(String[] args) {
        // From MATLAB documentation and testing, mexihat uses:
        // Standard interval [-5, 5] mapped to effective support
        
        // Let's work backwards from known values
        double matlabAt0 = 0.8673250706;
        double matlabAt1 = -0.3520653268;
        double matlabAt2 = -0.1711006461;
        
        // At t=0: C * 1 = 0.8673250706, so C = 0.8673250706
        double C = matlabAt0;
        
        // At t=1: C * (1 - (1/σ)²) * exp(-(1/σ)²/2) = -0.3520653268
        // So: 0.8673250706 * (1 - 1/σ²) * exp(-1/(2σ²)) = -0.3520653268
        // (1 - 1/σ²) * exp(-1/(2σ²)) = -0.3520653268 / 0.8673250706 = -0.4060036952
        
        // Let's solve for σ
        double target = matlabAt1 / matlabAt0;
        System.out.println("Target ratio: " + target);
        
        // Try different sigma values
        for (double sigma = 0.5; sigma <= 3.0; sigma += 0.0001) {
            double value = (1 - 1/(sigma*sigma)) * Math.exp(-1/(2*sigma*sigma));
            if (Math.abs(value - target) < 1e-8) {
                System.out.println("Found sigma = " + sigma);
                
                // Verify with other points
                System.out.println("\nVerification:");
                double[][] testPoints = {
                    {0.0, 0.8673250706},
                    {1.0, -0.3520653268},
                    {2.0, -0.1711006461},
                    {3.0, -0.0131550316},
                    {5.0, -0.0000888178}
                };
                
                for (double[] point : testPoints) {
                    double t = point[0];
                    double matlab = point[1];
                    double x = t / sigma;
                    double calc = C * (1 - x*x) * Math.exp(-x*x/2);
                    System.out.printf("t=%.1f: MATLAB=%.10f, Calc=%.10f, Error=%.2e\n",
                        t, matlab, calc, Math.abs(matlab - calc));
                }
                break;
            }
        }
    }
}
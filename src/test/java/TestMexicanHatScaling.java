import ai.prophetizo.wavelet.cwt.finance.DOGWavelet;

public class TestMexicanHatScaling {
    public static void main(String[] args) {
        DOGWavelet dog2 = new DOGWavelet(2);
        
        // Test current implementation
        System.out.println("Current implementation:");
        double[][] matlabRef = {
            {-5.0, -0.0000888178},
            {-3.0, -0.0131550316},
            {-2.0, -0.1711006461},
            {-1.0, -0.3520653268},
            {0.0,   0.8673250706}
        };
        
        for (double[] point : matlabRef) {
            double t = point[0];
            double matlab = point[1];
            double calc = dog2.psi(t);
            System.out.printf("t=%5.1f: MATLAB=%13.10f, Calc=%13.10f, Error=%.2e\n", 
                t, matlab, calc, Math.abs(matlab - calc));
        }
        
        // Now let's find the exact sigma by matching at t=-1
        double targetAt1 = -0.3520653268;
        double base = 2.0 / (Math.sqrt(3.0) * Math.pow(Math.PI, 0.25));
        
        // Binary search for sigma
        double sigmaLow = 1.0;
        double sigmaHigh = 2.0;
        double tolerance = 1e-12;
        
        while (sigmaHigh - sigmaLow > tolerance) {
            double sigma = (sigmaLow + sigmaHigh) / 2;
            double x = -1.0 / sigma;
            double value = base * sigma * (1 - x*x) * Math.exp(-x*x/2) / sigma;
            // Simplifies to: base * (1 - x*x) * Math.exp(-x*x/2)
            
            if (Math.abs(value - targetAt1) < tolerance) {
                System.out.println("\nFound exact sigma = " + sigma);
                break;
            }
            
            if (value < targetAt1) {
                sigmaHigh = sigma;
            } else {
                sigmaLow = sigma;
            }
        }
        
        double finalSigma = (sigmaLow + sigmaHigh) / 2;
        System.out.println("\nFinal sigma = " + finalSigma);
        System.out.println("Should use this value in DOGWavelet.java");
    }
}
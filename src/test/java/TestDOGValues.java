import ai.prophetizo.wavelet.cwt.finance.DOGWavelet;

public class TestDOGValues {
    public static void main(String[] args) {
        DOGWavelet dog2 = new DOGWavelet(2);
        
        System.out.println("DOG2 values:");
        System.out.println("t=0: " + dog2.psi(0));
        System.out.println("t=-3: " + dog2.psi(-3));
        System.out.println("t=-5: " + dog2.psi(-5));
        
        // Manual calculation
        double norm = 0.8673250706;
        double t = -3.0;
        double manual = norm * (1 - t*t) * Math.exp(-t*t/2);
        System.out.println("\nManual calculation at t=-3:");
        System.out.println("norm * (1 - t²) * exp(-t²/2) = " + manual);
        System.out.println("(1 - 9) = " + (1 - t*t));
        System.out.println("exp(-4.5) = " + Math.exp(-t*t/2));
        
        // MATLAB values
        System.out.println("\nMATLAB values:");
        System.out.println("t=-3: -0.0131550316");
        System.out.println("Ratio: " + (-0.0131550316 / dog2.psi(-3)));
    }
}
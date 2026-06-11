package linbactsim.resources;

import java.awt.Color;

// Species are declared in canonical order: VN, MM, PP, VF, EC.
public class BacteriumSpecies {

    // -------------------------------------------------------------------------
    // Canonical species constants
    // Order must always be: VN, MM, PP, VF, EC
    // Colors follow matplotlib tab10: C0 blue, C3 red, C8 yellow, C4 purple, C2 green
    // -------------------------------------------------------------------------
    public static final BacteriumSpecies VN = new BacteriumSpecies(
            "VN",
            3.742088, 1.783831, 3.048342, 0.231857,
            new double[][] {
                {-0.0036,  1.2507,  0.9317},
                { 0.0715,  2.8665,  3.8596},
                { 0.0071,  7.8100, 12.8844},
                {-0.0528,  2.0906,  4.7022},
                { 0.0068, 40.2948, 45.2815}
            },
            8, 3,
            new Color(31, 119, 180));

    // MC and MM refer to the same species (different abbreviation conventions).
    public static final BacteriumSpecies MM = new BacteriumSpecies(
            "MM",
            10.50237, 1.81222, 4.581969, 0.52014,
            new double[][] {
                { 0.0153,  2.5950,  1.9702},
                {-0.0040, 11.3104,  6.4047},
                { 0.0186, 14.9915, 33.8388},
                {-0.0095, 28.9726, 14.7796}
            },
            67, 18,
            new Color(214, 39, 40));

    public static final BacteriumSpecies PP = new BacteriumSpecies(
            "PP",
            3.331379, 1.403308, 3.008067, 0.184833,
            new double[][] {
                { 0.0122,  0.5554,  1.5147},
                { 0.0136,  3.4549,  3.8106},
                { 0.0036,  9.3358,  2.6879},
                { 0.0010, 27.6339,  3.2085},
                { 0.0087, 30.0462,  9.8569},
                { 0.0108, 56.5761, 17.3077},
                { 0.0099, 14.2141,  7.0088}
            },
            26, 11,
            new Color(188, 189, 34));

    public static final BacteriumSpecies VF = new BacteriumSpecies(
            "VF",
            3.189115, 1.365345, 2.285821, 0.172073,
            new double[][] {
                {-0.2726,  1.1716,  1.5535},
                {-0.0054, 19.2327, 19.7369},
                { 0.3078,  1.0795,  1.6279}
            },
            15, 6,
            new Color(148, 103, 189));

    public static final BacteriumSpecies EC = new BacteriumSpecies(
            "EC",
            3.367212, 1.245058, 3.271016, 0.210141,
            new double[][] {
                { 0.0385,  2.0206,  1.5073},
                { 0.0097,  4.5895,  4.0643},
                { 0.0083, 37.4501, 18.5767},
                { 0.0060, 11.4156, 10.8695}
            },
            8, 2,
            new Color(44, 160, 44));

    // Ordered array for GUI iteration — always VN, MM, PP, VF, EC
    public static final BacteriumSpecies[] DEFAULT_SPECIES = { VN, MM, PP, VF, EC };

    // -------------------------------------------------------------------------
    // Instance fields
    // -------------------------------------------------------------------------
    private final String name;
    private final double k, lambda, multiplier, baseline;
    // forceCoeffs: each row is {a, b, c} for one Gaussian term.
    // F(x) = kBT * sum(-2a(x-b)/c^2 * exp(-((x-b)/c)^2)) / sum(a * exp(-((x-b)/c)^2))
    // Positive F → force away from wall; negative F → force toward wall.
    private final double[][] forceCoeffs;
    private final double velocity;
    private final double velocityStdDev;
    private final Color color;

    public BacteriumSpecies(String name,
                            double k, double lambda, double multiplier, double baseline,
                            double[][] forceCoeffs,
                            double velocity, double velocityStdDev,
                            Color color) {
        this.name           = name;
        this.k              = k;
        this.lambda         = lambda;
        this.multiplier     = multiplier;
        this.baseline       = baseline;
        this.forceCoeffs    = forceCoeffs;
        this.velocity       = velocity;
        this.velocityStdDev = velocityStdDev;
        this.color          = color;
    }

    // -------------------------------------------------------------------------
    // Weibull getters
    // -------------------------------------------------------------------------
    public String getName()       { return name; }
    public double getK()          { return k; }
    public double getLambda()     { return lambda; }
    public double getMultiplier() { return multiplier; }
    public double getBaseline()   { return baseline; }

    // -------------------------------------------------------------------------
    // Force model
    // -------------------------------------------------------------------------
    public double[][] getForceCoeffs() { return forceCoeffs; }

    // Inverse-Boltzmann force at distance x from the wall (kBT = 1).
    // Derived from the negative gradient of a sum-of-Gaussians free-energy fit.
    public double forceFunction(double x) {
        double num = 0.0, den = 0.0;
        for (double[] c : forceCoeffs) {
            double z = (x - c[1]) / c[2];
            double g = Math.exp(-z * z);
            num += -2.0 * c[0] * (x - c[1]) / (c[2] * c[2]) * g;
            den += c[0] * g;
        }
        return num / den;
    }

    // -------------------------------------------------------------------------
    // Velocity getters
    // -------------------------------------------------------------------------
    public double getVelocity()       { return velocity; }
    public double getVelocityStdDev() { return velocityStdDev; }

    public Color getColor() { return color; }

    @Override public String toString() { return name; }
}

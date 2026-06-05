package linbactsim.resources;

import java.awt.Color;

// Source: SURE.BacteriumSpecies (direct carry) + species constants from SURE.Main.
// Species are declared in canonical order: VN, MM, PP, VF, EC.
public class BacteriumSpecies {

    // -------------------------------------------------------------------------
    // Canonical species constants — Source: SURE.Main speciesList (~lines 76–82)
    // Order must always be: VN, MM, PP, VF, EC
    // -------------------------------------------------------------------------
    public static final BacteriumSpecies VN = new BacteriumSpecies(
            "VN", 3.742088, 1.783831, 3.048342, 0.231857, Color.ORANGE);

    public static final BacteriumSpecies MM = new BacteriumSpecies(
            "MM", 10.50237, 1.81222, 4.581969, 0.52014, Color.MAGENTA);

    public static final BacteriumSpecies PP = new BacteriumSpecies(
            "PP", 3.331379, 1.403308, 3.008067, 0.184833, Color.BLUE);

    public static final BacteriumSpecies VF = new BacteriumSpecies(
            "VF", 3.189115, 1.365345, 2.285821, 0.172073, Color.GREEN);

    public static final BacteriumSpecies EC = new BacteriumSpecies(
            "EC", 3.367212, 1.245058, 3.271016, 0.210141, Color.RED);

    // Ordered array for GUI iteration — always VN, MM, PP, VF, EC
    public static final BacteriumSpecies[] DEFAULT_SPECIES = { VN, MM, PP, VF, EC };

    // -------------------------------------------------------------------------
    // Instance fields — Source: SURE.BacteriumSpecies fields
    // -------------------------------------------------------------------------
    private final String name;
    private final double k, lambda, multiplier, baseline;
    private final Color color;

    // Source: SURE.BacteriumSpecies(String, double, double, double, double, Color)
    public BacteriumSpecies(String name, double k, double lambda,
                             double multiplier, double baseline, Color color) {
        this.name       = name;
        this.k          = k;
        this.lambda     = lambda;
        this.multiplier = multiplier;
        this.baseline   = baseline;
        this.color      = color;
    }

    // Source: SURE.BacteriumSpecies getters
    public String getName()       { return name; }
    public double getK()          { return k; }
    public double getLambda()     { return lambda; }
    public double getMultiplier() { return multiplier; }
    public double getBaseline()   { return baseline; }
    public Color  getColor()      { return color; }

    // Source: SURE.BacteriumSpecies#toString()
    @Override public String toString() { return name; }
}

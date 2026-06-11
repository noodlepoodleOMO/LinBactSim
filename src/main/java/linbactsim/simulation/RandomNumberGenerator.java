package linbactsim.simulation;

import java.util.Random;

// Source: SURE.RandomNumberGenerator (direct carry, package move only)
public class RandomNumberGenerator {

    private static Random rand = new Random();

    // Source: SURE.RandomNumberGenerator#setSeed(long)
    public static void setSeed(long seed) {
        rand = new Random(seed);
    }

    // Source: SURE.RandomNumberGenerator#getRandomPosition(int)
    public static int getRandomPosition(int upperBound) {
        return rand.nextInt(upperBound);
    }

    // Source: SURE.RandomNumberGenerator#getPositionNoise(double, int)
    public static double getPositionNoise(double velocity, int dt) {
        double maxDisplacement = velocity * dt;
        int max = (int) Math.round(maxDisplacement);
        return rand.nextInt(2 * max + 1) - max;
    }

    // Samples velocity noise ~ N(0, sigma²) truncated at ±3σ.
    // sigma is the literal standard deviation (not a 3σ range — differs from SURE convention).
    public static double getDisplacementNoise(double sigma) {
        if (sigma <= 0) return 0.0;
        double noise;
        do {
            noise = rand.nextGaussian();
        } while (Math.abs(noise) > 3.0);
        return noise * sigma;
    }

    // Source: SURE.RandomNumberGenerator#getAngleNoise(double)
    // Returns a random angle perturbation in [-upperBound, +upperBound], sampled as
    // N(0, (upperBound/3)²) truncated to the bounds.
    public static double getAngleNoise(double upperBound) {
        if (upperBound <= 0) return 0.0;
        double stdDev = upperBound / 3.0;
        double noise;
        do {
            noise = rand.nextGaussian() * stdDev;
        } while (Math.abs(noise) > upperBound);
        return noise;
    }
}

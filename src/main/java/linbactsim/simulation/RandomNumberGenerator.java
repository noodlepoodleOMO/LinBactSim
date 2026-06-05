package linbactsim.simulation;

import java.util.Random;

// Source: SURE.RandomNumberGenerator (direct carry, package move only)
public class RandomNumberGenerator {

    // Source: SURE.RandomNumberGenerator#rand
    private static Random rand = new Random();

    // Source: SURE.RandomNumberGenerator#setSeed(long)
    public static void setSeed(long seed) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RandomNumberGenerator#getRandomPosition(int)
    public static int getRandomPosition(int upperBound) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RandomNumberGenerator#getPositionNoise(double, int)
    public static double getPositionNoise(double velocity, int dt) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RandomNumberGenerator#getDisplacementNoise(double)
    public static double getDisplacementNoise(double range3StdDev) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RandomNumberGenerator#getAngleNoise(double)
    public static double getAngleNoise(double upperBound) {
        throw new UnsupportedOperationException("TODO");
    }
}

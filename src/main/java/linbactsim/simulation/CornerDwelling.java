package linbactsim.simulation;

// Shared by all MovementModel implementations' collision handling: decides
// how much to shorten the post-collision slide when a bacterium hits a wall
// close to head-on, so it dwells at obvious corners instead of gliding
// straight through them.
public final class CornerDwelling {

    private CornerDwelling() {}

    // travelDir: the bacterium's direction of travel (radians, atan2 convention)
    // wallTangentRad: the wall's local tangent-line angle at the collision pixel
    //                 (radians, mod PI -- a line has no direction, only an orientation)
    // thresholdRad: how close to perpendicular counts as a head-on hit
    // dwellFactor: multiplier applied to the remaining slide distance on a head-on hit
    //
    // Returns dwellFactor on a head-on hit, 1.0 (no change) otherwise.
    public static double headOnFactor(double travelDir, double wallTangentRad,
                                      double thresholdRad, double dwellFactor) {
        double diff = angleModPi(travelDir - wallTangentRad);
        double perpDeviation = Math.abs(diff - Math.PI / 2.0);
        return (perpDeviation <= thresholdRad) ? dwellFactor : 1.0;
    }

    private static double angleModPi(double angle) {
        double m = angle % Math.PI;
        if (m < 0) m += Math.PI;
        return m;
    }
}

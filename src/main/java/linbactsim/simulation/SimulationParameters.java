package linbactsim.simulation;

// Holds all user-configurable simulation parameters.
// Source: scattered JTextField values in SURE.Main (leftPanel fields ~lines 445–483).
public class SimulationParameters {

    // Source: SURE.Main durationField, dtField, stepField
    private int duration;
    private int dt;
    private int maxSteps; // derived: duration / dt, or set directly

    // Source: SURE.Main velocityField, stdDevField, noiseField
    private double velocity;
    private double stdDev;
    private double angleNoise;

    // Source: SURE.Main wMemoryField, wNoiseWeightField, wWallField
    private double wMemory = 0.70;
    private double wNoise  = 0.10;
    private double wWall   = 0.20;

    // Corner-dwelling collision parameters — see CornerDwelling.headOnFactor().
    private double dwellThresholdDeg = 20;
    private double dwellFactor       = 0.3;

    public SimulationParameters() {}

    // Source: SURE.Main#parseIntOrDefault(String, int)
    public static int parseIntOrDefault(String text, int defaultVal) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Source: SURE.Main#parseOrDefault(String, double)
    public static double parseOrDefault(String text, double defaultVal) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Parses a comma-separated list of integers (e.g. "1, 2, 0"). Returns
    // null if text is blank (meaning "not specified"); throws
    // NumberFormatException if any token fails to parse.
    public static int[] parseIntCsv(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String[] parts = text.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    // Source: SURE.Main start-button anonymous ActionListener init block
    // Computes maxSteps from duration+dt or falls back to explicit step count.
    public int getMaxSteps() {
        if (duration > 0 && dt > 0) return duration / dt;
        return maxSteps;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public int    getDuration()    { return duration; }
    public int    getDt()          { return dt; }
    public double getVelocity()    { return velocity; }
    public double getStdDev()      { return stdDev; }
    public double getAngleNoise()  { return angleNoise; }
    public double getWMemory()     { return wMemory; }
    public double getWNoise()      { return wNoise; }
    public double getWWall()       { return wWall; }
    public double getDwellThresholdDeg() { return dwellThresholdDeg; }
    public double getDwellFactor()       { return dwellFactor; }

    public void setDuration(int duration)        { this.duration = duration; }
    public void setDt(int dt)                    { this.dt = dt; }
    public void setMaxSteps(int maxSteps)        { this.maxSteps = maxSteps; }
    public void setVelocity(double velocity)     { this.velocity = velocity; }
    public void setStdDev(double stdDev)         { this.stdDev = stdDev; }
    public void setAngleNoise(double angleNoise) { this.angleNoise = angleNoise; }
    public void setWMemory(double wMemory)       { this.wMemory = wMemory; }
    public void setWNoise(double wNoise)         { this.wNoise = wNoise; }
    public void setWWall(double wWall)           { this.wWall = wWall; }
    public void setDwellThresholdDeg(double d)   { this.dwellThresholdDeg = d; }
    public void setDwellFactor(double f)         { this.dwellFactor = f; }
}

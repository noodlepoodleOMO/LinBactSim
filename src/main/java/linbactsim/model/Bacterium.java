package linbactsim.model;

import linbactsim.resources.BacteriumSpecies;
import linbactsim.simulation.RandomNumberGenerator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

// Source: SURE.Bacterium
// Movement physics (weibullPDF, directionWithMemory, displacement, etc.)
// have been extracted to simulation.WeibullModel/ForceModel. Bacterium is now a
// pure state container; the simulation runner calls the MovementModel.
public class Bacterium {

    // Source: SURE.Bacterium fields
    private int length, width;
    private double weibullPixelSize = 0.25;
    private double forcePixelSize   = 1.0;
    private BacteriumSpecies species;
    private double noise;
    private boolean exited;
    private int time;
    private int trajectoryLength;

    // Current position as continuous doubles (always at a pixel centre in practice,
    // since all movement goes through Bresenham). Kept for potential future use.
    private double continuousRow, continuousCol;

    // Discrete (pixel-grid) position — rounded from continuous; used for
    // wall/exit lookup, density counting, and rendering/animation.
    private int[] position;
    private List<int[]> trajectory;

    // Heading state — read/written by WeibullModel/ForceModel during each step
    // Source: SURE.Bacterium#headingRow, headingCol, headingInitialized
    private double headingRow, headingCol;
    private boolean headingInitialized;

    // Direction blend weights — passed to WeibullModel/ForceModel.directionWithMemory()
    // Source: SURE.Bacterium#wMemory, wNoise, wWall
    private double wMemory, wNoise, wWall;

    // ---- Per-step vector debug info (written by WeibullModel/ForceModel each step) ----
    private String   lastModelType;
    private double[] lastRawDistances = new double[4]; // [up,down,right,left] distances
    private double[] lastRawValues    = new double[4]; // Weibull PDF or Force values
    private double[] lastWallVector   = new double[2]; // normalized [row, col]
    private double[] lastNoiseVector  = new double[2]; // normalized [row, col]
    private boolean  lastVectorsSet   = false;

    // ---- Pending (pre-sampled) values — consumed on first continuation step ----
    private double[] pendingNoiseVector  = null;
    private Double   pendingDisplacement = null;

    // ---- Probe result — predicted next-step outcome (written by probeFullStep) ----
    private int[]   probeNextPos      = null;
    private boolean probeHadCollision = false;
    private int[]   probeLastFreePos  = null;
    private int[]   probeSlidePos     = null;
    private double  lastSampledDisplacement = 0.0;
    private String  probeSlideInfo          = null;

    // Source: SURE.Bacterium(int, int, double, int, int, double, double, BacteriumSpecies)
    // velocity and stdDev are now fixed per-species; they are no longer constructor params.
    public Bacterium(int length, int width, int row, int col, double noise,
                     BacteriumSpecies species) {
        this.length = length;
        this.width = width;
        this.noise = noise;
        this.species = species;
        this.exited = false;
        this.trajectoryLength = 0;
        this.time = 0;
        this.trajectory = new ArrayList<>();
        setContinuousPosition(row, col);
        recordPosition(row, col);
    }

    // Source: SURE.Bacterium(int, int, double, double, Maze, double, BacteriumSpecies)
    // Places bacterium at a random non-wall position in the maze.
    public Bacterium(int length, int width, Maze maze, double noise,
                     BacteriumSpecies species) {
        this.length = length;
        this.width = width;
        this.noise = noise;
        this.species = species;
        this.exited = false;
        this.trajectoryLength = 0;
        this.time = 0;
        this.trajectory = new ArrayList<>();
        int row, col;
        do {
            row = RandomNumberGenerator.getRandomPosition(maze.getNumRows());
            col = RandomNumberGenerator.getRandomPosition(maze.getNumCols());
        } while (!maze.isValid(row, col) || maze.isWall(row, col));
        setContinuousPosition(row, col);
        recordPosition(row, col);
        maze.getPixel(row, col).addCount();
    }

    // Source: SURE.Bacterium#ensureHeadingInitialized()
    public void ensureHeadingInitialized() {
        if (headingInitialized) return;
        headingRow = 0.0;
        headingCol = 0.0;
        headingInitialized = true;
    }

    // Source: SURE.Bacterium#setDirectionWeights(double, double, double)
    public void setDirectionWeights(double wMemory, double wNoise, double wWall) {
        double sum = wMemory + wNoise + wWall;
        if (sum <= 1e-9) return;
        this.wMemory = wMemory / sum;
        this.wNoise  = wNoise  / sum;
        this.wWall   = wWall   / sum;
    }

    // -------------------------------------------------------------------------
    // Heading accessors (used by WeibullModel/ForceModel)
    // -------------------------------------------------------------------------

    public double getHeadingRow()        { return headingRow; }
    public double getHeadingCol()        { return headingCol; }
    public boolean isHeadingInitialized(){ return headingInitialized; }

    public void setHeading(double row, double col) {
        this.headingRow = row;
        this.headingCol = col;
        this.headingInitialized = true;
    }

    public double getWMemory() { return wMemory; }
    public double getWNoise()  { return wNoise; }
    public double getWWall()   { return wWall; }

    // -------------------------------------------------------------------------
    // Position
    // -------------------------------------------------------------------------

    public double getContinuousRow() { return continuousRow; }
    public double getContinuousCol() { return continuousCol; }

    // Sets continuous position and synchronises the discrete position by rounding.
    public void setContinuousPosition(double row, double col) {
        this.continuousRow = row;
        this.continuousCol = col;
        this.position = new int[]{ (int) Math.round(row), (int) Math.round(col) };
    }

    // Appends current pixel to the integer trajectory.
    public void recordPosition(double row, double col) {
        trajectory.add(new int[]{ (int) Math.round(row), (int) Math.round(col) });
    }

    public int[] getPosition() { return position; }

    // Use only when the caller already has integer coordinates (e.g. during
    // collision correction). Prefer setContinuousPosition in normal stepping.
    public void setPosition(int[] position)   { this.position = position; }
    public void setPosition(int row, int col) { this.position = new int[]{ row, col }; }

    // -------------------------------------------------------------------------
    // Trajectory / time bookkeeping
    // Source: SURE.Bacterium
    // -------------------------------------------------------------------------

    public void addToTrajectory(int[] pos)   { trajectory.add(pos); }
    public void addTime(int dt)              { this.time += dt; }
    public void addTrajectoryLength(int n)   { this.trajectoryLength += n; }
    public List<int[]> getTrajectory()       { return trajectory; }

    // -------------------------------------------------------------------------
    // State setters (used by WeibullModel/ForceModel / SimulationRunner)
    // -------------------------------------------------------------------------

    public void setExited(boolean exited) { this.exited = exited; }

    // -------------------------------------------------------------------------
    // Getters — Source: SURE.Bacterium
    // -------------------------------------------------------------------------

    public int              getLength()          { return length; }
    public int              getWidth()           { return width; }
    public double           getVelocity()        { return species.getVelocity(); }
    public double           getStdDev()          { return species.getVelocityStdDev(); }
    public double           getNoise()           { return noise; }
    public int              getTime()            { return time; }
    public int              getTrajectoryLength(){ return trajectoryLength; }
    public boolean          hasExited()          { return exited; }
    public BacteriumSpecies getSpecies()         { return species; }
    public Color            getColor()           { return species.getColor(); }
    public double           getWeibullPixelSize() { return weibullPixelSize; }
    public double           getForcePixelSize()   { return forcePixelSize; }

    // Weibull parameter pass-throughs from species
    public double getK()          { return species.getK(); }
    public double getLambda()     { return species.getLambda(); }
    public double getMultiplier() { return species.getMultiplier(); }
    public double getBaseline()   { return species.getBaseline(); }

    // Force model: inverse-Boltzmann sum-of-Gaussians at distance x from the wall.
    public double forceAtDistance(double x) { return species.forceFunction(x); }

    // -------------------------------------------------------------------------
    // Per-step vector debug info
    // -------------------------------------------------------------------------

    public void setLastVectorInfo(String modelType,
                                  double[] distances, double[] rawValues,
                                  double wallRow, double wallCol,
                                  double noiseRow, double noiseCol) {
        this.lastModelType = modelType;
        System.arraycopy(distances, 0, this.lastRawDistances, 0, 4);
        System.arraycopy(rawValues, 0, this.lastRawValues,    0, 4);
        this.lastWallVector[0]  = wallRow;  this.lastWallVector[1]  = wallCol;
        this.lastNoiseVector[0] = noiseRow; this.lastNoiseVector[1] = noiseCol;
        this.lastVectorsSet = true;
    }

    public boolean  isLastVectorsSet()    { return lastVectorsSet; }
    public String   getLastModelType()    { return lastModelType; }
    public double[] getLastRawDistances() { return lastRawDistances; }
    public double[] getLastRawValues()    { return lastRawValues; }
    public double[] getLastWallVector()   { return lastWallVector; }
    public double[] getLastNoiseVector()  { return lastNoiseVector; }

    // -------------------------------------------------------------------------
    // Pending noise / displacement (for exact continuity on first resumed step)
    // -------------------------------------------------------------------------

    public boolean hasPendingNoise() { return pendingNoiseVector != null; }

    public void setPendingNoise(double row, double col) {
        pendingNoiseVector = new double[]{row, col};
    }

    public double[] consumePendingNoise() {
        double[] v = pendingNoiseVector;
        pendingNoiseVector = null;
        return v;
    }

    public boolean hasPendingDisplacement() { return pendingDisplacement != null; }

    public void setPendingDisplacement(double dist) { pendingDisplacement = dist; }

    public double consumePendingDisplacement() {
        double d = pendingDisplacement;
        pendingDisplacement = null;
        return d;
    }

    // -------------------------------------------------------------------------
    // Probe result (predicted next-step outcome)
    // -------------------------------------------------------------------------

    public void setProbeResult(int nextRow, int nextCol,
                               boolean collision, int[] lastFree, int[] slide) {
        probeNextPos      = new int[]{nextRow, nextCol};
        probeHadCollision = collision;
        probeLastFreePos  = lastFree;
        probeSlidePos     = slide;
    }

    public int[]   getProbeNextPos()      { return probeNextPos; }
    public boolean isProbeHadCollision()  { return probeHadCollision; }
    public int[]   getProbeLastFreePos()  { return probeLastFreePos; }
    public int[]   getProbeSlidePos()     { return probeSlidePos; }

    public void   setLastSampledDisplacement(double d) { this.lastSampledDisplacement = d; }
    public double getLastSampledDisplacement()         { return lastSampledDisplacement; }

    public void   setProbeSlideInfo(String s) { this.probeSlideInfo = s; }
    public String getProbeSlideInfo()          { return probeSlideInfo; }

    // ---- One-shot noise burst after concave-corner collision ----
    private boolean concaveNoiseBurst = false;

    public void    scheduleConcaveNoiseBurst() { this.concaveNoiseBurst = true; }
    public boolean hasConcaveNoiseBurst()      { return concaveNoiseBurst; }
    public void    consumeConcaveNoiseBurst()  { this.concaveNoiseBurst = false; }

    // ---- Probed wall pixels (set by probeFullStep for display) ----
    private List<int[]> lastProbedWallPixels = null;

    public void        setLastProbedWallPixels(List<int[]> pixels) { this.lastProbedWallPixels = pixels; }
    public List<int[]> getLastProbedWallPixels()                   { return lastProbedWallPixels; }
}

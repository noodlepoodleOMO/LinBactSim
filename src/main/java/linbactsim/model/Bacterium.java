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
}

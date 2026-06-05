package linbactsim.model;

import linbactsim.resources.BacteriumSpecies;

import java.awt.Color;
import java.util.List;

// Source: SURE.Bacterium
// Movement physics (weibullPDF, directionWithMemory, displacement, etc.)
// have been extracted to simulation.CurrentModel. Bacterium is now a
// pure state container; the simulation runner calls the MovementModel.
public class Bacterium {

    // Source: SURE.Bacterium fields
    private int length, width;
    private double velocity;
    private int[] position;
    private int trajectoryLength, time;
    private List<int[]> trajectory;
    private double stdDev;
    private BacteriumSpecies species;
    private double noise;
    private boolean exited;
    private double pixelsize = 0.25;

    // Heading state — read/written by CurrentModel during each step
    // Source: SURE.Bacterium#headingRow, headingCol, headingInitialized
    private double headingRow, headingCol;
    private boolean headingInitialized;

    // Direction blend weights — passed to CurrentModel.directionWithMemory()
    // Source: SURE.Bacterium#wMemory, wNoise, wWall
    private double wMemory, wNoise, wWall;

    // Source: SURE.Bacterium(int, int, double, int, int, double, double, BacteriumSpecies)
    public Bacterium(int length, int width, double velocity,
                     int row, int col, double stdDev, double noise,
                     BacteriumSpecies species) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium(int, int, double, double, Maze, double, BacteriumSpecies)
    // Places bacterium at a random non-wall position in the maze
    public Bacterium(int length, int width, double velocity,
                     double stdDev, Maze maze, double noise,
                     BacteriumSpecies species) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#ensureHeadingInitialized()
    public void ensureHeadingInitialized() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#setDirectionWeights(double, double, double)
    public void setDirectionWeights(double wMemory, double wNoise, double wWall) {
        throw new UnsupportedOperationException("TODO");
    }

    // -------------------------------------------------------------------------
    // Heading accessors (used by CurrentModel)
    // -------------------------------------------------------------------------

    public double getHeadingRow() { return headingRow; }
    public double getHeadingCol() { return headingCol; }
    public boolean isHeadingInitialized() { return headingInitialized; }

    public void setHeading(double row, double col) {
        this.headingRow = row;
        this.headingCol = col;
        this.headingInitialized = true;
    }

    public double getWMemory() { return wMemory; }
    public double getWNoise()  { return wNoise; }
    public double getWWall()   { return wWall; }

    // -------------------------------------------------------------------------
    // Trajectory / time bookkeeping
    // Source: SURE.Bacterium
    // -------------------------------------------------------------------------

    public void addToTrajectory(int[] position) { throw new UnsupportedOperationException("TODO"); }
    public void addTime(int dt)                 { throw new UnsupportedOperationException("TODO"); }
    public void addTrajectoryLength(int n)      { throw new UnsupportedOperationException("TODO"); }

    // -------------------------------------------------------------------------
    // State setters (used by CurrentModel / SimulationRunner)
    // -------------------------------------------------------------------------

    public void setPosition(int[] position)       { throw new UnsupportedOperationException("TODO"); }
    public void setPosition(int row, int col)     { throw new UnsupportedOperationException("TODO"); }
    public void setExited(boolean exited)         { this.exited = exited; }
    public void setStdDev(double stdDev)          { this.stdDev = stdDev; }

    // -------------------------------------------------------------------------
    // Getters — Source: SURE.Bacterium
    // -------------------------------------------------------------------------

    public int[]           getPosition()        { throw new UnsupportedOperationException("TODO"); }
    public double          getVelocity()        { return velocity; }
    public double          getStdDev()          { return stdDev; }
    public double          getNoise()           { return noise; }
    public int             getTime()            { return time; }
    public int             getTrajectoryLength(){ return trajectoryLength; }
    public List<int[]>     getTrajectory()      { throw new UnsupportedOperationException("TODO"); }
    public boolean         hasExited()          { return exited; }
    public BacteriumSpecies getSpecies()        { return species; }
    public Color           getColor()           { return species.getColor(); }

    // Weibull parameter pass-throughs from species
    // Source: SURE.Bacterium#getK(), getLambda(), getMultiplier(), getBaseline()
    public double getK()          { return species.getK(); }
    public double getLambda()     { return species.getLambda(); }
    public double getMultiplier() { return species.getMultiplier(); }
    public double getBaseline()   { return species.getBaseline(); }
}

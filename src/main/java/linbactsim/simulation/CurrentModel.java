package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

// Weibull-weighted run-and-tumble movement model.
// All physics extracted from SURE.Bacterium.
public class CurrentModel implements MovementModel {

    // Source: SURE.Bacterium#move(Maze, int)
    @Override
    public void step(Bacterium bacterium, Maze maze, int dt) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#directionWithMemory(Maze, double)
    @Override
    public double computeDirection(Bacterium bacterium, Maze maze, double noiseBound) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#weibullPDF(double, double, double, double, double)
    public double weibullPDF(double x, double k, double lambda,
                             double multiplier, double baseline) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#getDistanceToWall(Maze, int, int)
    // Returns [distRow, distCol] to nearest wall in each axis direction.
    public double[] getDistanceToWall(Maze maze, int row, int col) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#applyRandomCorrection(Maze, int, int, int, int, int, double)
    public void applyRandomCorrection(Maze maze, int row, int col,
                                      int newRow, int newCol,
                                      int dt, double originalDir,
                                      Bacterium bacterium) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#displacement(int)
    public double displacement(Bacterium bacterium, int dt) {
        throw new UnsupportedOperationException("TODO");
    }

    // -------------------------------------------------------------------------
    // Static math helpers — Source: SURE.Bacterium
    // -------------------------------------------------------------------------

    // Source: SURE.Bacterium#clamp(double, double, double)
    public static double clamp(double v, double lo, double hi) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#norm(double, double)
    public static double norm(double a, double b) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Bacterium#unit(double, double)
    public static double[] unit(double a, double b) {
        throw new UnsupportedOperationException("TODO");
    }
}

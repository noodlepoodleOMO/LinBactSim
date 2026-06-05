package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

// Potential-field-based movement model — placeholder for future implementation.
// Intended to improve simulation accuracy if current (Weibull) results are
// insufficient. No equivalent in SURE.
public class PotentialModel implements MovementModel {

    @Override
    public void step(Bacterium bacterium, Maze maze, int dt) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public double computeDirection(Bacterium bacterium, Maze maze, double noiseBound) {
        throw new UnsupportedOperationException("TODO");
    }
}

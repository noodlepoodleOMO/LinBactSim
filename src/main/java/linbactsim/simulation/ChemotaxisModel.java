package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

// Chemotaxis movement model — placeholder for future incorporation of
// chemical gradient-directed motility. No equivalent in SURE.
public class ChemotaxisModel implements MovementModel {

    @Override
    public void step(Bacterium bacterium, Maze maze, int dt) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public double computeDirection(Bacterium bacterium, Maze maze, double noiseBound) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void probeFullStep(Bacterium bacterium, Maze maze, int dt) {
        throw new UnsupportedOperationException("TODO");
    }
}

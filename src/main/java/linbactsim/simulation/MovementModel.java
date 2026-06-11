package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

// Interface for pluggable movement strategies.
// Implementations: WeibullModel (Weibull-weighted), ForceModel (inverse-Boltzmann force), ChemotaxisModel.
// Source: extracted from SURE.Bacterium#move() and SURE.Bacterium#directionWithMemory()
public interface MovementModel {

    // Advance the bacterium one time step. Mutates bacterium position,
    // trajectory, heading, time, and exited flag.
    // Source: SURE.Bacterium#move(Maze, int)
    void step(Bacterium bacterium, Maze maze, int dt);

    // Compute the movement direction angle (radians) without applying it.
    // Source: SURE.Bacterium#directionWithMemory(Maze, double)
    double computeDirection(Bacterium bacterium, Maze maze, double noiseBound);
}

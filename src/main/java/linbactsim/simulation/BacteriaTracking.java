package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Queries and aggregates per-bacterium state after a simulation run.
// Source: logic extracted from SURE.Main list-button action (~lines 748–878)
// and export-successful-visit-matrix (~lines 959–976).
public class BacteriaTracking {

    private final Maze maze;

    public BacteriaTracking(Maze maze) {
        this.maze = maze;
    }

    // Returns indices (0-based) of bacteria that reached an exit.
    // Source: SURE.Main export-successful-visit-matrix filter (~lines 959–964).
    public List<Integer> getExitedIndices() {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns count of bacteria that reached an exit.
    public int getExitedCount() {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns map of speciesName → list of trajectory lengths for that species.
    // Source: SURE.Main trajectoryLengthsBySpecies computation (~lines 748–755).
    public Map<String, List<Integer>> getTrajectoryLengthsBySpecies() {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns map of speciesName → average trajectory length.
    // Source: SURE.Main average-trajectory button logic (~lines 845–878).
    public Map<String, Double> getAverageTrajectoryBySpecies() {
        throw new UnsupportedOperationException("TODO");
    }
}

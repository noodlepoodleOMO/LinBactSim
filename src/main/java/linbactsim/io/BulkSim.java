package linbactsim.io;

import linbactsim.model.Maze;
import linbactsim.simulation.SimulationParameters;

import java.io.File;
import java.io.IOException;
import java.util.List;

// Imports bulk simulation parameter sets from a file and runs them sequentially.
// New class — no equivalent in SURE.
public class BulkSim {

    // Reads a parameter file (CSV or JSON) and returns one SimulationParameters per row.
    public static List<SimulationParameters> loadParameters(File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // Runs all parameter sets against the given maze, collecting results.
    public static void run(List<SimulationParameters> paramSets, Maze maze) {
        throw new UnsupportedOperationException("TODO");
    }
}

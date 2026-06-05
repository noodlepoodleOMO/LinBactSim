package linbactsim.simulation;

import linbactsim.gui.MazePanel;
import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

import javax.swing.Timer;

// Owns and drives the simulation loop (both fast and animated modes).
// Source: SURE.Main start-button ActionListener (~lines 1164–1296).
public class SimulationRunner {

    private final MovementModel movementModel;
    private SimulationTracking tracking;
    private Timer animationTimer;

    public SimulationRunner(MovementModel movementModel) {
        this.movementModel = movementModel;
    }

    // Runs all steps synchronously with no repainting.
    // Source: SURE.Main fast-mode block (~lines 1201–1232).
    public void runFast(Maze maze, SimulationParameters params) {
        throw new UnsupportedOperationException("TODO");
    }

    // Runs one step per timer tick, repainting the panel each time.
    // Source: SURE.Main Timer-based block (~lines 1237–1293).
    public void runAnimated(Maze maze, SimulationParameters params, MazePanel panel) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Main simulationTimer[0].stop()
    public void stop() {
        if (animationTimer != null) animationTimer.stop();
    }

    // Advances every non-exited bacterium by one dt tick.
    // Source: inner loop shared by both run modes.
    private void stepAll(Maze maze, int dt) {
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            if (!b.hasExited()) movementModel.step(b, maze, dt);
        }
    }

    public SimulationTracking getTracking() { return tracking; }
}

package linbactsim.simulation;

import linbactsim.gui.MazePanel;
import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

import javax.swing.Timer;

// Owns and drives the simulation loop (both fast and animated modes).
// Source: SURE.Main start-button ActionListener (~lines 1164–1296).
public class SimulationRunner {

    private MovementModel movementModel;
    private SimulationTracking tracking;
    private Timer animationTimer;

    public SimulationRunner(MovementModel movementModel) {
        this.movementModel = movementModel;
    }

    public void setMovementModel(MovementModel movementModel) {
        this.movementModel = movementModel;
    }

    public MovementModel getMovementModel() { return movementModel; }

    // Runs all steps synchronously with no repainting.
    // Source: SURE.Main fast-mode block (~lines 1201–1232).
    public void runFast(Maze maze, SimulationParameters params) {
        int dt       = params.getDt();
        int maxSteps = params.getMaxSteps();
        for (int step = 0; step < maxSteps; step++) {
            stepAll(maze, dt);
            boolean allDone = true;
            for (int i = 0; i < maze.getBacteriaCount(); i++)
                if (!maze.getBacterium(i).hasExited()) { allDone = false; break; }
            if (allDone) break;
        }
    }

    // Runs one step per timer tick (~10 fps), repainting the panel each time.
    // Source: SURE.Main Timer-based block (~lines 1237–1293).
    public void runAnimated(Maze maze, SimulationParameters params, MazePanel panel) {
        if (animationTimer != null) animationTimer.stop();
        int dt       = params.getDt();
        int maxSteps = params.getMaxSteps();
        int[] step   = {0};
        animationTimer = new Timer(100, e -> {
            if (step[0] < maxSteps) {
                stepAll(maze, dt);
                panel.repaint();
                step[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        animationTimer.start();
    }

    // Source: SURE.Main simulationTimer[0].stop()
    public void stop() {
        if (animationTimer != null) animationTimer.stop();
    }

    // Advances every non-exited bacterium by one dt tick.
    private void stepAll(Maze maze, int dt) {
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            if (!b.hasExited()) movementModel.step(b, maze, dt);
        }
    }

    public SimulationTracking getTracking() { return tracking; }
}

package linbactsim.gui;

import linbactsim.model.Maze;
import linbactsim.analysis.RAG;
import linbactsim.simulation.BacteriaTracking;

import javax.swing.*;
import java.awt.*;

// Displays per-bacterium trajectory data in a table window, with sub-views
// for average trajectory and visit-matrix export.
// Source: SURE.Main list-button ActionListener (~lines 711–1024). Extracted
// so the bacteria table and its download actions are not buried in Main.
public class TrajectoryPanel extends JPanel {

    private final Maze maze;
    private final RAG rag; // may be null if analysis has not been run
    private BacteriaTracking tracking;

    private JTable table;
    private JScrollPane scrollPane;

    // Source: SURE.Main listButton action (~line 711) — constructs the properties table
    public TrajectoryPanel(Maze maze, RAG rag) {
        this.maze = maze;
        this.rag  = rag;
        throw new UnsupportedOperationException("TODO");
    }

    // Builds or refreshes the bacteria properties JTable.
    // Source: SURE.Main listButton action — columnNames, data array (~lines 716–731)
    public void refresh() {
        throw new UnsupportedOperationException("TODO");
    }

    // Opens a sub-frame showing average trajectory length per species.
    // Source: SURE.Main averageTrajectoryButton action (~lines 845–878)
    public void showAverageTrajectoryWindow() {
        throw new UnsupportedOperationException("TODO");
    }

    // Opens a standalone JFrame containing this panel.
    // Source: SURE.Main listFrame construction (~lines 712–714)
    public JFrame openWindow() {
        throw new UnsupportedOperationException("TODO");
    }
}

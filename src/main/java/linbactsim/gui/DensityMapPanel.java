package linbactsim.gui;

import linbactsim.model.Maze;
import linbactsim.resources.InfernoColorMap;

import javax.swing.*;
import java.awt.*;

// Renders density maps. Merges SURE.DensityMapPanel (inferno colormap) and
// SURE.OverlappedDensityMapPanel (folded/overlapped view) into one class
// with a RenderMode toggle.
public class DensityMapPanel extends JPanel {

    public enum RenderMode {
        INFERNO,    // Source: SURE.DensityMapPanel
        OVERLAPPED  // Source: SURE.OverlappedDensityMapPanel
    }

    // Source: SURE.DensityMapPanel#maze / SURE.OverlappedDensityMapPanel#maze
    private final Maze maze;
    private RenderMode renderMode;

    // Source: SURE.DensityMapPanel(Maze)
    public DensityMapPanel(Maze maze) {
        this(maze, RenderMode.INFERNO);
    }

    public DensityMapPanel(Maze maze, RenderMode mode) {
        this.maze = maze;
        this.renderMode = mode;
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.DensityMapPanel#addNotify()
    @Override public void addNotify() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.DensityMapPanel#paintComponent(Graphics) (INFERNO mode)
    //         SURE.OverlappedDensityMapPanel#paintComponent(Graphics) (OVERLAPPED mode)
    @Override protected void paintComponent(Graphics g) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.DensityMapPanel inferno rendering — maps pixel count to InfernoColorMap
    private void paintInferno(Graphics2D g2) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.OverlappedDensityMapPanel — folds four quadrants onto one,
    // renders white-to-red gradient
    private void paintOverlapped(Graphics2D g2) {
        throw new UnsupportedOperationException("TODO");
    }

    public void setRenderMode(RenderMode mode) { this.renderMode = mode; repaint(); }
    public RenderMode getRenderMode()          { return renderMode; }
}

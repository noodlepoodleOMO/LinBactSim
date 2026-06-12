package linbactsim.gui;

import linbactsim.model.Maze;
import linbactsim.resources.InfernoColorMap;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

// Renders density maps. Merges SURE.DensityMapPanel (inferno colormap) and
// SURE.OverlappedDensityMapPanel (folded/overlapped view) into one class
// with a RenderMode toggle.
public class DensityMapPanel extends JPanel {

    public enum RenderMode {
        INFERNO,    // Source: SURE.DensityMapPanel
        OVERLAPPED  // Source: SURE.OverlappedDensityMapPanel
    }

    private final Maze maze;
    private RenderMode renderMode;

    public DensityMapPanel(Maze maze) {
        this(maze, RenderMode.INFERNO);
    }

    public DensityMapPanel(Maze maze, RenderMode mode) {
        this.maze = maze;
        this.renderMode = mode;
        int cs = maze.getDisplayPixelSize();
        setPreferredSize(new Dimension(maze.getNumCols() * cs, maze.getNumRows() * cs));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (renderMode == RenderMode.OVERLAPPED) paintOverlapped(g2);
        else paintInferno(g2);
    }

    // Source: SURE.DensityMapPanel#paintComponent — inferno colour map, 95th-percentile normalisation
    private void paintInferno(Graphics2D g2) {
        int cs   = maze.getDisplayPixelSize();
        int rows = maze.getNumRows();
        int cols = maze.getNumCols();

        int[] counts = new int[rows * cols];
        int i = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                counts[i++] = maze.getPixel(r, c).getCount();

        Arrays.sort(counts);
        int p95 = counts[(int)(0.95 * (counts.length - 1))];
        if (p95 <= 0) p95 = 1;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (maze.isWall(r, c)) {
                    g2.setColor(Color.DARK_GRAY);
                } else {
                    float t = maze.getPixel(r, c).getCount() / (float) p95;
                    t = Math.max(0f, Math.min(1f, t));
                    g2.setColor(InfernoColorMap.get(t));
                }
                g2.fillRect(c * cs, r * cs, cs, cs);
                g2.setColor(Color.BLACK);
                g2.drawRect(c * cs, r * cs, cs, cs);
            }
        }
    }

    // Source: SURE.OverlappedDensityMapPanel — white-to-red gradient, 8px cells
    private void paintOverlapped(Graphics2D g2) {
        int cs   = 8;
        int rows = maze.getNumRows();
        int cols = maze.getNumCols();

        int maxCount = 1;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                maxCount = Math.max(maxCount, maze.getPixel(r, c).getCount());

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (maze.isWall(r, c)) {
                    g2.setColor(Color.DARK_GRAY);
                } else {
                    float t = maze.getPixel(r, c).getCount() / (float) maxCount;
                    t = Math.max(0f, Math.min(1f, t));
                    int green = (int)(255 * (1f - t));
                    int blue  = (int)(255 * (1f - t));
                    g2.setColor(new Color(255, green, blue));
                }
                g2.fillRect(c * cs, r * cs, cs, cs);
                g2.setColor(new Color(200, 200, 200));
                g2.drawRect(c * cs, r * cs, cs, cs);
            }
        }
    }

    public void setRenderMode(RenderMode mode) { this.renderMode = mode; repaint(); }
    public RenderMode getRenderMode()          { return renderMode; }
}

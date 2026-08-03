package linbactsim.gui;

import linbactsim.resources.InfernoColorMap;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

// Renders a static density snapshot (e.g. one bulk-analysis combo's per-pixel
// visit counts) against a fixed wall layout. Mirrors DensityMapPanel's inferno
// rendering but doesn't need a live, mutable Maze -- just counts + wall mask.
public class DensitySnapshotPanel extends JPanel {

    private final boolean[][] walls;
    private final int rows, cols, cellSize;
    private int[] counts;

    public DensitySnapshotPanel(boolean[][] walls, int rows, int cols, int cellSize) {
        this.walls = walls;
        this.rows = rows;
        this.cols = cols;
        this.cellSize = cellSize;
        this.counts = new int[rows * cols];
        setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));
    }

    public void setCounts(int[] counts) {
        this.counts = counts;
        repaint();
    }

    // Source: DensityMapPanel#paintInferno -- inferno colour map, 95th-percentile normalisation
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int[] sorted = counts.clone();
        Arrays.sort(sorted);
        int p95 = sorted[(int) (0.95 * (sorted.length - 1))];
        if (p95 <= 0) p95 = 1;

        int i = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++, i++) {
                if (walls[r][c]) {
                    g2.setColor(Color.DARK_GRAY);
                } else {
                    float t = counts[i] / (float) p95;
                    t = Math.max(0f, Math.min(1f, t));
                    g2.setColor(InfernoColorMap.get(t));
                }
                g2.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
                g2.setColor(Color.BLACK);
                g2.drawRect(c * cellSize, r * cellSize, cellSize, cellSize);
            }
        }
    }
}

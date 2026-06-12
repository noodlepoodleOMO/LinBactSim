package linbactsim.gui;

import linbactsim.analysis.RAG;
import linbactsim.model.Maze;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

// Displays the Region Adjacency Graph overlaid on the maze.
// Source: SURE.RAGGraphPanel (rename + package move; SkeletonAnalyzer → RAG)
public class RAGPanel extends JPanel {

    private final Maze maze;
    private final RAG  rag;
    private Map<Integer, double[]> centroids;

    private static final int NODE_RADIUS = 14;

    public RAGPanel(Maze maze, RAG rag) {
        this.maze = maze;
        this.rag  = rag;
        int cs = maze.getDisplayPixelSize();
        setPreferredSize(new Dimension(maze.getNumCols() * cs, maze.getNumRows() * cs));
        setBackground(Color.WHITE);
        setToolTipText(" ");
    }

    // Source: SURE.RAGGraphPanel#getToolTipText(MouseEvent)
    @Override
    public String getToolTipText(MouseEvent e) {
        ensureCentroids();
        int cs = maze.getDisplayPixelSize();
        if (rag.getAdjacency() != null) {
            for (Integer jId : rag.getAdjacency().keySet()) {
                double[] c = centroids.get(jId);
                if (c == null) continue;
                int cx = (int)(c[1] * cs + cs / 2.0);
                int cy = (int)(c[0] * cs + cs / 2.0);
                if (Math.hypot(e.getX() - cx, e.getY() - cy) <= NODE_RADIUS + 4)
                    return "Junction " + jId + "  (connected edges: " + rag.getAdjacency().get(jId) + ")";
            }
        }
        if (rag.getEdgeEndpoints() != null) {
            for (Map.Entry<Integer, List<Integer>> entry : rag.getEdgeEndpoints().entrySet()) {
                int eId = entry.getKey();
                List<Integer> ends = entry.getValue();
                double[] mc = edgeMidpoint(eId, ends);
                if (mc == null) continue;
                int mx = (int)(mc[1] * cs + cs / 2.0);
                int my = (int)(mc[0] * cs + cs / 2.0);
                if (Math.hypot(e.getX() - mx, e.getY() - my) <= 14)
                    return "Edge " + eId + "  (endpoints: " + ends + ")";
            }
        }
        return null;
    }

    // Source: SURE.RAGGraphPanel#paintComponent(Graphics)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ensureCentroids();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cs   = maze.getDisplayPixelSize();
        int rows = maze.getNumRows(), cols = maze.getNumCols();
        int[][] rm = rag.getRegionMap();

        // 1. Background: maze + light voronoi tint
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (maze.isWall(r, c)) {
                    g.setColor(new Color(40, 40, 40));
                } else {
                    int id = (rm != null) ? rm[r][c] : 0;
                    if (id != 0) {
                        Color base = rag.getRegionColor(id);
                        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 60));
                    } else {
                        g.setColor(new Color(230, 230, 230));
                    }
                }
                g.fillRect(c * cs, r * cs, cs, cs);
            }
        }

        Font labelFont = new Font("SansSerif", Font.BOLD, Math.max(9, (int)(cs * 0.7)));
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();

        // 2. Edge segments
        if (rag.getEdgeEndpoints() != null) {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Map.Entry<Integer, List<Integer>> entry : rag.getEdgeEndpoints().entrySet()) {
                int eId = entry.getKey();
                List<Integer> ends = entry.getValue();
                int[] lc = edgeLineCoords(eId, ends, cs);
                if (lc == null) continue;
                int x1 = lc[0], y1 = lc[1], x2 = lc[2], y2 = lc[3];
                g2.setColor(new Color(80, 80, 80, 200));
                g2.drawLine(x1, y1, x2, y2);
                int mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
                String label = "e" + eId;
                int lw = fm.stringWidth(label), lh = fm.getHeight();
                g2.setColor(new Color(255, 255, 255, 210));
                g2.fillRoundRect(mx - lw/2 - 3, my - lh + 2, lw + 6, lh + 1, 4, 4);
                g2.setColor(new Color(40, 40, 120));
                g2.drawString(label, mx - lw/2, my);
            }
        }

        // 3. Junction nodes
        if (rag.getAdjacency() != null) {
            for (Integer jId : rag.getAdjacency().keySet()) {
                double[] c = centroids.get(jId);
                if (c == null) continue;
                int cx = (int)(c[1] * cs + cs / 2.0);
                int cy = (int)(c[0] * cs + cs / 2.0);
                Color jc = rag.getRegionColor(jId);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(cx - NODE_RADIUS + 2, cy - NODE_RADIUS + 2, NODE_RADIUS*2, NODE_RADIUS*2);
                g2.setColor(jc);
                g2.fillOval(cx - NODE_RADIUS, cy - NODE_RADIUS, NODE_RADIUS*2, NODE_RADIUS*2);
                g2.setColor(jc.darker().darker());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - NODE_RADIUS, cy - NODE_RADIUS, NODE_RADIUS*2, NODE_RADIUS*2);
                String label = "v" + jId;
                int lw = fm.stringWidth(label);
                g2.setColor(Color.BLACK);
                g2.drawString(label, cx - lw/2, cy + fm.getAscent()/2 - 1);
            }
        }
    }

    private void ensureCentroids() {
        if (centroids == null) centroids = rag.computeRegionCentroids();
    }

    private double[] edgeMidpoint(int eId, List<Integer> ends) {
        ensureCentroids();
        int cs = maze.getDisplayPixelSize();
        int[] lc = edgeLineCoords(eId, ends, cs);
        if (lc == null) return null;
        return new double[]{(lc[1] + lc[3]) / (2.0 * cs), (lc[0] + lc[2]) / (2.0 * cs)};
    }

    private int[] edgeLineCoords(int eId, List<Integer> ends, int cs) {
        if (ends.isEmpty()) return null;
        double[] j1c = centroids.get(ends.get(0));
        if (j1c == null) return null;
        int x1 = (int)(j1c[1] * cs + cs / 2.0);
        int y1 = (int)(j1c[0] * cs + cs / 2.0);
        int x2, y2;
        if (ends.size() >= 2) {
            double[] j2c = centroids.get(ends.get(1));
            if (j2c == null) return null;
            x2 = (int)(j2c[1] * cs + cs / 2.0);
            y2 = (int)(j2c[0] * cs + cs / 2.0);
        } else {
            double[] ec = centroids.get(eId);
            if (ec == null) return null;
            x2 = (int)(ec[1] * cs + cs / 2.0);
            y2 = (int)(ec[0] * cs + cs / 2.0);
        }
        return new int[]{x1, y1, x2, y2};
    }
}

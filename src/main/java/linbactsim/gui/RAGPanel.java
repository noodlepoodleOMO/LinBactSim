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

    // Source: SURE.RAGGraphPanel fields
    private final Maze maze;
    private final RAG rag;
    private Map<Integer, double[]> centroids;

    private static final int NODE_RADIUS = 14; // Source: SURE.RAGGraphPanel#NODE_RADIUS

    // Source: SURE.RAGGraphPanel(Maze, SkeletonAnalyzer) — second param now RAG
    public RAGPanel(Maze maze, RAG rag) {
        this.maze = maze;
        this.rag  = rag;
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RAGGraphPanel#getToolTipText(MouseEvent)
    @Override public String getToolTipText(MouseEvent e) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RAGGraphPanel#paintComponent(Graphics)
    @Override protected void paintComponent(Graphics g) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RAGGraphPanel#ensureCentroids()
    private void ensureCentroids() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RAGGraphPanel#edgeMidpoint(int, List<Integer>)
    private double[] edgeMidpoint(int eId, List<Integer> ends) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.RAGGraphPanel#edgeLineCoords(int, List<Integer>, int)
    private int[] edgeLineCoords(int eId, List<Integer> ends, int cs) {
        throw new UnsupportedOperationException("TODO");
    }
}

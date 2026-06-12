package linbactsim.analysis;

import linbactsim.model.Maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

// Performs Zhang-Suen skeleton thinning and labels junction / edge components.
// Source: SURE.SkeletonAnalyzer — steps 1–4 of analyze(), plus all thinning helpers.
public class Skeleton {

    // Source: SURE.SkeletonAnalyzer fields
    private boolean[][] skeleton;
    private int[][] degree;
    private int[][] junctionLabels; // positive junction IDs (step 3)
    private int[][] edgeLabels;     // positive edge IDs internally, negated externally (step 4)
    private int numJunctions;
    private int numEdges;

    // Source: SURE.SkeletonAnalyzer#analyze() steps 1–4
    public void analyze(Maze maze) {
        int rows = maze.getNumRows();
        int cols = maze.getNumCols();

        // Step 1: ZS thinning of open (non-wall) pixels
        boolean[][] open = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                open[r][c] = !maze.isWall(r, c);
        skeleton = zhangSuenThin(open, rows, cols);

        // Step 2: Degree of each skeleton pixel (# of 8-connected skeleton neighbors)
        degree = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (skeleton[r][c])
                    degree[r][c] = skeletonDegree(r, c, rows, cols);

        // Step 3: Junction blob grouping — degree 0, 1, or 3+ pixels,
        //         connected 8-neighbors sharing one positive junction ID.
        junctionLabels = new int[rows][cols];
        numJunctions = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (!skeleton[r][c] || degree[r][c] == 2 || junctionLabels[r][c] != 0) continue;
                bfsLabel(skeleton, degree, junctionLabels, r, c, ++numJunctions, rows, cols, false);
            }

        // Step 4: Edge component labeling — degree-2 pixels only.
        //         Junction pixels act as separators (not traversed).
        //         Each component gets a unique positive label internally;
        //         exposed as a negative region ID.
        edgeLabels = new int[rows][cols];
        numEdges = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (!skeleton[r][c] || degree[r][c] != 2 || edgeLabels[r][c] != 0) continue;
                bfsLabel(skeleton, degree, edgeLabels, r, c, ++numEdges, rows, cols, true);
            }
    }

    // Source: SURE.SkeletonAnalyzer#zhangSuenThin(boolean[][], int, int)
    private boolean[][] zhangSuenThin(boolean[][] src, int rows, int cols) {
        boolean[][] img = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) img[r] = src[r].clone();
        boolean changed = true;
        List<int[]> marks = new ArrayList<>();
        while (changed) {
            changed = false;
            marks.clear();
            for (int r = 1; r < rows - 1; r++) for (int c = 1; c < cols - 1; c++) {
                if (!img[r][c]) continue;
                int[] p = zsp(img, r, c);
                int B = 0; for (int v : p) B += v;
                if (B < 2 || B > 6 || trans(p) != 1) continue;
                if (p[0] * p[2] * p[4] != 0 || p[2] * p[4] * p[6] != 0) continue;
                marks.add(new int[]{r, c});
            }
            for (int[] m : marks) { img[m[0]][m[1]] = false; changed = true; }

            marks.clear();
            for (int r = 1; r < rows - 1; r++) for (int c = 1; c < cols - 1; c++) {
                if (!img[r][c]) continue;
                int[] p = zsp(img, r, c);
                int B = 0; for (int v : p) B += v;
                if (B < 2 || B > 6 || trans(p) != 1) continue;
                if (p[0] * p[2] * p[6] != 0 || p[0] * p[4] * p[6] != 0) continue;
                marks.add(new int[]{r, c});
            }
            for (int[] m : marks) { img[m[0]][m[1]] = false; changed = true; }
        }
        return img;
    }

    // Source: SURE.SkeletonAnalyzer#zsp(boolean[][], int, int)
    // Neighbors P2..P9 clockwise from top, indices 0..7
    private int[] zsp(boolean[][] img, int r, int c) {
        return new int[]{
            img[r-1][c]   ? 1:0, img[r-1][c+1] ? 1:0,
            img[r][c+1]   ? 1:0, img[r+1][c+1] ? 1:0,
            img[r+1][c]   ? 1:0, img[r+1][c-1] ? 1:0,
            img[r][c-1]   ? 1:0, img[r-1][c-1] ? 1:0
        };
    }

    // Source: SURE.SkeletonAnalyzer#trans(int[])
    private int trans(int[] p) {
        int n = 0;
        for (int i = 0; i < p.length; i++)
            if (p[i] == 0 && p[(i + 1) % p.length] == 1) n++;
        return n;
    }

    // Source: SURE.SkeletonAnalyzer#skeletonDegree(int, int, int, int)
    private int skeletonDegree(int r, int c, int rows, int cols) {
        int n = 0;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if ((dr | dc) == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && skeleton[nr][nc]) n++;
            }
        return n;
    }

    // Source: SURE.SkeletonAnalyzer#bfsLabel(...)
    // edgeOnly=true  → traverse degree-2 pixels only (edge components)
    // edgeOnly=false → traverse non-degree-2 pixels only (junction blobs)
    private void bfsLabel(boolean[][] skel, int[][] deg, int[][] label,
                          int sr, int sc, int id, int rows, int cols,
                          boolean edgeOnly) {
        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sr, sc});
        label[sr][sc] = id;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if ((dr | dc) == 0) continue;
                    int nr = cur[0] + dr, nc = cur[1] + dc;
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                    if (!skel[nr][nc] || label[nr][nc] != 0) continue;
                    if (edgeOnly ? deg[nr][nc] != 2 : deg[nr][nc] == 2) continue;
                    label[nr][nc] = id;
                    q.add(new int[]{nr, nc});
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public boolean[][] getSkeleton()       { return skeleton; }
    public int[][]     getDegree()         { return degree; }
    public int[][]     getJunctionLabels() { return junctionLabels; }
    public int[][]     getEdgeLabels()     { return edgeLabels; }
    public int         getNumJunctions()   { return numJunctions; }
    public int         getNumEdges()       { return numEdges; }
    public boolean     hasSkeleton()       { return skeleton != null; }
}

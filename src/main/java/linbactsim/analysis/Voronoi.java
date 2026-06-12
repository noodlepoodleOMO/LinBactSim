package linbactsim.analysis;

import linbactsim.model.Maze;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

// Builds and manages the Voronoi region map from skeleton labels.
// Source: SURE.SkeletonAnalyzer — step 5 of analyze() + region management methods.
public class Voronoi {

    // positive = junction region, negative = edge region, 0 = wall/unassigned
    private int[][] regionMap;

    private Skeleton skeleton;
    private boolean isLoadedFromFile = false;

    public Voronoi(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    // Source: SURE.SkeletonAnalyzer#analyze() step 5
    public void build(Maze maze) {
        int rows = maze.getNumRows();
        int cols = maze.getNumCols();
        int[][] jLabel = skeleton.getJunctionLabels();
        int[][] eLabel = skeleton.getEdgeLabels();

        // Combine junction and edge labels into a seed map:
        // junctions keep positive IDs; edges get negated IDs.
        int[][] skRegion = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (jLabel[r][c] != 0) skRegion[r][c] = jLabel[r][c];
                else if (eLabel[r][c] != 0) skRegion[r][c] = -eLabel[r][c];

        regionMap = voronoiBFS(skRegion, rows, cols, maze);
        isLoadedFromFile = false;
    }

    // Source: SURE.SkeletonAnalyzer#voronoiBFS(int[][], int, int)
    private int[][] voronoiBFS(int[][] seed, int rows, int cols, Maze maze) {
        final int NONE = Integer.MIN_VALUE;
        int[][] map = new int[rows][cols];
        Queue<int[]> q = new ArrayDeque<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (maze.isWall(r, c)) { map[r][c] = 0; continue; }
                map[r][c] = seed[r][c] != 0 ? seed[r][c] : NONE;
                if (seed[r][c] != 0) q.add(new int[]{r, c});
            }
        int[] dr = {-1, 1, 0, 0}, dc = {0, 0, -1, 1};
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int id = map[cur[0]][cur[1]];
            for (int d = 0; d < 4; d++) {
                int nr = cur[0] + dr[d], nc = cur[1] + dc[d];
                if (maze.isValid(nr, nc) && !maze.isWall(nr, nc) && map[nr][nc] == NONE) {
                    map[nr][nc] = id;
                    q.add(new int[]{nr, nc});
                }
            }
        }
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (map[r][c] == NONE) map[r][c] = 0;
        return map;
    }

    // Source: SURE.SkeletonAnalyzer#reassignRegion(int, int, int)
    public void reassignRegion(int row, int col, int newId, Maze maze) {
        if (regionMap != null && maze.isValid(row, col) && !maze.isWall(row, col))
            regionMap[row][col] = newId;
    }

    // Source: SURE.SkeletonAnalyzer#computeRegionCentroids()
    public Map<Integer, double[]> computeRegionCentroids() {
        if (regionMap == null) return new HashMap<>();
        int rows = regionMap.length, cols = regionMap[0].length;
        Map<Integer, long[]> acc = new HashMap<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int id = regionMap[r][c];
                if (id == 0) continue;
                long[] s = acc.computeIfAbsent(id, k -> new long[3]);
                s[0] += r; s[1] += c; s[2]++;
            }
        Map<Integer, double[]> result = new HashMap<>();
        for (Map.Entry<Integer, long[]> e : acc.entrySet()) {
            long[] s = e.getValue();
            result.put(e.getKey(), new double[]{(double) s[0] / s[2], (double) s[1] / s[2]});
        }
        return result;
    }

    // Source: SURE.SkeletonAnalyzer#loadFromVoronoi(int[][])
    public void loadFromSaved(int[][] savedMap, Maze maze) {
        int rows = maze.getNumRows(), cols = maze.getNumCols();
        regionMap = new int[rows][cols];
        for (int r = 0; r < Math.min(rows, savedMap.length); r++)
            for (int c = 0; c < Math.min(cols, savedMap[r].length); c++)
                regionMap[r][c] = savedMap[r][c];
        isLoadedFromFile = true;
    }

    // -------------------------------------------------------------------------
    // Display helpers — Source: SURE.SkeletonAnalyzer
    // -------------------------------------------------------------------------

    // Junction regions get distinct hue-spread colors; edge regions are gray.
    public Color getRegionColor(int id) {
        if (id <= 0) return new Color(160, 160, 160);
        float hue = (id * 0.618033f) % 1.0f;
        return Color.getHSBColor(hue, 0.55f, 0.88f);
    }

    public String formatRegionId(int id) {
        return id > 0 ? "Junction " + id : "Edge " + id;
    }

    public String formatRegionLabel(int id) {
        return id > 0 ? "v" + id : "e" + id;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int[][]  getRegionMap()       { return regionMap; }
    public boolean  isReady()            { return regionMap != null; }
    public Skeleton getSkeleton()        { return skeleton; }
    public boolean  isLoadedFromFile()   { return isLoadedFromFile; }

    // Direct write access used by RAG.deleteJunction
    public void setRegionMap(int[][] map) { this.regionMap = map; }
}

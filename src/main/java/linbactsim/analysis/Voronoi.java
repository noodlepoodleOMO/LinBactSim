package linbactsim.analysis;

import linbactsim.model.Maze;

import java.util.Map;

// Builds and manages the Voronoi region map from skeleton labels.
// Source: SURE.SkeletonAnalyzer — step 5 of analyze() + region management methods.
public class Voronoi {

    // Source: SURE.SkeletonAnalyzer#regionMap
    // positive = junction region, negative = edge region, 0 = wall/unassigned
    private int[][] regionMap;

    private Skeleton skeleton; // upstream dependency

    public Voronoi(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    // Multi-source BFS Voronoi expansion over open pixels.
    // Source: SURE.SkeletonAnalyzer#voronoiBFS(int[][], int, int)
    // and step 5 of SURE.SkeletonAnalyzer#analyze().
    public void build(Maze maze) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.SkeletonAnalyzer#voronoiBFS(int[][], int, int) — core BFS
    private int[][] voronoiBFS(int[][] seed, int rows, int cols, Maze maze) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.SkeletonAnalyzer#reassignRegion(int, int, int)
    public void reassignRegion(int row, int col, int newId, Maze maze) {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns centroid [avgRow, avgCol] for every non-zero region.
    // Source: SURE.SkeletonAnalyzer#computeRegionCentroids()
    public Map<Integer, double[]> computeRegionCentroids() {
        throw new UnsupportedOperationException("TODO");
    }

    // Restores region map from a saved CSV without re-running thinning.
    // Source: SURE.SkeletonAnalyzer#loadFromVoronoi(int[][])
    public void loadFromSaved(int[][] savedMap, Maze maze) {
        throw new UnsupportedOperationException("TODO");
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    // Source: SURE.SkeletonAnalyzer#getRegionMap()
    public int[][]  getRegionMap() { return regionMap; }

    // Source: SURE.SkeletonAnalyzer#isAnalyzed()
    public boolean  isReady()      { return regionMap != null; }

    public Skeleton getSkeleton()  { return skeleton; }
}

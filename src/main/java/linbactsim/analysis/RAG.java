package linbactsim.analysis;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

import java.awt.Color;
import java.util.*;

// Builds and queries the Region Adjacency Graph (junction ↔ edge connectivity).
// Also owns trajectory analysis and graph traversal methods.
// Source: SURE.SkeletonAnalyzer — buildRAG(), deleteJunction(), trajectory methods,
// shortest path, display helpers.
public class RAG {

    // junctionId → set of connected edgeIds (negative)
    private Map<Integer, Set<Integer>> adjacency;
    // edgeId (negative) → list of junctionIds at its endpoints
    private Map<Integer, List<Integer>> edgeEndpoints;

    private Voronoi voronoi;

    public RAG(Voronoi voronoi) {
        this.voronoi = voronoi;
    }

    // -------------------------------------------------------------------------
    // Build — runs the full analysis pipeline
    // -------------------------------------------------------------------------

    // Source: SURE.SkeletonAnalyzer#analyze() — triggers skeleton + voronoi + RAG
    public void build(Maze maze) {
        voronoi.getSkeleton().analyze(maze);
        voronoi.build(maze);
        buildRAG(maze.getNumRows(), maze.getNumCols());
    }

    // Restores state from a previously saved regionMap (skips skeleton thinning).
    public void loadFromVoronoi(int[][] savedMap, Maze maze) {
        voronoi.loadFromSaved(savedMap, maze);
        buildRAG(maze.getNumRows(), maze.getNumCols());
    }

    // Restores voronoi to a pristine imported state and rebuilds RAG.
    public void resetToImported(int[][] pristineCopy, Maze maze) {
        voronoi.loadFromSaved(pristineCopy, maze);
        buildRAG(maze.getNumRows(), maze.getNumCols());
    }

    // Clears all Voronoi and RAG state so nothing is displayed.
    public void clearVoronoi() {
        voronoi.setRegionMap(null);
        adjacency     = null;
        edgeEndpoints = null;
    }

    // Source: SURE.SkeletonAnalyzer#buildRAG(int, int)
    private void buildRAG(int rows, int cols) {
        int[][] regionMap = voronoi.getRegionMap();
        Set<Integer> presentJunctions = new HashSet<>();
        Set<Integer> presentEdges     = new HashSet<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int id = regionMap[r][c];
                if (id > 0) presentJunctions.add(id);
                else if (id < 0) presentEdges.add(id);
            }

        adjacency     = new HashMap<>();
        edgeEndpoints = new HashMap<>();
        for (int j : presentJunctions) adjacency.put(j, new HashSet<>());
        for (int e : presentEdges)     edgeEndpoints.put(e, new ArrayList<>());

        int[] dr = {0, 1}, dc = {1, 0};
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int a = regionMap[r][c];
                if (a == 0) continue;
                for (int d = 0; d < 2; d++) {
                    int nr = r + dr[d], nc = c + dc[d];
                    if (nr >= rows || nc >= cols) continue;
                    int b = regionMap[nr][nc];
                    if (b == 0 || a == b) continue;
                    if (a > 0 && b < 0) { adjacency.get(a).add(b); addEP(b, a); }
                    else if (a < 0 && b > 0) { adjacency.get(b).add(a); addEP(a, b); }
                }
            }
        }
    }

    private void addEP(int edgeId, int junctionId) {
        List<Integer> ep = edgeEndpoints.computeIfAbsent(edgeId, k -> new ArrayList<>());
        if (!ep.contains(junctionId)) ep.add(junctionId);
    }

    // -------------------------------------------------------------------------
    // Recompute RAG without re-running skeleton/voronoi
    // -------------------------------------------------------------------------

    public void recompute(Maze maze) {
        if (voronoi.getRegionMap() != null)
            buildRAG(maze.getNumRows(), maze.getNumCols());
    }

    // -------------------------------------------------------------------------
    // Delete junction — Source: SURE.SkeletonAnalyzer#deleteJunction(int)
    // -------------------------------------------------------------------------

    public void deleteJunction(int junctionId, Maze maze) {
        int[][] regionMap = voronoi.getRegionMap();
        if (regionMap == null || junctionId <= 0) return;
        int rows = maze.getNumRows(), cols = maze.getNumCols();
        final int EMPTY = Integer.MIN_VALUE;
        int[] dr = {-1, 1, 0, 0}, dc = {0, 0, -1, 1};

        // Step 1 — snapshot connected edges before modification
        Set<Integer> connectedEdges = (adjacency != null)
                ? new HashSet<>(adjacency.getOrDefault(junctionId, Set.of()))
                : new HashSet<>();

        // Step 2 — blank all pixels belonging to this junction
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (regionMap[r][c] == junctionId) regionMap[r][c] = EMPTY;

        // Step 3 — multi-source BFS: fill EMPTY pixels from bordering edge pixels
        Queue<int[]> q = new ArrayDeque<>();
        boolean[][] queued = new boolean[rows][cols];

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (regionMap[r][c] >= 0) continue;
                for (int d = 0; d < 4; d++) {
                    int nr = r + dr[d], nc = c + dc[d];
                    if (maze.isValid(nr, nc) && regionMap[nr][nc] == EMPTY && !queued[r][c]) {
                        q.add(new int[]{r, c});
                        queued[r][c] = true;
                        break;
                    }
                }
            }

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int id = regionMap[cur[0]][cur[1]];
            for (int d = 0; d < 4; d++) {
                int nr = cur[0] + dr[d], nc = cur[1] + dc[d];
                if (!maze.isValid(nr, nc) || queued[nr][nc]) continue;
                if (regionMap[nr][nc] == EMPTY) {
                    regionMap[nr][nc] = id;
                    queued[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }

        // Fallback: absorb remaining EMPTY pixels from any non-zero neighbour
        boolean anyLeft = true;
        while (anyLeft) {
            anyLeft = false;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++) {
                    if (regionMap[r][c] != EMPTY) continue;
                    for (int d = 0; d < 4; d++) {
                        int nr = r + dr[d], nc = c + dc[d];
                        if (maze.isValid(nr, nc) && regionMap[nr][nc] != EMPTY
                                && regionMap[nr][nc] != 0) {
                            regionMap[r][c] = regionMap[nr][nc];
                            anyLeft = true;
                            break;
                        }
                    }
                }
        }
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (regionMap[r][c] == EMPTY) regionMap[r][c] = 0;

        // Step 4 — merge all formerly-connected edges into one region
        if (connectedEdges.size() > 1) {
            int representative = connectedEdges.iterator().next();
            for (int eId : connectedEdges) {
                if (eId == representative) continue;
                for (int r = 0; r < rows; r++)
                    for (int c = 0; c < cols; c++)
                        if (regionMap[r][c] == eId)
                            regionMap[r][c] = representative;
            }
        }

        // Step 5 — rebuild RAG
        buildRAG(rows, cols);
    }

    // -------------------------------------------------------------------------
    // Trajectory analysis — Source: SURE.SkeletonAnalyzer
    // -------------------------------------------------------------------------

    public List<Integer> computeRegionTrajectory(Bacterium b, Maze maze) {
        List<Integer> seq = new ArrayList<>();
        int[][] regionMap = voronoi.getRegionMap();
        if (regionMap == null) return seq;
        int prev = Integer.MIN_VALUE;
        for (int[] pos : b.getTrajectory()) {
            if (!maze.isValid(pos[0], pos[1])) continue;
            int id = regionMap[pos[0]][pos[1]];
            if (id == 0 || id == prev) continue;
            seq.add(id);
            prev = id;
        }
        return inferSkips(seq);
    }

    // If bacterium jumps directly edge→edge, insert the shared junction.
    private List<Integer> inferSkips(List<Integer> seq) {
        if (edgeEndpoints == null) return seq;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < seq.size(); i++) {
            int cur = seq.get(i);
            out.add(cur);
            if (i + 1 < seq.size()) {
                int next = seq.get(i + 1);
                if (cur < 0 && next < 0) {
                    List<Integer> ep1 = edgeEndpoints.getOrDefault(cur, List.of());
                    List<Integer> ep2 = edgeEndpoints.getOrDefault(next, List.of());
                    for (int j : ep1) { if (ep2.contains(j)) { out.add(j); break; } }
                }
            }
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Shortest path — Source: SURE.SkeletonAnalyzer#shortestPath(int, int)
    // -------------------------------------------------------------------------

    public List<Integer> shortestPath(int src, int dst) {
        Map<Integer, Set<Integer>> graph = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> e : adjacency.entrySet()) {
            int jId = e.getKey();
            graph.computeIfAbsent(jId, k -> new HashSet<>()).addAll(e.getValue());
            for (int eId : e.getValue())
                graph.computeIfAbsent(eId, k -> new HashSet<>()).add(jId);
        }
        if (!graph.containsKey(src) || !graph.containsKey(dst)) return List.of();
        Map<Integer, Integer> prev = new HashMap<>();
        Queue<Integer> q = new ArrayDeque<>();
        q.add(src); prev.put(src, null);
        outer:
        while (!q.isEmpty()) {
            int cur = q.poll();
            if (cur == dst) break;
            for (int nb : graph.getOrDefault(cur, Set.of()))
                if (!prev.containsKey(nb)) { prev.put(nb, cur); q.add(nb); }
        }
        if (!prev.containsKey(dst)) return List.of();
        LinkedList<Integer> path = new LinkedList<>();
        for (Integer cur = dst; cur != null; cur = prev.get(cur)) path.addFirst(cur);
        return path;
    }

    // -------------------------------------------------------------------------
    // Display helpers — delegate to Voronoi
    // -------------------------------------------------------------------------

    public Color  getRegionColor(int id)    { return voronoi.getRegionColor(id); }
    public String formatRegionId(int id)    { return voronoi.formatRegionId(id); }
    public String formatRegionLabel(int id) { return voronoi.formatRegionLabel(id); }

    // -------------------------------------------------------------------------
    // Centroids (used by RAGPanel)
    // -------------------------------------------------------------------------

    public Map<Integer, double[]> computeRegionCentroids() {
        return voronoi.computeRegionCentroids();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Map<Integer, Set<Integer>>  getAdjacency()      { return adjacency; }
    public Map<Integer, List<Integer>> getEdgeEndpoints()  { return edgeEndpoints; }
    public int getNumJunctions() { return adjacency != null ? adjacency.size() : 0; }
    public int getNumEdges()     { return edgeEndpoints != null ? edgeEndpoints.size() : 0; }

    public Voronoi  getVoronoi()  { return voronoi; }
    public Skeleton getSkeleton() { return voronoi.getSkeleton(); }

    public int[][] getRegionMap() { return voronoi.getRegionMap(); }
    public boolean isReady()      { return adjacency != null; }
    public boolean hasSkeleton()  { return voronoi.getSkeleton().hasSkeleton(); }
}

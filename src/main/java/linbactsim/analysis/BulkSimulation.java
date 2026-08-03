package linbactsim.analysis;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.resources.BacteriumSpecies;
import linbactsim.simulation.SimulationParameters;
import linbactsim.simulation.SimulationRunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Runs all weight-ratio combinations × noise angles, scores each against an
 * experimental vertex-count histogram, and returns results sorted best-first.
 *
 * Weight ratios are defined as interpretable integer (or simple decimal) ratios.
 * Because setDirectionWeights() normalises internally, only the RATIO matters —
 * (2,1,1) and (4,2,2) produce identical behaviour.
 *
 * Ratio sets and their unique permutations (assigned to wMemory, wNoise, wWall):
 *
 *  Equal
 *    1:1:1      → 1 combo
 *
 *  1 weight dominant, others equal
 *    2:1:1      → 3 perms  (one weight is 2× each other)
 *    3:1:1      → 3 perms  (3×)
 *    4:1:1      → 3 perms  (4×)
 *
 *  2 weights equally dominant, 1 suppressed
 *    2:2:1      → 3 perms  (pair is 2× the third)
 *    3:3:1      → 3 perms  (pair is 3× the third)
 *
 *  All three different — graded dominance
 *    3:2:1      → 6 perms  (each step ×1.5 / ×2)
 *    4:2:1      → 6 perms  (top is 4×, mid is 2×)
 *    4:3:1      → 6 perms  (top is 4×, mid is 3×)
 *    5:2:1      → 6 perms  (top is 5×, mid is 2×)
 *    5:3:1      → 6 perms  (top is 5×, mid is 3×)
 *    3:2:1.5    → 6 perms  (step ×1.33 / ×1.5  — tighter gradation)
 *    4:2.5:1    → 6 perms  (step ×1.6  / ×2.5)
 *
 *  Total weight combos: 1+3+3+3 + 3+3 + 6×7 = 58
 *  × 3 noise angles {0.6, 1.6, π}
 *  × 4 dwell thresholds {0, 10, 20, 30}°
 *  × 5 dwell factors {0.1, 0.3, 0.5, 0.7, 1.0} = 3480 runs
 */
public class BulkSimulation {

    // Each row is one ratio set [wMemory_ratio, wNoise_ratio, wWall_ratio].
    // All unique permutations are generated automatically.
    private static final double[][] RATIO_SETS = {
        // equal
        {1,   1,   1  },
        // 1 dominant
        {2,   1,   1  },
        {3,   1,   1  },
        {4,   1,   1  },
        // 2 equally dominant
        {2,   2,   1  },
        {3,   3,   1  },
        // graded — all three different
        {3,   2,   1  },
        {4,   2,   1  },
        {4,   3,   1  },
        {5,   2,   1  },
        {5,   3,   1  },
        {3,   2,   1.5},
        {4,   2.5, 1  },
    };

    public static final double[] ANGLE_LEVELS = {0.6, 1.6, Math.PI};

    // Corner-dwelling params swept the same way as ANGLE_LEVELS — see Bacterium.setCornerDwellParams().
    // factor=1.0 is a true physics no-op (headOnFactor always returns 1.0 either way), included
    // as a baseline control alongside the swept values.
    public static final double[] DWELL_THRESHOLD_LEVELS = {0, 10, 20, 30};
    public static final double[] DWELL_FACTOR_LEVELS = {0.1, 0.3, 0.5, 0.7, 1.0};

    // -------------------------------------------------------------------------

    private record BacteriumInit(int row, int col, BacteriumSpecies species,
                                 int length, int width) {}
    
    // record: immutable data carrier type declaration
    // private final fields, accessor methods named after the field e.g. .row() .col()
    // equals, hascode, toString

    public record ComboResult(
            double wMemoryRaw, double wNoiseRaw, double wWallRaw, double noiseAngle,
            double dwellThresholdDeg, double dwellFactor,
            double wMemoryNorm, double wNoiseNorm, double wWallNorm,
            Map<Integer, Integer> histogram,
            double score
    ) {}

    // -------------------------------------------------------------------------

    /** Returns the total number of (weight combo × angle) runs that will be executed. */
    public static int totalCombos() {
        int count = 0;
        for (double[] rs : RATIO_SETS) count += uniquePermutations(rs).size();
        return count * ANGLE_LEVELS.length * DWELL_THRESHOLD_LEVELS.length * DWELL_FACTOR_LEVELS.length;
    }

    /**
     * Runs all combos. progressCallback receives the 0-based combo index after each run.
     * Returns results sorted by score descending (best first).
     * 
     * 
     * 
     */
    public static List<ComboResult> run(
            Maze maze,
            RAG rag,
            SimulationRunner runner,
            SimulationParameters baseParams,
            Map<Integer, Integer> expHistogram,
            Consumer<Integer> progressCallback
    ) {

        List<BacteriumInit> initList = snapshotBacteria(maze);
        if (initList.isEmpty()) throw new IllegalStateException("No bacteria in maze.");

        List<ComboResult> results = new ArrayList<>();
        int comboIndex = 0;

        for (double[] ratioSet : RATIO_SETS) {
            for (double[] perm : uniquePermutations(ratioSet)) {
                double wM = perm[0], wN = perm[1], wW = perm[2];

                for (double angle : ANGLE_LEVELS) {
                    for (double dwellThresholdDeg : DWELL_THRESHOLD_LEVELS) {
                        for (double dwellFactor : DWELL_FACTOR_LEVELS) {
                            maze.clearBacteria();
                            maze.clearDensity();

                            for (BacteriumInit init : initList) {
                                Bacterium b = new Bacterium(
                                        init.length(), init.width(),
                                        init.row(), init.col(),
                                        angle, init.species());
                                b.setDirectionWeights(wM, wN, wW);
                                b.setCornerDwellParams(dwellThresholdDeg, dwellFactor);
                                maze.addBacterium(b);
                            }

                            runner.runFast(maze, baseParams);

                            VertexCount vc = new VertexCount();
                            vc.compute(maze, rag, true); // exited bacteria only
                            Map<Integer, Integer> histogram = new HashMap<>(vc.getHistogram());

                            double sum = wM + wN + wW;
                            double score = HistogramSimilarity.similarityScore(histogram, expHistogram);

                            results.add(new ComboResult(
                                    wM, wN, wW, angle, dwellThresholdDeg, dwellFactor,
                                    wM / sum, wN / sum, wW / sum,
                                    histogram, score));

                            progressCallback.accept(comboIndex++);
                        }
                    }
                }
            }
        }

        results.sort(Comparator.comparingDouble(ComboResult::score).reversed());
        return results;
    }

    /** Exports all results as a ranked CSV. */
    public static void exportCsv(List<ComboResult> results, File file) throws IOException {
        File out = file.getName().toLowerCase().endsWith(".csv")
                ? file : new File(file.getAbsolutePath() + ".csv");
        try (PrintWriter pw = new PrintWriter(out)) {
            pw.println("rank,wMemory_raw,wNoise_raw,wWall_raw,noiseAngle,dwellThresholdDeg,dwellFactor," +
                       "wMemory_norm,wNoise_norm,wWall_norm,emd_distance,similarity_score");
            for (int i = 0; i < results.size(); i++) {
                ComboResult r = results.get(i);
                double emd = 1.0 / r.score() - 1.0;
                pw.printf("%d,%.2f,%.2f,%.2f,%.4f,%.2f,%.4f,%.4f,%.4f,%.4f,%.6f,%.6f%n",
                        i + 1,
                        r.wMemoryRaw(), r.wNoiseRaw(), r.wWallRaw(), r.noiseAngle(),
                        r.dwellThresholdDeg(), r.dwellFactor(),
                        r.wMemoryNorm(), r.wNoiseNorm(), r.wWallNorm(),
                        emd, r.score());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static List<BacteriumInit> snapshotBacteria(Maze maze) {
        List<BacteriumInit> list = new ArrayList<>();
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            int[] pos = b.getTrajectory().get(0);
            list.add(new BacteriumInit(
                    pos[0], pos[1], b.getSpecies(), b.getLength(), b.getWidth()));
        }
        return list;
    }

    /** Returns all unique permutations of a length-3 array. */
    private static List<double[]> uniquePermutations(double[] arr) {
        int[][] orders = {{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
        Set<String> seen = new LinkedHashSet<>();
        List<double[]> result = new ArrayList<>();
        for (int[] o : orders) {
            double[] p = {arr[o[0]], arr[o[1]], arr[o[2]]};
            // Use rounded string key so 1.0 vs 1.000000001 don't create duplicates
            String key = String.format("%.6f,%.6f,%.6f", p[0], p[1], p[2]);
            if (seen.add(key)) result.add(p);
        }
        return result;
    }
}

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

// Runs 75 parameter combinations (25 distinct weight combos × 3 noise angles),
// computes a vertex-count histogram per combo, and scores each against an experimental histogram.
public class BulkSimulation {

    public static final double[] WEIGHT_LEVELS = {0.1, 0.33, 0.7};
    public static final double[] ANGLE_LEVELS  = {0.6, 1.6, Math.PI};

    // Immutable snapshot of a bacterium's initial state for re-creation across combos.
    private record BacteriumInit(int row, int col, BacteriumSpecies species,
                                 int length, int width) {}

    // Result for one parameter combination.
    public record ComboResult(
            double wMemoryRaw, double wNoiseRaw, double wWallRaw, double noiseAngle,
            double wMemoryNorm, double wNoiseNorm, double wWallNorm,
            Map<Integer, Integer> histogram,
            double score
    ) {}

    /**
     * Runs all 75 combos against the current maze/bacteria setup.
     * progressCallback is called with the combo index (0-based) after each combo completes.
     * Returns results sorted by score descending (best first).
     */
    public static List<ComboResult> run(
            Maze maze,
            RAG rag,
            SimulationRunner runner,
            SimulationParameters baseParams,
            Map<Integer, Integer> expHistogram,
            Consumer<Integer> progressCallback
    ) throws IOException {

        // Snapshot initial bacteria state before any simulation alters them.
        List<BacteriumInit> initList = snapshotBacteria(maze);
        if (initList.isEmpty()) throw new IllegalStateException("No bacteria in maze.");

        List<ComboResult> results = new ArrayList<>();
        int comboIndex = 0;

        for (double wM : WEIGHT_LEVELS) {
            for (double wN : WEIGHT_LEVELS) {
                for (double wW : WEIGHT_LEVELS) {
                    // Skip L,L,L and H,H,H — they normalize identically to M,M,M
                    if (allEqual(wM, wN, wW) && Math.abs(wM - 0.33) > 1e-9) continue;

                    for (double angle : ANGLE_LEVELS) {
                        // Reset maze state
                        maze.clearBacteria();
                        maze.clearDensity();

                        // Recreate bacteria from snapshot with this combo's weights and angle
                        for (BacteriumInit init : initList) {
                            Bacterium b = new Bacterium(
                                    init.length(), init.width(),
                                    init.row(), init.col(),
                                    angle, init.species()
                            );
                            b.setDirectionWeights(wM, wN, wW);
                            maze.addBacterium(b);
                        }

                        runner.runFast(maze, baseParams);

                        VertexCount vc = new VertexCount();
                        vc.compute(maze, rag, true); // exited bacteria only
                        Map<Integer, Integer> histogram = new HashMap<>(vc.getHistogram());

                        double sum = wM + wN + wW;
                        double score = HistogramSimilarity.similarityScore(histogram, expHistogram);

                        results.add(new ComboResult(
                                wM, wN, wW, angle,
                                wM / sum, wN / sum, wW / sum,
                                histogram, score
                        ));

                        progressCallback.accept(comboIndex++);
                    }
                }
            }
        }

        results.sort(Comparator.comparingDouble(ComboResult::score).reversed());
        return results;
    }

    // Exports all results as a ranked CSV.
    public static void exportCsv(List<ComboResult> results, File file) throws IOException {
        File out = file.getName().toLowerCase().endsWith(".csv")
                ? file : new File(file.getAbsolutePath() + ".csv");
        try (PrintWriter pw = new PrintWriter(out)) {
            pw.println("rank,wMemory_raw,wNoise_raw,wWall_raw,noiseAngle," +
                       "wMemory_norm,wNoise_norm,wWall_norm,emd_distance,similarity_score");
            for (int i = 0; i < results.size(); i++) {
                ComboResult r = results.get(i);
                double emd = 1.0 / r.score() - 1.0; // inverse of similarityScore formula
                pw.printf("%d,%.2f,%.2f,%.2f,%.4f,%.4f,%.4f,%.4f,%.6f,%.6f%n",
                        i + 1,
                        r.wMemoryRaw(), r.wNoiseRaw(), r.wWallRaw(), r.noiseAngle(),
                        r.wMemoryNorm(), r.wNoiseNorm(), r.wWallNorm(),
                        emd, r.score());
            }
        }
    }

    private static List<BacteriumInit> snapshotBacteria(Maze maze) {
        List<BacteriumInit> list = new ArrayList<>();
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            int[] initPos = b.getTrajectory().get(0); // position set in constructor
            list.add(new BacteriumInit(
                    initPos[0], initPos[1],
                    b.getSpecies(), b.getLength(), b.getWidth()
            ));
        }
        return list;
    }

    private static boolean allEqual(double a, double b, double c) {
        return Math.abs(a - b) < 1e-9 && Math.abs(b - c) < 1e-9;
    }
}

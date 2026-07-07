package linbactsim.analysis;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

// Computes a histogram of unique junction vertices visited per bacterium.
// The initialization vertex is only counted if the bacterium revisits it after leaving.
// Call compute(), then exportCsv(). compute() clears state, so calling it twice
// (once with exitedOnly=false, once with true) yields two independent results.
public class VertexCount {

    // key = unique vertex count, value = number of bacteria with that count
    private final TreeMap<Integer, Integer> histogram = new TreeMap<>();
    private final List<Integer> perBacteriumCounts = new ArrayList<>();

    public void compute(Maze maze, RAG rag, boolean exitedOnly) {
        histogram.clear();
        perBacteriumCounts.clear();
        if (rag == null || !rag.isReady()) return;

        int[][] regionMap = rag.getRegionMap();
        if (regionMap == null) return;

        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            if (exitedOnly && !b.hasExited()) continue;

            // Initialization position is always trajectory element 0 (set in Bacterium constructor)
            int[] initPos = b.getTrajectory().get(0);
            int initVertexId = regionMap[initPos[0]][initPos[1]];

            // Count visits per junction vertex (id > 0)
            Map<Integer, Integer> counts = new HashMap<>();
            for (int id : rag.computeRegionTrajectory(b, maze)) {
                if (id > 0) counts.merge(id, 1, Integer::sum);
            }

            // Subtract the initialization visit — only counts if bacterium revisits
            if (initVertexId > 0 && counts.containsKey(initVertexId)) {
                int adjusted = counts.get(initVertexId) - 1;
                if (adjusted <= 0) counts.remove(initVertexId);
                else counts.put(initVertexId, adjusted);
            }

            int uniqueCount = counts.size();
            perBacteriumCounts.add(uniqueCount);
            histogram.merge(uniqueCount, 1, Integer::sum);
        }
    }

    public Map<Integer, Integer> getHistogram() {
        return Collections.unmodifiableMap(histogram);
    }

    public List<Integer> getPerBacteriumCounts() {
        return Collections.unmodifiableList(perBacteriumCounts);
    }

    // Exports histogram as CSV with two columns: unique_vertices_visited, bacteria_count.
    // Appends .csv if file does not already have that extension.
    public void exportCsv(File file) throws IOException {
        File out = file.getName().toLowerCase().endsWith(".csv")
                ? file : new File(file.getAbsolutePath() + ".csv");
        try (PrintWriter pw = new PrintWriter(out)) {
            pw.println("unique_vertices_visited,bacteria_count");
            for (Map.Entry<Integer, Integer> e : histogram.entrySet()) {
                pw.println(e.getKey() + "," + e.getValue());
            }
        }
    }
}

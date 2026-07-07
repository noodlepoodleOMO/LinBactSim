package linbactsim.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

// Compares two vertex-count histograms using Wasserstein-1 distance (Earth Mover's Distance).
// Histograms have integer keys (unique vertices visited) and integer values (bacteria count).
public class HistogramSimilarity {

    // Reads a CSV produced by VertexCount.exportCsv(): header line, then "key,value" rows.
    public static Map<Integer, Integer> loadFromCsv(File file) throws IOException {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 2);
                map.put(Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()));
            }
        }
        return map;
    }

    // Wasserstein-1 distance between two histograms (lower = more similar, 0 = identical).
    // For 1D ordered distributions: W1 = integral |CDF_a - CDF_b| = sum |CDF_a[k] - CDF_b[k]|
    // over all integer keys in the union of both histograms' key ranges.
    public static double wasserstein1(Map<Integer, Integer> a, Map<Integer, Integer> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;

        double totalA = a.values().stream().mapToInt(Integer::intValue).sum();
        double totalB = b.values().stream().mapToInt(Integer::intValue).sum();
        if (totalA == 0 && totalB == 0) return 0.0;
        // If one is empty treat it as a degenerate distribution at 0
        if (totalA == 0 || totalB == 0) {
            // Degenerate case: distance is the mean of the non-empty distribution
            Map<Integer, Integer> nonEmpty = totalA == 0 ? b : a;
            double total = totalA == 0 ? totalB : totalA;
            double mean = nonEmpty.entrySet().stream()
                    .mapToDouble(e -> e.getKey() * e.getValue() / total)
                    .sum();
            return mean;
        }

        // Find key range covering both histograms
        int minKey = Math.min(
                a.isEmpty() ? Integer.MAX_VALUE : ((TreeMap<Integer,Integer>)new TreeMap<>(a)).firstKey(),
                b.isEmpty() ? Integer.MAX_VALUE : ((TreeMap<Integer,Integer>)new TreeMap<>(b)).firstKey()
        );
        int maxKey = Math.max(
                a.isEmpty() ? Integer.MIN_VALUE : ((TreeMap<Integer,Integer>)new TreeMap<>(a)).lastKey(),
                b.isEmpty() ? Integer.MIN_VALUE : ((TreeMap<Integer,Integer>)new TreeMap<>(b)).lastKey()
        );

        // Walk keys in order, accumulate CDFs, sum |CDF_a - CDF_b|
        double cdfA = 0.0, cdfB = 0.0, emd = 0.0;
        for (int k = minKey; k <= maxKey; k++) {
            cdfA += a.getOrDefault(k, 0) / totalA;
            cdfB += b.getOrDefault(k, 0) / totalB;
            emd += Math.abs(cdfA - cdfB);
        }
        return emd;
    }

    // Similarity score in (0, 1]: higher = more similar. Returns 1.0 for identical distributions.
    public static double similarityScore(Map<Integer, Integer> sim, Map<Integer, Integer> exp) {
        return 1.0 / (1.0 + wasserstein1(sim, exp));
    }
}

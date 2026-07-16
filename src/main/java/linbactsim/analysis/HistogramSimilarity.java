package linbactsim.analysis;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

// Compares two vertex-count histograms using Wasserstein-1 distance (Earth Mover's Distance).
// Histograms have integer keys (unique vertices visited) and integer values (bacteria count).
public class HistogramSimilarity {

    // Loads a histogram from a CSV or XLSX file.
    // Expected layout: header row, then rows of (unique_vertices_visited, bacteria_count).
    // Accepts .csv (plain text) or .xlsx/.xls (Apache POI).
    public static Map<Integer, Integer> loadFromFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return loadFromXlsx(file);
        }
        return loadFromCsv(file);
    }

    private static Map<Integer, Integer> loadFromCsv(File file) throws IOException {
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

    private static Map<Integer, Integer> loadFromXlsx(File file) throws IOException {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; } // skip header
                if (row.getCell(0) == null || row.getCell(1) == null) continue;
                int key   = (int) row.getCell(0).getNumericCellValue();
                int value = (int) row.getCell(1).getNumericCellValue();
                map.put(key, value);
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
        if (totalA == 0 || totalB == 0) {
            Map<Integer, Integer> nonEmpty = totalA == 0 ? b : a;
            double total = totalA == 0 ? totalB : totalA;
            return nonEmpty.entrySet().stream()
                    .mapToDouble(e -> e.getKey() * e.getValue() / total)
                    .sum();
        }

        TreeMap<Integer, Integer> ta = new TreeMap<>(a);
        TreeMap<Integer, Integer> tb = new TreeMap<>(b);
        int minKey = Math.min(ta.firstKey(), tb.firstKey());
        int maxKey = Math.max(ta.lastKey(),  tb.lastKey());

        double cdfA = 0.0, cdfB = 0.0, emd = 0.0;
        for (int k = minKey; k <= maxKey; k++) {
            cdfA += a.getOrDefault(k, 0) / totalA;
            cdfB += b.getOrDefault(k, 0) / totalB;
            emd  += Math.abs(cdfA - cdfB);
        }
        return emd;
    }

    // Similarity score in (0, 1]: higher = more similar. Returns 1.0 for identical distributions.
    public static double similarityScore(Map<Integer, Integer> sim, Map<Integer, Integer> exp) {
        return 1.0 / (1.0 + wasserstein1(sim, exp));
    }
}

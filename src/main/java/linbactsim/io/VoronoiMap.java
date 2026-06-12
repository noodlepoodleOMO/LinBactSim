package linbactsim.io;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Saves and loads the Voronoi regionMap to/from CSV.
// Source: SURE.Main saveVoronoiButton (~lines 336–361)
//         and loadVoronoiButton (~lines 363–395).
public class VoronoiMap {

    // Source: SURE.Main saveVoronoiButton ActionListener (~lines 336–361)
    public static void save(int[][] regionMap, File file) throws IOException {
        File out = file.getName().toLowerCase().endsWith(".csv")
                ? file : new File(file.getAbsolutePath() + ".csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            for (int[] row : regionMap) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < row.length; c++) {
                    if (c > 0) sb.append(',');
                    sb.append(row[c]);
                }
                pw.println(sb);
            }
        }
    }

    // Source: SURE.Main loadVoronoiButton ActionListener (~lines 363–395)
    public static int[][] load(File file) throws IOException {
        List<int[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++)
                    row[i] = Integer.parseInt(tokens[i].trim());
                rows.add(row);
            }
        }
        return rows.toArray(new int[0][]);
    }

    // Deep-copy a regionMap (used to store the pristine imported state).
    public static int[][] deepCopy(int[][] src) {
        int[][] copy = new int[src.length][];
        for (int r = 0; r < src.length; r++)
            copy[r] = src[r].clone();
        return copy;
    }
}

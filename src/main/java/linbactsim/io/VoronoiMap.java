package linbactsim.io;

import java.io.File;
import java.io.IOException;

// Saves and loads the Voronoi regionMap to/from CSV.
// Source: SURE.Main saveVoronoiButton (~lines 336–361)
//         and loadVoronoiButton (~lines 363–395).
public class VoronoiMap {

    // Source: SURE.Main saveVoronoiButton ActionListener (~lines 336–361)
    // Writes regionMap as a CSV where each cell contains its region ID.
    public static void save(int[][] regionMap, File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Main loadVoronoiButton ActionListener (~lines 363–395)
    // Reads a region-map CSV and returns it as a 2D int array.
    public static int[][] load(File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }
}

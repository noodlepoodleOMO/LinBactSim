package linbactsim.resources;

// Source: SURE.Main plaza-maze construction (~lines 59–72) and resource path strings.
public class MazeMaps {

    public static final String UNIFORM_PATH     = "mazes/uniformMaze_java_array.txt";
    public static final String NON_UNIFORM_PATH = "mazes/nonUniformMaze_java_array_UPDATED.txt";

    // Builds the outer-boundary wall list for a 100×100 plaza grid.
    // Source: SURE.Main#main() (~lines 59–72). Returns [row, col] pairs.
    public static int[][] buildPlazaWalls() {
        int[][] walls = new int[100 * 4 - 4][2];
        int idx = 0;
        for (int col = 0; col < 100; col++) {
            walls[idx++] = new int[]{0, col};
            walls[idx++] = new int[]{99, col};
        }
        for (int row = 1; row < 99; row++) {
            walls[idx++] = new int[]{row, 0};
            walls[idx++] = new int[]{row, 99};
        }
        return walls;
    }
}

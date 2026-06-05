package linbactsim.resources;

// Stores maze data and resource paths.
// Source: SURE.Main plaza-maze construction (~lines 59–72) and
//         uniform/non-uniform maze resource path strings scattered in listeners.
public class MazeMaps {

    // Resource paths for bundled mazes — Source: SURE.Main maze-loading listeners
    public static final String UNIFORM_PATH     = "mazes/uniformMaze_java_array.txt";
    public static final String NON_UNIFORM_PATH = "mazes/nonUniformMaze_java_array_UPDATED.txt";

    // Builds the outer-boundary-only plaza maze wall list for a 100x100 grid.
    // Source: SURE.Main#main() plaza construction (~lines 59–72).
    // Returns array of [row, col] wall positions.
    public static int[][] buildPlazaWalls() {
        throw new UnsupportedOperationException("TODO");
    }
}

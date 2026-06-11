package linbactsim.model;

import java.util.ArrayList;
import java.util.List;

// Source: SURE.Maze
// Note: SURE.MazeManager (trivial getMaze/setMaze wrapper) is absorbed here.
// In lambdas that previously needed MazeManager for effective-final reassignment,
// use Maze[] mazeRef = {maze}; instead.
public class Maze {

    private Pixel[][] grid;
    private int displayPixelSize;
    private List<Bacterium> bacteria;
    private int boundaryThickness = 2;

    // Source: SURE.Maze(int, int, int)
    public Maze(int rows, int cols, int displayPixelSize) {
        this.displayPixelSize = displayPixelSize;
        this.grid = new Pixel[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = new Pixel(r, c);
        this.bacteria = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Grid accessors
    // -------------------------------------------------------------------------

    // Returns null for out-of-bounds coordinates.
    public Pixel getPixel(int row, int col) {
        return isValid(row, col) ? grid[row][col] : null;
    }

    public int getDisplayPixelSize() { return displayPixelSize; }
    public int getNumRows()          { return grid.length; }
    public int getNumCols()          { return grid[0].length; }

    public boolean isValid(int row, int col) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }

    // Out-of-bounds coordinates are treated as walls so movement models
    // never need a separate bounds check alongside the wall check.
    public boolean isWall(int row, int col) {
        return !isValid(row, col) || grid[row][col].isWall();
    }

    public void setWall(int row, int col) {
        grid[row][col].setWall(true);
    }

    public int getBoundaryThickness()                    { return boundaryThickness; }
    public void setBoundaryThickness(int thickness)      { this.boundaryThickness = thickness; }

    // -------------------------------------------------------------------------
    // Bacterium list
    // -------------------------------------------------------------------------

    public List<Bacterium> getBacteria()              { return bacteria; }
    public void addBacterium(Bacterium b)             { bacteria.add(b); }
    public Bacterium getBacterium(int index)          { return bacteria.get(index); }
    public int[] getBacteriumPosition(int index)      { return bacteria.get(index).getPosition(); }
    public int getBacteriaCount()                     { return bacteria.size(); }
    public void clearBacteria()                       { bacteria.clear(); }

    // -------------------------------------------------------------------------
    // Grid-wide clear helpers
    // -------------------------------------------------------------------------

    public void clearDensity() {
        for (Pixel[] row : grid)
            for (Pixel p : row)
                p.resetCount();
    }

    public void clearWalls() {
        for (Pixel[] row : grid)
            for (Pixel p : row)
                p.setWall(false);
    }

    public void clearExits() {
        for (Pixel[] row : grid)
            for (Pixel p : row)
                p.setExit(false);
    }

    // -------------------------------------------------------------------------
    // Bresenham line — Source: SURE.Maze#getPixelsOnLine
    // Rasterises the straight line from (row1,col1) to (row2,col2) into an
    // ordered list of grid pixels. Pure integer arithmetic; no rounding.
    // Out-of-bounds pixels are skipped so callers never receive a null Pixel.
    // -------------------------------------------------------------------------
    public List<Pixel> getPixelsOnLine(int row1, int col1, int row2, int col2) {
        List<Pixel> line = new ArrayList<>();

        int dx  = Math.abs(col2 - col1);
        int dy  = Math.abs(row2 - row1);
        int sx  = col1 < col2 ? 1 : -1;
        int sy  = row1 < row2 ? 1 : -1;
        int err = dx - dy;

        int x = col1, y = row1;
        while (true) {
            if (isValid(y, x)) line.add(grid[y][x]);

            if (x == col2 && y == row2) break;

            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
        return line;
    }
}

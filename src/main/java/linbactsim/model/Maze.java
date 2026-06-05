package linbactsim.model;

import java.util.List;

// Source: SURE.Maze
// Note: SURE.MazeManager (trivial getMaze/setMaze wrapper) is absorbed here.
// In lambdas that previously needed MazeManager for effective-final reassignment,
// use Maze[] mazeRef = {maze}; instead.
public class Maze {

    // Source: SURE.Maze fields
    private Pixel[][] grid;
    private int pixelSize;
    private List<Bacterium> bacteria;
    private int boundaryThickness = 2;

    // Source: SURE.Maze(int, int, int)
    public Maze(int rows, int cols, int pixelSize) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getPixel(int, int)
    public Pixel getPixel(int row, int col) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getPixelSize()
    public int getPixelSize() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#setWall(int, int)
    public void setWall(int row, int col) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#isWall(int, int)
    public boolean isWall(int row, int col) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#isValid(int, int)
    public boolean isValid(int row, int col) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getNumRows()
    public int getNumRows() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getNumCols()
    public int getNumCols() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getBacteria()
    public List<Bacterium> getBacteria() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#addBacterium(Bacterium)
    public void addBacterium(Bacterium bacterium) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getBacteriumPosition(int)
    public int[] getBacteriumPosition(int index) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getBacterium(int)
    public Bacterium getBacterium(int index) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getBacteriaCount()
    public int getBacteriaCount() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getPixelsOnLine(int, int, int, int) — Bresenham line algorithm
    public List<Pixel> getPixelsOnLine(int row1, int col1, int row2, int col2) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#clearBacteria()
    public void clearBacteria() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#clearDensity()
    public void clearDensity() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#clearWalls()
    public void clearWalls() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#clearExits()
    public void clearExits() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#getBoundaryThickness()
    public int getBoundaryThickness() {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Maze#setBoundaryThickness(int)
    public void setBoundaryThickness(int boundaryThickness) {
        throw new UnsupportedOperationException("TODO");
    }
}

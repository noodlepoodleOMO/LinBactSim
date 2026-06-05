package linbactsim.simulation;

// Converts between pixel grid coordinates and continuous world coordinates.
// New class — no equivalent in SURE. Provides the foundation for a future
// continuous-space simulation where positions are not snapped to integer pixels.
public class CoordinateSystem {

    private final double pixelSize; // physical size of one pixel (µm)

    public CoordinateSystem(double pixelSize) {
        this.pixelSize = pixelSize;
    }

    // Grid pixel → continuous world position [worldRow, worldCol]
    public double[] pixelToWorld(int row, int col) {
        throw new UnsupportedOperationException("TODO");
    }

    // Continuous world position → nearest grid pixel [row, col]
    public int[] worldToPixel(double worldRow, double worldCol) {
        throw new UnsupportedOperationException("TODO");
    }

    public double getPixelSize() { return pixelSize; }
}

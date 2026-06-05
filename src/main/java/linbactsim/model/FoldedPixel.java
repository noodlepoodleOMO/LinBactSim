package linbactsim.model;

// Source: SURE.FoldedPixel (direct carry)
// Holds the combined visit count and wall status for one cell in the
// symmetry-folded density map (four quadrants overlaid onto one).
public class FoldedPixel {

    // Source: SURE.FoldedPixel fields (public in original — kept public for compatibility)
    public int count;
    public boolean isWall;

    // Source: SURE.FoldedPixel(int, boolean)
    public FoldedPixel(int count, boolean isWall) {
        this.count = count;
        this.isWall = isWall;
    }
}

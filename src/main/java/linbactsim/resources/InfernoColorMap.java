package linbactsim.resources;

import java.awt.Color;

// Source: SURE.InfernoColorMap (direct carry, package move only)
// Provides the 256-entry Inferno perceptual colormap for density visualization.
public class InfernoColorMap {

    // Source: SURE.InfernoColorMap#INFERNO — 256 RGB entries
    // Copy the full array from SURE.InfernoColorMap.java
    public static final int[][] INFERNO = new int[256][3]; // TODO: populate from SURE.InfernoColorMap#INFERNO

    // Source: SURE.InfernoColorMap#CACHE — pre-computed Color objects
    public static final Color[] CACHE = new Color[256]; // TODO: populate from SURE.InfernoColorMap#CACHE

    // Source: SURE.InfernoColorMap#get(float)
    // Returns the Color for a normalized value t in [0.0, 1.0].
    public static Color get(float t) {
        throw new UnsupportedOperationException("TODO");
    }
}

package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.model.Pixel;

import java.util.List;

// Inverse-Boltzmann force-field movement model.
// Same blend/memory/noise structure as WeibullModel; differs only in how the
// wall interaction vector is computed — uses the per-species force function
// F(x) derived from a sum-of-Gaussians free-energy fit rather than a Weibull PDF.
//
// Sign convention (F > 0 means force is directed away from that wall):
//   up wall    → F_up   > 0 → downward  (+row); F_up   < 0 → upward   (-row)
//   down wall  → F_down > 0 → upward    (-row); F_down < 0 → downward (+row)
//   left wall  → F_left > 0 → rightward (+col); F_left < 0 → leftward (-col)
//   right wall → F_right> 0 → leftward  (-col); F_right< 0 → rightward(+col)
//
// Net wall vector:
//   wallRow = F(d_up)   - F(d_down)    (up pushes +row, down pushes -row)
//   wallCol = F(d_left) - F(d_right)   (left pushes +col, right pushes -col)
//
// The raw force magnitudes inform the net direction (not normalised per-side);
// wWall in setDirectionWeights() controls the overall influence on the blend.
public class ForceModel implements MovementModel {

    // Source: WeibullModel#step — identical; wall vector is computed in computeDirection
    @Override
    public void step(Bacterium bacterium, Maze maze, int dt) {
        if (bacterium.hasExited()) return;

        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        double dir           = computeDirection(bacterium, maze, bacterium.getNoise());
        double verticalDir   = Math.sin(dir);
        double horizontalDir = Math.cos(dir);

        double stepDist = displacement(bacterium, dt);

        int newRow = (int)(row + verticalDir   * stepDist);
        int newCol = (int)(col + horizontalDir * stepDist);

        if (maze.isValid(newRow, newCol) && !maze.isWall(newRow, newCol)) {
            List<Pixel> path = maze.getPixelsOnLine(row, col, newRow, newCol);

            boolean collision = false;
            for (Pixel p : path) {
                if (p.isWall()) { collision = true; break; }
            }

            if (!collision) {
                for (int i = 1; i < path.size(); i++) {
                    Pixel p = path.get(i);
                    bacterium.setContinuousPosition(p.getRow(), p.getCol());
                    bacterium.recordPosition(p.getRow(), p.getCol());
                    maze.getPixel(p.getRow(), p.getCol()).addCount();
                    if (p.isExit()) {
                        bacterium.setExited(true);
                        bacterium.addTime(dt);
                        return;
                    }
                }
                bacterium.addTime(dt);
            } else {
                applyRandomCorrection(maze, row, col, newRow, newCol, dt, dir, bacterium);
            }
        } else {
            applyRandomCorrection(maze, row, col, newRow, newCol, dt, dir, bacterium);
        }
    }

    @Override
    public double computeDirection(Bacterium bacterium, Maze maze, double noiseBound) {
        bacterium.ensureHeadingInitialized();

        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        // Distances: [up, down, right, left]
        double[] d = getDistanceToWall(maze, row, col, bacterium.getForcePixelSize());

        // Net force components — magnitudes sum; wWall weight controls overall influence.
        double wallRow = bacterium.forceAtDistance(d[0]) - bacterium.forceAtDistance(d[1]);
        double wallCol = bacterium.forceAtDistance(d[3]) - bacterium.forceAtDistance(d[2]);

        double[] wallU = unit(wallRow, wallCol);
        wallRow = wallU[0];
        wallCol = wallU[1];

        double headingRow = bacterium.getHeadingRow();
        double headingCol = bacterium.getHeadingCol();

        double currentAngle;
        if (norm(headingRow, headingCol) >= 1e-9) {
            currentAngle = Math.atan2(headingRow, headingCol);
        } else if (norm(wallRow, wallCol) >= 1e-9) {
            currentAngle = Math.atan2(wallRow, wallCol);
        } else {
            currentAngle = Math.PI / 2.0;
        }

        double dTheta     = RandomNumberGenerator.getAngleNoise(noiseBound);
        double noisyAngle = currentAngle + dTheta;
        double[] noiseU   = unit(Math.sin(noisyAngle), Math.cos(noisyAngle));
        double noiseRow   = noiseU[0];
        double noiseCol   = noiseU[1];

        double combRow = bacterium.getWMemory() * headingRow + bacterium.getWNoise() * noiseRow + bacterium.getWWall() * wallRow;
        double combCol = bacterium.getWMemory() * headingCol + bacterium.getWNoise() * noiseCol + bacterium.getWWall() * wallCol;

        if (norm(combRow, combCol) < 1e-9) {
            combRow = wallRow;
            combCol = wallCol;
        }

        double[] combU = unit(combRow, combCol);
        combRow = combU[0];
        combCol = combU[1];

        int boundary = maze.getBoundaryThickness();
        int minRow = boundary;
        int minCol = boundary;
        int maxRow = maze.getNumRows() - 1 - boundary;
        int maxCol = maze.getNumCols() - 1 - boundary;

        boolean changed = false;
        if (row <= minRow && combRow < 0) { combRow = 0; changed = true; }
        if (row >= maxRow && combRow > 0) { combRow = 0; changed = true; }
        if (col <= minCol && combCol < 0) { combCol = 0; changed = true; }
        if (col >= maxCol && combCol > 0) { combCol = 0; changed = true; }

        if (changed) {
            double[] boundedU = unit(combRow, combCol);
            combRow = boundedU[0];
            combCol = boundedU[1];
            if (norm(combRow, combCol) < 1e-9) {
                combRow = wallRow;
                combCol = wallCol;
            }
        }

        bacterium.setHeading(combRow, combCol);
        return Math.atan2(combRow, combCol);
    }

    // Source: WeibullModel#applyRandomCorrection — identical
    public void applyRandomCorrection(Maze maze, int row, int col,
                                      int newRow, int newCol,
                                      int dt, double originalDir,
                                      Bacterium bacterium) {
        int boundary = maze.getBoundaryThickness();
        int clampedNewRow = Math.max(boundary, Math.min(newRow, maze.getNumRows() - 1 - boundary));
        int clampedNewCol = Math.max(boundary, Math.min(newCol, maze.getNumCols() - 1 - boundary));

        double totalDist = Math.sqrt((clampedNewRow - row) * (clampedNewRow - row)
                                   + (clampedNewCol - col) * (clampedNewCol - col));

        List<Pixel> path = maze.getPixelsOnLine(row, col, clampedNewRow, clampedNewCol);

        int lastFreeRow = row;
        int lastFreeCol = col;
        Pixel collisionPixel = null;
        for (Pixel p : path) {
            if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) {
                collisionPixel = p;
                break;
            }
            lastFreeRow = p.getRow();
            lastFreeCol = p.getCol();
            bacterium.setContinuousPosition(lastFreeRow, lastFreeCol);
            bacterium.recordPosition(lastFreeRow, lastFreeCol);
            maze.getPixel(lastFreeRow, lastFreeCol).addCount();
        }

        if (collisionPixel == null) { bacterium.addTime(dt); return; }

        double usedDist = Math.sqrt((lastFreeRow - row) * (lastFreeRow - row)
                                  + (lastFreeCol - col) * (lastFreeCol - col));
        double remaining = totalDist - usedDist;
        if (remaining < 0.5) { bacterium.addTime(dt); return; }

        int wRow = collisionPixel.getRow();
        int wCol = collisionPixel.getCol();

        boolean horizontalWall = maze.isWall(wRow, wCol - 1) || maze.isWall(wRow, wCol + 1);
        boolean verticalWall   = maze.isWall(wRow - 1, wCol) || maze.isWall(wRow + 1, wCol);

        double verticalDir   = Math.sin(originalDir);
        double horizontalDir = Math.cos(originalDir);
        int slideDirRow = 0;
        int slideDirCol = 0;
        if (horizontalWall) {
            slideDirCol = (horizontalDir > 0 ? 1 : -1);
        } else if (verticalWall) {
            slideDirRow = (verticalDir > 0 ? 1 : -1);
        } else {
            bacterium.addTime(dt);
            return;
        }

        int sRow = lastFreeRow + (int) Math.round(slideDirRow * remaining);
        int sCol = lastFreeCol + (int) Math.round(slideDirCol * remaining);

        List<Pixel> slidePath = maze.getPixelsOnLine(lastFreeRow, lastFreeCol, sRow, sCol);
        for (Pixel p : slidePath) {
            if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) break;
            bacterium.setContinuousPosition(p.getRow(), p.getCol());
            bacterium.recordPosition(p.getRow(), p.getCol());
            maze.getPixel(p.getRow(), p.getCol()).addCount();
            if (p.isExit()) {
                bacterium.setExited(true);
                bacterium.addTime(dt);
                return;
            }
        }
        bacterium.addTime(dt);
    }

    // Source: WeibullModel#displacement
    public double displacement(Bacterium bacterium, int dt) {
        double noise = RandomNumberGenerator.getDisplacementNoise(bacterium.getStdDev());
        return (bacterium.getVelocity() + noise) * dt;
    }

    // Source: WeibullModel#getDistanceToWall — returns [up, down, right, left] in physical units
    public double[] getDistanceToWall(Maze maze, int row, int col, double pixelsize) {
        double[] result = new double[4];
        int r, c;

        r = row - 1;
        while (maze.isValid(r, col) && !maze.isWall(r, col)) {
            result[0]++;
            if (maze.getPixel(r, col).isExit()) break;
            r--;
        }

        r = row + 1;
        while (maze.isValid(r, col) && !maze.isWall(r, col)) {
            result[1]++;
            if (maze.getPixel(r, col).isExit()) break;
            r++;
        }

        c = col + 1;
        while (maze.isValid(row, c) && !maze.isWall(row, c)) {
            result[2]++;
            if (maze.getPixel(row, c).isExit()) break;
            c++;
        }

        c = col - 1;
        while (maze.isValid(row, c) && !maze.isWall(row, c)) {
            result[3]++;
            if (maze.getPixel(row, c).isExit()) break;
            c--;
        }

        for (int i = 0; i < 4; i++) {
            result[i] = (result[i] == 0) ? 0.00001 : result[i] * pixelsize;
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Static math helpers (same as WeibullModel)
    // -------------------------------------------------------------------------

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static double norm(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }

    public static double[] unit(double a, double b) {
        double n = norm(a, b);
        if (n < 1e-12) return new double[]{0.0, 0.0};
        return new double[]{a / n, b / n};
    }
}

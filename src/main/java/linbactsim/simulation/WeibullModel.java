package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.model.Pixel;

import java.util.List;

// Weibull-weighted run-and-tumble movement model.
// All physics extracted from SURE.Bacterium.
public class WeibullModel implements MovementModel {

    // Source: SURE.Bacterium#move(Maze, int)
    @Override
    public void step(Bacterium bacterium, Maze maze, int dt) {
        if (bacterium.hasExited()) return;

        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        double dir          = computeDirection(bacterium, maze, bacterium.getNoise());
        double verticalDir  = Math.sin(dir);
        double horizontalDir = Math.cos(dir);

        double stepDist = displacement(bacterium, dt);

        int newRow = (int)(row + verticalDir  * stepDist);
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

    // Source: SURE.Bacterium#directionWithMemory(Maze, double)
    @Override
    public double computeDirection(Bacterium bacterium, Maze maze, double noiseBound) {
        bacterium.ensureHeadingInitialized();

        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        double[] d = getDistanceToWall(maze, row, col, bacterium.getPixelsize());

        double ceo1 = weibullPDF(d[0], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // up
        double ceo2 = weibullPDF(d[1], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // down
        double ceo3 = weibullPDF(d[2], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // right
        double ceo4 = weibullPDF(d[3], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // left

        // Row increases downward, so upward pull (ceo1) means row--, hence the negation.
        double wallRow = -(ceo1 - ceo2);
        double wallCol =   ceo3 - ceo4;
        double[] wallU = unit(wallRow, wallCol);
        wallRow = wallU[0];
        wallCol = wallU[1];

        double headingRow = bacterium.getHeadingRow();
        double headingCol = bacterium.getHeadingCol();

        // Angle noise is centred on the current heading (or wall direction on the very first step).
        double currentAngle;
        if (norm(headingRow, headingCol) >= 1e-9) {
            currentAngle = Math.atan2(headingRow, headingCol);
        } else if (norm(wallRow, wallCol) >= 1e-9) {
            currentAngle = Math.atan2(wallRow, wallCol);
        } else {
            currentAngle = Math.PI / 2.0; // fallback: downward
        }

        double dTheta    = RandomNumberGenerator.getAngleNoise(noiseBound);
        double noisyAngle = currentAngle + dTheta;
        double[] noiseU  = unit(Math.sin(noisyAngle), Math.cos(noisyAngle));
        double noiseRow  = noiseU[0];
        double noiseCol  = noiseU[1];

        double combRow = bacterium.getWMemory() * headingRow + bacterium.getWNoise() * noiseRow + bacterium.getWWall() * wallRow;
        double combCol = bacterium.getWMemory() * headingCol + bacterium.getWNoise() * noiseCol + bacterium.getWWall() * wallCol;

        if (norm(combRow, combCol) < 1e-9) {
            combRow = wallRow;
            combCol = wallCol;
        }

        double[] combU = unit(combRow, combCol);
        combRow = combU[0];
        combCol = combU[1];

        // Boundary guard: clamp direction at the border ring so bacteria don't walk into it.
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

    // Source: SURE.Bacterium#weibullPDF(double, double, double, double, double)
    public double weibullPDF(double x, double k, double lambda,
                             double multiplier, double baseline) {
        if (x < 0) return 0.0;
        if (x > 20) baseline = 0;
        double part1 = k / lambda;
        double part2 = Math.pow(x / lambda, k - 1);
        double part3 = Math.exp(-Math.pow(x / lambda, k));
        return multiplier * (part1 * part2 * part3) - baseline;
    }

    // Source: SURE.Bacterium#getDistanceToWall(Maze, int, int)
    // Returns [up, down, right, left] distances to nearest wall in physical units.
    // Zero-distance directions are clamped to a small positive value to avoid division by zero in weibullPDF.
    public double[] getDistanceToWall(Maze maze, int row, int col, double pixelsize) {
        double[] result = new double[4];
        int r, c;

        // Up (row decreasing)
        r = row - 1;
        while (maze.isValid(r, col) && !maze.isWall(r, col)) {
            result[0]++;
            if (maze.getPixel(r, col).isExit()) break;
            r--;
        }

        // Down (row increasing)
        r = row + 1;
        while (maze.isValid(r, col) && !maze.isWall(r, col)) {
            result[1]++;
            if (maze.getPixel(r, col).isExit()) break;
            r++;
        }

        // Right (col increasing)
        c = col + 1;
        while (maze.isValid(row, c) && !maze.isWall(row, c)) {
            result[2]++;
            if (maze.getPixel(row, c).isExit()) break;
            c++;
        }

        // Left (col decreasing)
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

    // Source: SURE.Bacterium#applyRandomCorrection(Maze, int, int, int, int, int, double)
    public void applyRandomCorrection(Maze maze, int row, int col,
                                      int newRow, int newCol,
                                      int dt, double originalDir,
                                      Bacterium bacterium) {
        int boundary = maze.getBoundaryThickness();
        int minRow = boundary;
        int minCol = boundary;
        int maxRow = maze.getNumRows() - 1 - boundary;
        int maxCol = maze.getNumCols() - 1 - boundary;

        // Clamp target to the interior (treat border ring as walls).
        int clampedNewRow = Math.max(minRow, Math.min(newRow, maxRow));
        int clampedNewCol = Math.max(minCol, Math.min(newCol, maxCol));

        double totalDist = Math.sqrt((clampedNewRow - row) * (clampedNewRow - row)
                                   + (clampedNewCol - col) * (clampedNewCol - col));

        List<Pixel> path = maze.getPixelsOnLine(row, col, clampedNewRow, clampedNewCol);

        // Walk to the last free pixel before the collision.
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

        if (collisionPixel == null) {
            bacterium.addTime(dt);
            return;
        }

        double usedDist = Math.sqrt((lastFreeRow - row) * (lastFreeRow - row)
                                  + (lastFreeCol - col) * (lastFreeCol - col));
        double remaining = totalDist - usedDist;
        if (remaining < 0.5) {
            bacterium.addTime(dt);
            return;
        }

        // Classify wall orientation from which neighbors of the collision pixel are also walls.
        int wRow = collisionPixel.getRow();
        int wCol = collisionPixel.getCol();

        // Left/right neighbors are walls → wall band runs ——— → slide left/right
        boolean horizontalWall = maze.isWall(wRow, wCol - 1) || maze.isWall(wRow, wCol + 1);
        // Up/down neighbors are walls → wall band runs | → slide up/down
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

    // Source: SURE.Bacterium#displacement(int)
    public double displacement(Bacterium bacterium, int dt) {
        double noise = RandomNumberGenerator.getDisplacementNoise(bacterium.getStdDev());
        return (bacterium.getVelocity() + noise) * dt;
    }

    // -------------------------------------------------------------------------
    // Static math helpers — Source: SURE.Bacterium
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

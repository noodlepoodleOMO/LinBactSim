package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.model.Pixel;

import java.util.List;

// Inverse-Boltzmann force-field movement model.
// Same blend/memory/noise structure as WeibullModel; differs only in how the
// wall interaction vector is computed.
public class ForceModel implements MovementModel {

    @Override
    public void step(Bacterium bacterium, Maze maze, int dt) {
        if (bacterium.hasExited()) return;

        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        double dir           = computeDirection(bacterium, maze, bacterium.getNoise());
        double verticalDir   = Math.sin(dir);
        double horizontalDir = Math.cos(dir);

        double stepDist = bacterium.hasPendingDisplacement()
                ? bacterium.consumePendingDisplacement()
                : displacement(bacterium, dt);

        int newRow = (int)(row + verticalDir   * stepDist);
        int newCol = (int)(col + horizontalDir * stepDist);

        if (maze.isValid(newRow, newCol) && !maze.isWall(newRow, newCol)) {
            List<Pixel> path = maze.getPixelsOnLine(row, col, newRow, newCol);
            boolean collision = false;
            for (Pixel p : path) { if (p.isWall()) { collision = true; break; } }
            if (!collision) {
                for (int i = 1; i < path.size(); i++) {
                    Pixel p = path.get(i);
                    bacterium.setContinuousPosition(p.getRow(), p.getCol());
                    bacterium.recordPosition(p.getRow(), p.getCol());
                    maze.getPixel(p.getRow(), p.getCol()).addCount();
                    if (p.isExit()) { bacterium.setExited(true); bacterium.addTime(dt); return; }
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

        double[] d = getDistanceToWall(maze, row, col, bacterium.getForcePixelSize());

        double fUp    = bacterium.forceAtDistance(d[0]);
        double fDown  = bacterium.forceAtDistance(d[1]);
        double fRight = bacterium.forceAtDistance(d[2]);
        double fLeft  = bacterium.forceAtDistance(d[3]);

        double wallRow = fUp - fDown;
        double wallCol = fLeft - fRight;
        double[] wallU = unit(wallRow, wallCol);
        wallRow = wallU[0]; wallCol = wallU[1];

        double headingRow = bacterium.getHeadingRow();
        double headingCol = bacterium.getHeadingCol();

        double currentAngle;
        if (norm(headingRow, headingCol) >= 1e-9)
            currentAngle = Math.atan2(headingRow, headingCol);
        else if (norm(wallRow, wallCol) >= 1e-9)
            currentAngle = Math.atan2(wallRow, wallCol);
        else
            currentAngle = Math.PI / 2.0;

        double noiseRow, noiseCol;
        if (bacterium.hasPendingNoise()) {
            double[] pending = bacterium.consumePendingNoise();
            noiseRow = pending[0];
            noiseCol = pending[1];
        } else {
            double dTheta    = RandomNumberGenerator.getAngleNoise(noiseBound);
            double noisyAngle = currentAngle + dTheta;
            double[] noiseU  = unit(Math.sin(noisyAngle), Math.cos(noisyAngle));
            noiseRow = noiseU[0];
            noiseCol = noiseU[1];
        }

        // Store vectors for display
        bacterium.setLastVectorInfo("Force", d,
                new double[]{fUp, fDown, fRight, fLeft},
                wallRow, wallCol, noiseRow, noiseCol);

        double combRow = bacterium.getWMemory() * headingRow + bacterium.getWNoise() * noiseRow + bacterium.getWWall() * wallRow;
        double combCol = bacterium.getWMemory() * headingCol + bacterium.getWNoise() * noiseCol + bacterium.getWWall() * wallCol;

        if (norm(combRow, combCol) < 1e-9) { combRow = wallRow; combCol = wallCol; }
        double[] combU = unit(combRow, combCol);
        combRow = combU[0]; combCol = combU[1];

        int boundary = maze.getBoundaryThickness();
        boolean changed = false;
        if (row <= boundary && combRow < 0)                          { combRow = 0; changed = true; }
        if (row >= maze.getNumRows()-1-boundary && combRow > 0)      { combRow = 0; changed = true; }
        if (col <= boundary && combCol < 0)                          { combCol = 0; changed = true; }
        if (col >= maze.getNumCols()-1-boundary && combCol > 0)      { combCol = 0; changed = true; }

        if (changed) {
            double[] boundedU = unit(combRow, combCol);
            combRow = boundedU[0]; combCol = boundedU[1];
            if (norm(combRow, combCol) < 1e-9) { combRow = wallRow; combCol = wallCol; }
        }

        bacterium.setHeading(combRow, combCol);
        return Math.atan2(combRow, combCol);
    }

    // -------------------------------------------------------------------------
    // probeFullStep — dry-run: compute full next step without modifying state.
    // -------------------------------------------------------------------------
    public void probeFullStep(Bacterium bacterium, Maze maze, int dt) {
        if (bacterium.hasExited()) return;

        bacterium.ensureHeadingInitialized();
        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        double[] d = getDistanceToWall(maze, row, col, bacterium.getForcePixelSize());
        double fUp    = bacterium.forceAtDistance(d[0]);
        double fDown  = bacterium.forceAtDistance(d[1]);
        double fRight = bacterium.forceAtDistance(d[2]);
        double fLeft  = bacterium.forceAtDistance(d[3]);

        double wallRow = fUp - fDown, wallCol = fLeft - fRight;
        double[] wallU = unit(wallRow, wallCol);
        wallRow = wallU[0]; wallCol = wallU[1];

        double headingRow = bacterium.getHeadingRow();
        double headingCol = bacterium.getHeadingCol();
        double currentAngle;
        if (norm(headingRow, headingCol) >= 1e-9)
            currentAngle = Math.atan2(headingRow, headingCol);
        else if (norm(wallRow, wallCol) >= 1e-9)
            currentAngle = Math.atan2(wallRow, wallCol);
        else
            currentAngle = Math.PI / 2.0;

        double dTheta = RandomNumberGenerator.getAngleNoise(bacterium.getNoise());
        double noisyAngle = currentAngle + dTheta;
        double[] noiseU = unit(Math.sin(noisyAngle), Math.cos(noisyAngle));
        double noiseRow = noiseU[0], noiseCol = noiseU[1];

        bacterium.setPendingNoise(noiseRow, noiseCol);
        bacterium.setLastVectorInfo("Force", d,
                new double[]{fUp, fDown, fRight, fLeft},
                wallRow, wallCol, noiseRow, noiseCol);

        double combRow = bacterium.getWMemory() * headingRow + bacterium.getWNoise() * noiseRow + bacterium.getWWall() * wallRow;
        double combCol = bacterium.getWMemory() * headingCol + bacterium.getWNoise() * noiseCol + bacterium.getWWall() * wallCol;
        if (norm(combRow, combCol) < 1e-9) { combRow = wallRow; combCol = wallCol; }
        double[] cu = unit(combRow, combCol);
        combRow = cu[0]; combCol = cu[1];

        int boundary = maze.getBoundaryThickness();
        boolean changed = false;
        if (row <= boundary && combRow < 0)                          { combRow = 0; changed = true; }
        if (row >= maze.getNumRows()-1-boundary && combRow > 0)      { combRow = 0; changed = true; }
        if (col <= boundary && combCol < 0)                          { combCol = 0; changed = true; }
        if (col >= maze.getNumCols()-1-boundary && combCol > 0)      { combCol = 0; changed = true; }
        if (changed) {
            double[] bu = unit(combRow, combCol);
            combRow = bu[0]; combCol = bu[1];
            if (norm(combRow, combCol) < 1e-9) { combRow = wallRow; combCol = wallCol; }
        }
        // NOTE: heading NOT updated — pure probe

        double dir = Math.atan2(combRow, combCol);
        double verticalDir = Math.sin(dir), horizontalDir = Math.cos(dir);

        double stepDist = displacement(bacterium, dt);
        bacterium.setPendingDisplacement(stepDist);

        int newRow = (int)(row + verticalDir   * stepDist);
        int newCol = (int)(col + horizontalDir * stepDist);

        if (!maze.isValid(newRow, newCol) || maze.isWall(newRow, newCol)) {
            int b2 = maze.getBoundaryThickness();
            int cr = Math.max(b2, Math.min(newRow, maze.getNumRows()-1-b2));
            int cc = Math.max(b2, Math.min(newCol, maze.getNumCols()-1-b2));
            int[][] sd = computeSlideDestination(maze, row, col, cr, cc, dir, stepDist);
            if (sd != null)
                bacterium.setProbeResult(sd[1][0], sd[1][1], true,
                        new int[]{sd[0][0], sd[0][1]}, new int[]{sd[1][0], sd[1][1]});
            else
                bacterium.setProbeResult(row, col, true, new int[]{row, col}, new int[]{row, col});
            return;
        }

        List<Pixel> path = maze.getPixelsOnLine(row, col, newRow, newCol);
        boolean collision = false;
        for (Pixel p : path) { if (p.isWall()) { collision = true; break; } }

        if (!collision) {
            int pr = row, pc = col;
            for (int i = 1; i < path.size(); i++) { pr = path.get(i).getRow(); pc = path.get(i).getCol(); }
            bacterium.setProbeResult(pr, pc, false, null, null);
        } else {
            int b2 = maze.getBoundaryThickness();
            int cr = Math.max(b2, Math.min(newRow, maze.getNumRows()-1-b2));
            int cc = Math.max(b2, Math.min(newCol, maze.getNumCols()-1-b2));
            int[][] sd = computeSlideDestination(maze, row, col, cr, cc, dir, stepDist);
            if (sd != null)
                bacterium.setProbeResult(sd[1][0], sd[1][1], true,
                        new int[]{sd[0][0], sd[0][1]}, new int[]{sd[1][0], sd[1][1]});
            else
                bacterium.setProbeResult(row, col, true, new int[]{row, col}, new int[]{row, col});
        }
    }

    // -------------------------------------------------------------------------
    // computeSlideDestination — read-only helper (no state changes)
    // -------------------------------------------------------------------------
    private int[][] computeSlideDestination(Maze maze, int row, int col,
                                            int clampedNewRow, int clampedNewCol,
                                            double originalDir, double totalDist) {
        List<Pixel> path = maze.getPixelsOnLine(row, col, clampedNewRow, clampedNewCol);
        int lastFreeRow = row, lastFreeCol = col;
        Pixel collisionPixel = null;
        for (Pixel p : path) {
            if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) { collisionPixel = p; break; }
            lastFreeRow = p.getRow(); lastFreeCol = p.getCol();
        }
        if (collisionPixel == null) return null;

        double usedDist = Math.sqrt((lastFreeRow-row)*(lastFreeRow-row) + (lastFreeCol-col)*(lastFreeCol-col));
        double remaining = totalDist - usedDist;
        if (remaining < 0.5) return null;

        int wRow = collisionPixel.getRow(), wCol = collisionPixel.getCol();
        boolean horizontalWall = maze.isWall(wRow, wCol-1) || maze.isWall(wRow, wCol+1);
        boolean verticalWall   = maze.isWall(wRow-1, wCol) || maze.isWall(wRow+1, wCol);

        double vertDir = Math.sin(originalDir), horizDir = Math.cos(originalDir);
        int slideDirRow = 0, slideDirCol = 0;
        if (horizontalWall)   slideDirCol = (horizDir > 0 ? 1 : -1);
        else if (verticalWall) slideDirRow = (vertDir  > 0 ? 1 : -1);
        else return new int[][]{{lastFreeRow, lastFreeCol}, {lastFreeRow, lastFreeCol}};

        int sRow = lastFreeRow + (int) Math.round(slideDirRow * remaining);
        int sCol = lastFreeCol + (int) Math.round(slideDirCol * remaining);

        List<Pixel> slidePath = maze.getPixelsOnLine(lastFreeRow, lastFreeCol, sRow, sCol);
        int slideEndRow = lastFreeRow, slideEndCol = lastFreeCol;
        for (Pixel p : slidePath) {
            if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) break;
            slideEndRow = p.getRow(); slideEndCol = p.getCol();
        }
        return new int[][]{{lastFreeRow, lastFreeCol}, {slideEndRow, slideEndCol}};
    }

    public void applyRandomCorrection(Maze maze, int row, int col,
                                      int newRow, int newCol,
                                      int dt, double originalDir,
                                      Bacterium bacterium) {
        int boundary = maze.getBoundaryThickness();
        int clampedNewRow = Math.max(boundary, Math.min(newRow, maze.getNumRows()-1-boundary));
        int clampedNewCol = Math.max(boundary, Math.min(newCol, maze.getNumCols()-1-boundary));
        double totalDist = Math.sqrt((clampedNewRow-row)*(clampedNewRow-row) + (clampedNewCol-col)*(clampedNewCol-col));

        List<Pixel> path = maze.getPixelsOnLine(row, col, clampedNewRow, clampedNewCol);
        int lastFreeRow = row, lastFreeCol = col;
        Pixel collisionPixel = null;
        for (Pixel p : path) {
            if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) { collisionPixel = p; break; }
            lastFreeRow = p.getRow(); lastFreeCol = p.getCol();
            bacterium.setContinuousPosition(lastFreeRow, lastFreeCol);
            bacterium.recordPosition(lastFreeRow, lastFreeCol);
            maze.getPixel(lastFreeRow, lastFreeCol).addCount();
        }
        if (collisionPixel == null) { bacterium.addTime(dt); return; }

        double usedDist = Math.sqrt((lastFreeRow-row)*(lastFreeRow-row) + (lastFreeCol-col)*(lastFreeCol-col));
        double remaining = totalDist - usedDist;
        if (remaining < 0.5) { bacterium.addTime(dt); return; }

        int wRow = collisionPixel.getRow(), wCol = collisionPixel.getCol();
        boolean horizontalWall = maze.isWall(wRow, wCol-1) || maze.isWall(wRow, wCol+1);
        boolean verticalWall   = maze.isWall(wRow-1, wCol) || maze.isWall(wRow+1, wCol);

        double vertDir = Math.sin(originalDir), horizDir = Math.cos(originalDir);
        int slideDirRow = 0, slideDirCol = 0;
        if (horizontalWall)   slideDirCol = (horizDir > 0 ? 1 : -1);
        else if (verticalWall) slideDirRow = (vertDir  > 0 ? 1 : -1);
        else { bacterium.addTime(dt); return; }

        int sRow = lastFreeRow + (int) Math.round(slideDirRow * remaining);
        int sCol = lastFreeCol + (int) Math.round(slideDirCol * remaining);

        List<Pixel> slidePath = maze.getPixelsOnLine(lastFreeRow, lastFreeCol, sRow, sCol);
        for (Pixel p : slidePath) {
            if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) break;
            bacterium.setContinuousPosition(p.getRow(), p.getCol());
            bacterium.recordPosition(p.getRow(), p.getCol());
            maze.getPixel(p.getRow(), p.getCol()).addCount();
            if (p.isExit()) { bacterium.setExited(true); bacterium.addTime(dt); return; }
        }
        bacterium.addTime(dt);
    }

    public double displacement(Bacterium bacterium, int dt) {
        double noise = RandomNumberGenerator.getDisplacementNoise(bacterium.getStdDev());
        return (bacterium.getVelocity() + noise) * dt;
    }

    public double[] getDistanceToWall(Maze maze, int row, int col, double pixelsize) {
        double[] result = new double[4];
        int r, c;
        r = row - 1;
        while (maze.isValid(r, col) && !maze.isWall(r, col)) { result[0]++; if (maze.getPixel(r, col).isExit()) break; r--; }
        r = row + 1;
        while (maze.isValid(r, col) && !maze.isWall(r, col)) { result[1]++; if (maze.getPixel(r, col).isExit()) break; r++; }
        c = col + 1;
        while (maze.isValid(row, c) && !maze.isWall(row, c)) { result[2]++; if (maze.getPixel(row, c).isExit()) break; c++; }
        c = col - 1;
        while (maze.isValid(row, c) && !maze.isWall(row, c)) { result[3]++; if (maze.getPixel(row, c).isExit()) break; c--; }
        for (int i = 0; i < 4; i++) result[i] = (result[i] == 0) ? 0.00001 : result[i] * pixelsize;
        return result;
    }

    public static double norm(double a, double b)  { return Math.sqrt(a*a + b*b); }

    public static double[] unit(double a, double b) {
        double n = norm(a, b);
        if (n < 1e-12) return new double[]{0.0, 0.0};
        return new double[]{a/n, b/n};
    }
}

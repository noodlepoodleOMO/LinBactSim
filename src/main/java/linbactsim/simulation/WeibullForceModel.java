package linbactsim.simulation;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.model.Pixel;

import java.util.ArrayList;
import java.util.List;

// WeibullModel with ForceModel4Ray wall-collision handling:
// direction computation is identical to WeibullModel; applyRandomCorrection
// adds scheduleConcaveNoiseBurst() on concave stops, and computeDirection
// handles the resulting noise burst the same way ForceModel4Ray does.
public class WeibullForceModel implements MovementModel {

    private static final double CONCAVE_NOISE_BURST_FACTOR = 5.0;

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

        double[] d = getDistanceToWall(maze, row, col, bacterium.getWeibullPixelSize());

        double ceo1 = weibullPDF(d[0], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // up
        double ceo2 = weibullPDF(d[1], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // down
        double ceo3 = weibullPDF(d[2], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // right
        double ceo4 = weibullPDF(d[3], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline()); // left

        double wallRow = -(ceo1 - ceo2);
        double wallCol =   ceo3 - ceo4;
        double[] wallU = unit(wallRow, wallCol);
        wallRow = wallU[0];
        wallCol = wallU[1];

        double headingRow = bacterium.getHeadingRow();
        double headingCol = bacterium.getHeadingCol();

        double currentAngle;
        if (norm(headingRow, headingCol) >= 1e-9)
            currentAngle = Math.atan2(headingRow, headingCol);
        else if (norm(wallRow, wallCol) >= 1e-9)
            currentAngle = Math.atan2(wallRow, wallCol);
        else
            currentAngle = Math.random() * 2.0 * Math.PI;

        double noiseRow, noiseCol;
        if (bacterium.hasConcaveNoiseBurst()) {
            if (bacterium.hasPendingNoise()) bacterium.consumePendingNoise();
            double dTheta     = RandomNumberGenerator.getAngleNoise(Math.PI);
            double noisyAngle = currentAngle + dTheta;
            double[] noiseU   = unit(Math.sin(noisyAngle), Math.cos(noisyAngle));
            noiseRow = noiseU[0];
            noiseCol = noiseU[1];
        } else if (bacterium.hasPendingNoise()) {
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

        bacterium.setLastVectorInfo("WeibullF", d,
                new double[]{ceo1, ceo2, ceo3, ceo4},
                wallRow, wallCol, noiseRow, noiseCol);

        double effWMemory, effWNoise, effWWall;
        if (bacterium.hasConcaveNoiseBurst()) {
            double rawMem   = bacterium.getWMemory();
            double rawNoise = bacterium.getWNoise() * CONCAVE_NOISE_BURST_FACTOR;
            double rawWall  = bacterium.getWWall();
            double sum      = rawMem + rawNoise + rawWall;
            effWMemory = rawMem   / sum;
            effWNoise  = rawNoise / sum;
            effWWall   = rawWall  / sum;
            bacterium.consumeConcaveNoiseBurst();
        } else {
            effWMemory = bacterium.getWMemory();
            effWNoise  = bacterium.getWNoise();
            effWWall   = bacterium.getWWall();
        }

        double combRow = effWMemory * headingRow + effWNoise * noiseRow + effWWall * wallRow;
        double combCol = effWMemory * headingCol + effWNoise * noiseCol + effWWall * wallCol;

        if (norm(combRow, combCol) < 1e-9) { combRow = wallRow; combCol = wallCol; }

        double[] combU = unit(combRow, combCol);
        combRow = combU[0];
        combCol = combU[1];

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

    public void probeFullStep(Bacterium bacterium, Maze maze, int dt) {
        if (bacterium.hasExited()) return;
        bacterium.setProbeSlideInfo(null);
        bacterium.setLastProbedWallPixels(null);

        bacterium.ensureHeadingInitialized();
        int[] pos = bacterium.getPosition();
        int row = pos[0], col = pos[1];

        double[] d = getDistanceToWall(maze, row, col, bacterium.getWeibullPixelSize());

        List<int[]> probedPixels = new ArrayList<>();
        int[] wUp    = getFirstWallPixel(maze, row, col, -1,  0); if (wUp    != null) probedPixels.add(wUp);
        int[] wDown  = getFirstWallPixel(maze, row, col,  1,  0); if (wDown  != null) probedPixels.add(wDown);
        int[] wRight = getFirstWallPixel(maze, row, col,  0,  1); if (wRight != null) probedPixels.add(wRight);
        int[] wLeft  = getFirstWallPixel(maze, row, col,  0, -1); if (wLeft  != null) probedPixels.add(wLeft);
        bacterium.setLastProbedWallPixels(probedPixels);

        double ceo1 = weibullPDF(d[0], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline());
        double ceo2 = weibullPDF(d[1], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline());
        double ceo3 = weibullPDF(d[2], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline());
        double ceo4 = weibullPDF(d[3], bacterium.getK(), bacterium.getLambda(), bacterium.getMultiplier(), bacterium.getBaseline());

        double wallRow = -(ceo1 - ceo2), wallCol = ceo3 - ceo4;
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
            currentAngle = Math.random() * 2.0 * Math.PI;

        double dTheta = RandomNumberGenerator.getAngleNoise(bacterium.getNoise());
        double noisyAngle = currentAngle + dTheta;
        double[] noiseU = unit(Math.sin(noisyAngle), Math.cos(noisyAngle));
        double noiseRow = noiseU[0], noiseCol = noiseU[1];

        bacterium.setPendingNoise(noiseRow, noiseCol);
        bacterium.setLastVectorInfo("WeibullF", d,
                new double[]{ceo1, ceo2, ceo3, ceo4},
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

        double dir = Math.atan2(combRow, combCol);
        double verticalDir = Math.sin(dir), horizontalDir = Math.cos(dir);

        double stepDist = displacement(bacterium, dt);
        bacterium.setLastSampledDisplacement(stepDist);
        bacterium.setPendingDisplacement(stepDist);

        int newRow = (int)(row + verticalDir   * stepDist);
        int newCol = (int)(col + horizontalDir * stepDist);

        if (!maze.isValid(newRow, newCol) || maze.isWall(newRow, newCol)) {
            int b2 = maze.getBoundaryThickness();
            int cr = Math.max(b2, Math.min(newRow, maze.getNumRows()-1-b2));
            int cc = Math.max(b2, Math.min(newCol, maze.getNumCols()-1-b2));
            int[][] sd = computeSlideDestination(maze, row, col, cr, cc, dir, stepDist, bacterium);
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
            int[][] sd = computeSlideDestination(maze, row, col, cr, cc, dir, stepDist, bacterium);
            if (sd != null)
                bacterium.setProbeResult(sd[1][0], sd[1][1], true,
                        new int[]{sd[0][0], sd[0][1]}, new int[]{sd[1][0], sd[1][1]});
            else
                bacterium.setProbeResult(row, col, true, new int[]{row, col}, new int[]{row, col});
        }
    }

    private int[][] computeSlideDestination(Maze maze, int row, int col,
                                            int clampedNewRow, int clampedNewCol,
                                            double originalDir, double totalDist,
                                            Bacterium bacterium) {
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

        int slideEndRow = lastFreeRow, slideEndCol = lastFreeCol;
        StringBuilder slideLog = new StringBuilder();
        int maxSlides = 5;

        while (remaining > 0.5 && maxSlides-- > 0) {
            int wRow = collisionPixel.getRow(), wCol = collisionPixel.getCol();
            int dRow = wRow - lastFreeRow, dCol = wCol - lastFreeCol;
            double vertDir = Math.sin(originalDir), horizDir = Math.cos(originalDir);
            int slideDirRow = 0, slideDirCol = 0;
            String entryTag;

            if (dRow == 0) {
                int primary = (vertDir >= 0 ? 1 : -1);
                if (!maze.isWall(lastFreeRow + primary, lastFreeCol)) {
                    entryTag = "horiz"; slideDirRow = primary;
                } else {
                    if (slideLog.length() > 0) slideLog.append("\n");
                    slideLog.append(wRow).append(",").append(wCol).append(";")
                            .append(lastFreeRow).append(",").append(lastFreeCol)
                            .append(";concave→stop");
                    break;
                }
            } else if (dCol == 0) {
                int primary = (horizDir >= 0 ? 1 : -1);
                if (!maze.isWall(lastFreeRow, lastFreeCol + primary)) {
                    entryTag = "vert"; slideDirCol = primary;
                } else {
                    if (slideLog.length() > 0) slideLog.append("\n");
                    slideLog.append(wRow).append(",").append(wCol).append(";")
                            .append(lastFreeRow).append(",").append(lastFreeCol)
                            .append(";concave→stop");
                    break;
                }
            } else {
                boolean canSlideRow = !maze.isWall(wRow, wCol - dCol);
                boolean canSlideCol = !maze.isWall(wRow - dRow, wCol);
                if (canSlideRow && !canSlideCol) {
                    entryTag = "diag"; slideDirRow = (vertDir >= 0 ? 1 : -1);
                } else if (canSlideCol && !canSlideRow) {
                    entryTag = "diag"; slideDirCol = (horizDir >= 0 ? 1 : -1);
                } else if (canSlideRow) {
                    entryTag = "diag(corner)";
                    if (RandomNumberGenerator.getRandomPosition(2) == 0) slideDirRow = (vertDir  >= 0 ? 1 : -1);
                    else                                                  slideDirCol = (horizDir >= 0 ? 1 : -1);
                } else {
                    if (slideLog.length() > 0) slideLog.append("\n");
                    slideLog.append(wRow).append(",").append(wCol).append(";")
                            .append(lastFreeRow).append(",").append(lastFreeCol)
                            .append(";concave→stop");
                    break;
                }
            }

            String dirTag = (slideDirRow > 0) ? "↓" : (slideDirRow < 0) ? "↑"
                          : (slideDirCol > 0) ? "→" : "←";
            if (slideLog.length() > 0) slideLog.append("\n");
            slideLog.append(wRow).append(",").append(wCol).append(";")
                    .append(lastFreeRow).append(",").append(lastFreeCol).append(";")
                    .append(entryTag).append(" ").append(dirTag);

            int sRow = lastFreeRow + (int) Math.round(slideDirRow * remaining);
            int sCol = lastFreeCol + (int) Math.round(slideDirCol * remaining);
            List<Pixel> slidePath = maze.getPixelsOnLine(lastFreeRow, lastFreeCol, sRow, sCol);
            int curEndRow = lastFreeRow, curEndCol = lastFreeCol;
            Pixel nextCollision = null;
            for (Pixel p : slidePath) {
                if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) { nextCollision = p; break; }
                curEndRow = p.getRow(); curEndCol = p.getCol();
            }
            slideEndRow = curEndRow; slideEndCol = curEndCol;

            if (slideEndRow == lastFreeRow && slideEndCol == lastFreeCol) {
                slideLog.append("(blocked)"); break;
            }
            if (nextCollision == null) { remaining = 0; break; }

            double slidedDist = Math.sqrt((slideEndRow - lastFreeRow) * (double)(slideEndRow - lastFreeRow)
                                        + (slideEndCol - lastFreeCol) * (double)(slideEndCol - lastFreeCol));
            remaining -= slidedDist;
            lastFreeRow = slideEndRow; lastFreeCol = slideEndCol;
            collisionPixel = nextCollision;
            originalDir = Math.atan2(slideDirRow, slideDirCol);
        }

        if (bacterium != null) bacterium.setProbeSlideInfo(slideLog.length() > 0 ? slideLog.toString() : null);
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

        int maxSlides = 5;
        while (remaining > 0.5 && maxSlides-- > 0) {
            int wRow = collisionPixel.getRow(), wCol = collisionPixel.getCol();
            int dRow = wRow - lastFreeRow, dCol = wCol - lastFreeCol;
            double vertDir = Math.sin(originalDir), horizDir = Math.cos(originalDir);
            int slideDirRow = 0, slideDirCol = 0;
            boolean concave = false;

            if (dRow == 0) {
                int primary = (vertDir >= 0 ? 1 : -1);
                if (!maze.isWall(lastFreeRow + primary, lastFreeCol)) { slideDirRow = primary; }
                else                                                   { concave = true; }
            } else if (dCol == 0) {
                int primary = (horizDir >= 0 ? 1 : -1);
                if (!maze.isWall(lastFreeRow, lastFreeCol + primary)) { slideDirCol = primary; }
                else                                                   { concave = true; }
            } else {
                boolean canSlideRow = !maze.isWall(wRow, wCol - dCol);
                boolean canSlideCol = !maze.isWall(wRow - dRow, wCol);
                if      (canSlideRow && !canSlideCol) { slideDirRow = (vertDir  >= 0 ? 1 : -1); }
                else if (canSlideCol && !canSlideRow) { slideDirCol = (horizDir >= 0 ? 1 : -1); }
                else if (canSlideRow) {
                    if (RandomNumberGenerator.getRandomPosition(2) == 0) slideDirRow = (vertDir  >= 0 ? 1 : -1);
                    else                                                  slideDirCol = (horizDir >= 0 ? 1 : -1);
                } else { concave = true; }
            }

            if (concave) { bacterium.setHeading(0.0, 0.0); bacterium.scheduleConcaveNoiseBurst(); break; }

            int sRow = lastFreeRow + (int) Math.round(slideDirRow * remaining);
            int sCol = lastFreeCol + (int) Math.round(slideDirCol * remaining);
            List<Pixel> slidePath = maze.getPixelsOnLine(lastFreeRow, lastFreeCol, sRow, sCol);
            int slideEndRow = lastFreeRow, slideEndCol = lastFreeCol;
            Pixel nextCollision = null;
            for (Pixel p : slidePath) {
                if (!maze.isValid(p.getRow(), p.getCol()) || p.isWall()) { nextCollision = p; break; }
                slideEndRow = p.getRow(); slideEndCol = p.getCol();
                bacterium.setContinuousPosition(slideEndRow, slideEndCol);
                bacterium.recordPosition(slideEndRow, slideEndCol);
                maze.getPixel(slideEndRow, slideEndCol).addCount();
                if (p.isExit()) { bacterium.setExited(true); bacterium.addTime(dt); return; }
            }

            if (slideEndRow == lastFreeRow && slideEndCol == lastFreeCol) {
                bacterium.setHeading(0.0, 0.0); bacterium.scheduleConcaveNoiseBurst(); break;
            }
            if (nextCollision == null) { remaining = 0; break; }

            double slidedDist = Math.sqrt((slideEndRow - lastFreeRow) * (double)(slideEndRow - lastFreeRow)
                                        + (slideEndCol - lastFreeCol) * (double)(slideEndCol - lastFreeCol));
            remaining -= slidedDist;
            lastFreeRow = slideEndRow; lastFreeCol = slideEndCol;
            collisionPixel = nextCollision;
            originalDir = Math.atan2(slideDirRow, slideDirCol);
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

    private static int[] getFirstWallPixel(Maze maze, int row, int col, int dr, int dc) {
        int r = row + dr, c = col + dc;
        while (maze.isValid(r, c) && !maze.isWall(r, c)) {
            if (maze.getPixel(r, c).isExit()) return null;
            r += dr; c += dc;
        }
        if (maze.isValid(r, c) && maze.isWall(r, c)) return new int[]{r, c};
        return null;
    }

    public double weibullPDF(double x, double k, double lambda,
                             double multiplier, double baseline) {
        if (x < 0) return 0.0;
        if (x > 20) baseline = 0;
        double part1 = k / lambda;
        double part2 = Math.pow(x / lambda, k - 1);
        double part3 = Math.exp(-Math.pow(x / lambda, k));
        return multiplier * (part1 * part2 * part3) - baseline;
    }

    public static double norm(double a, double b)  { return Math.sqrt(a*a + b*b); }

    public static double[] unit(double a, double b) {
        double n = norm(a, b);
        if (n < 1e-12) return new double[]{0.0, 0.0};
        return new double[]{a/n, b/n};
    }
}

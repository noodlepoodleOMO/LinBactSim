package linbactsim.gui;

import linbactsim.analysis.RAG;
import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.model.Pixel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

// Source: SURE.MazePanel — direct carry with the following changes:
//   • maze.getDisplayPixelSize() replaces maze.getPixelSize()
//   • SkeletonAnalyzer replaced by RAG
//   • Second JWindow added for bacterium hover info (non-overlapping with pixel window)
public class MazePanel extends JPanel {

    private Maze maze;
    private boolean showGrid, showSkeleton, showVoronoi, showTrajectories, showDegrees;
    private RAG rag;

    public enum EditMode { SELECT, WALL, EXIT, ERASE_WALL, REMEDY_VORONOI, DELETE_JUNCTION }
    private EditMode currentMode = EditMode.SELECT;

    private int  remedySelectedJunctionId = -1;
    private boolean remedyDragging        = false;
    private int  hoveredBacteriumIdx      = -1;

    // Pixel hover window
    private JWindow hoverInfoWindow;
    private JLabel  hoverLabel;

    // Bacterium hover window — positioned to the right of the pixel window
    private JWindow bacteriumHoverWindow;
    private JLabel  bacteriumLabel;

    private int lastRow = -1, lastCol = -1;
    private int hoveredRow = -1, hoveredCol = -1;
    private int selectedRow = -1, selectedCol = -1;
    private boolean popupPinned = false;
    private final Set<Point> selectedCells = new HashSet<>();

    private int startRow = -1, startCol = -1;
    private List<Point> exitPoints = new ArrayList<>();
    private boolean isDragging = false;

    private Consumer<Point> exitPointListener;
    private Consumer<Point> startPointListener;

    public MazePanel(Maze maze) {
        this.maze     = maze;
        this.showGrid = true;

        updatePreferredSize();
        setToolTipText(" ");
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        ToolTipManager.sharedInstance().setInitialDelay(200);

        // --- Pixel info hover window ---
        hoverInfoWindow = new JWindow();
        hoverLabel      = new JLabel();
        hoverLabel.setOpaque(true);
        hoverLabel.setBackground(new Color(255, 255, 200));
        hoverLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        hoverLabel.setFont(hoverLabel.getFont().deriveFont(11f));

        JButton setStartButton = new JButton("Set Start");
        JButton setExitButton  = new JButton("Set Exit");
        JButton setWallButton  = new JButton("Set Wall");

        JPanel popupPanel  = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        buttonPanel.add(setStartButton);
        buttonPanel.add(setExitButton);
        buttonPanel.add(setWallButton);
        popupPanel.add(hoverLabel, BorderLayout.CENTER);
        popupPanel.add(buttonPanel, BorderLayout.SOUTH);
        hoverInfoWindow.getContentPane().add(popupPanel);
        hoverInfoWindow.setAlwaysOnTop(true);
        hoverInfoWindow.pack();

        // --- Bacterium info hover window ---
        bacteriumHoverWindow = new JWindow();
        bacteriumLabel = new JLabel();
        bacteriumLabel.setOpaque(true);
        bacteriumLabel.setBackground(new Color(220, 245, 255));
        bacteriumLabel.setBorder(BorderFactory.createLineBorder(new Color(100, 160, 200)));
        bacteriumLabel.setFont(bacteriumLabel.getFont().deriveFont(11f));
        bacteriumLabel.setVerticalAlignment(JLabel.TOP);
        JPanel bPopup = new JPanel(new BorderLayout());
        bPopup.add(bacteriumLabel, BorderLayout.CENTER);
        bacteriumHoverWindow.getContentPane().add(bPopup);
        bacteriumHoverWindow.setAlwaysOnTop(true);
        bacteriumHoverWindow.pack();

        setStartButton.addActionListener(e -> {
            setEditMode(EditMode.SELECT);
            if (selectedRow >= 0 && selectedCol >= 0) {
                startRow = selectedRow; startCol = selectedCol;
                maze.getPixel(startRow, startCol).setWall(false);
                maze.getPixel(startRow, startCol).setExit(false);
                exitPoints.removeIf(p -> p.x == startCol && p.y == startRow);
                if (startPointListener != null)
                    startPointListener.accept(new Point(startCol, startRow));
                hideHoverWindow(); repaint();
            }
        });
        setExitButton.addActionListener(e -> {
            if (selectedRow < 0 && selectedCells.isEmpty()) return;
            List<Point> targets = selectedCells.isEmpty()
                    ? List.of(new Point(selectedCol, selectedRow))
                    : new ArrayList<>(selectedCells);
            for (Point cell : targets) applyExitAt(cell.y, cell.x);
            hideHoverWindow(); repaint();
        });
        setWallButton.addActionListener(e -> {
            if (selectedRow < 0 && selectedCells.isEmpty()) return;
            List<Point> targets = selectedCells.isEmpty()
                    ? List.of(new Point(selectedCol, selectedRow))
                    : new ArrayList<>(selectedCells);
            for (Point cell : targets) applyWallAt(cell.y, cell.x);
            hideHoverWindow(); repaint();
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int cs  = maze.getDisplayPixelSize();
                int col = e.getX() / cs, row = e.getY() / cs;
                if (maze.isValid(row, col)) {
                    hoveredRow = row; hoveredCol = col;
                    if (!popupPinned && currentMode == EditMode.SELECT) {
                        hoverLabel.setText(buildCellHtml(row, col));
                        Point sp = e.getPoint();
                        SwingUtilities.convertPointToScreen(sp, MazePanel.this);
                        hoverInfoWindow.setLocation(sp.x + 15, sp.y + 15);
                        hoverInfoWindow.pack();
                        hoverInfoWindow.setVisible(true);
                    }
                    lastRow = row; lastCol = col;
                } else {
                    hoveredRow = -1; hoveredCol = -1;
                    if (!popupPinned) hideHoverWindow();
                }

                int bIdx = findHoveredBacterium(e.getX(), e.getY());
                if (bIdx != hoveredBacteriumIdx) { hoveredBacteriumIdx = bIdx; repaint(); }

                if (bIdx >= 0) {
                    bacteriumLabel.setText(buildBacteriumHtml(bIdx));
                    Point sp = e.getPoint();
                    SwingUtilities.convertPointToScreen(sp, MazePanel.this);
                    bacteriumHoverWindow.setLocation(sp.x + 220, sp.y + 15);
                    bacteriumHoverWindow.pack();
                    bacteriumHoverWindow.setVisible(true);
                } else {
                    bacteriumHoverWindow.setVisible(false);
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                int cs  = maze.getDisplayPixelSize();
                int col = e.getX() / cs, row = e.getY() / cs;
                if (!maze.isValid(row, col)) return;
                if (isDragging && isPaintMode()) {
                    switch (currentMode) {
                        case WALL       -> applyWallAt(row, col);
                        case EXIT       -> applyExitAt(row, col);
                        case ERASE_WALL -> applyEraseWallAt(row, col);
                        default -> {}
                    }
                    repaint();
                }
                if (remedyDragging && currentMode == EditMode.REMEDY_VORONOI
                        && rag != null && rag.isReady()
                        && remedySelectedJunctionId != -1) {
                    rag.getVoronoi().reassignRegion(row, col, remedySelectedJunctionId, maze);
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int cs  = maze.getDisplayPixelSize();
                int col = e.getX() / cs, row = e.getY() / cs;
                if (!maze.isValid(row, col)) return;

                if (currentMode == EditMode.REMEDY_VORONOI && rag != null && rag.isReady()) {
                    remedyDragging = true;
                    return;
                }

                if (currentMode == EditMode.DELETE_JUNCTION && rag != null && rag.isReady()) {
                    int[][] rm = rag.getRegionMap();
                    if (rm != null) {
                        int id = rm[row][col];
                        if (id > 0) { rag.deleteJunction(id, maze); repaint(); }
                    }
                    return;
                }

                if (isPaintMode()) {
                    isDragging = true;
                    switch (currentMode) {
                        case WALL       -> applyWallAt(row, col);
                        case EXIT       -> applyExitAt(row, col);
                        case ERASE_WALL -> applyEraseWallAt(row, col);
                        default -> {}
                    }
                    repaint();
                }
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int cs  = maze.getDisplayPixelSize();
                int col = e.getX() / cs, row = e.getY() / cs;

                if (currentMode == EditMode.REMEDY_VORONOI && rag != null && rag.isReady()) {
                    int[][] rm = rag.getRegionMap();
                    if (rm != null && maze.isValid(row, col)) {
                        remedySelectedJunctionId = rm[row][col];
                        repaint();
                    }
                    return;
                }

                if (currentMode != EditMode.SELECT) return;
                if (!maze.isValid(row, col)) { hideHoverWindow(); repaint(); return; }

                if (e.isShiftDown()) {
                    Point cell = new Point(col, row);
                    if (selectedCells.contains(cell)) selectedCells.remove(cell);
                    else selectedCells.add(cell);
                } else {
                    selectedCells.clear();
                    selectedCells.add(new Point(col, row));
                }
                selectedRow = row; selectedCol = col;
                hoverLabel.setText(buildCellHtml(row, col));
                Point sp = e.getPoint();
                SwingUtilities.convertPointToScreen(sp, MazePanel.this);
                hoverInfoWindow.setLocation(sp.x + 15, sp.y + 15);
                hoverInfoWindow.pack();
                hoverInfoWindow.setVisible(true);
                popupPinned = true;
                repaint();
            }

            @Override public void mouseReleased(MouseEvent e) { isDragging = false; remedyDragging = false; }
            @Override public void mouseExited(MouseEvent e) {
                isDragging = false;
                hoveredRow = -1; hoveredCol = -1;
                if (hoveredBacteriumIdx != -1) {
                    hoveredBacteriumIdx = -1;
                    bacteriumHoverWindow.setVisible(false);
                    repaint();
                }
                if (!popupPinned) hideHoverWindow();
            }
        });

        setFocusable(true);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "hidePopup");
        getActionMap().put("hidePopup", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                hideHoverWindow(); setEditMode(EditMode.SELECT); repaint();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Tooltip (kept for accessibility; bacterium info now in the 2nd window)
    // -------------------------------------------------------------------------
    @Override
    public String getToolTipText(MouseEvent e) {
        int cs  = maze.getDisplayPixelSize();
        int col = e.getX() / cs, row = e.getY() / cs;
        if (!maze.isValid(row, col) || maze.isWall(row, col)) return null;
        return null; // bacterium info shown in bacteriumHoverWindow instead
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cs   = maze.getDisplayPixelSize();
        int rows = maze.getNumRows();
        int cols = maze.getNumCols();

        // 1. Base maze
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                g.setColor(maze.isWall(r, c) ? Color.BLACK : Color.WHITE);
                g.fillRect(c * cs, r * cs, cs, cs);
                g.setColor(showGrid ? Color.GRAY : (maze.isWall(r, c) ? Color.BLACK : Color.WHITE));
                g.drawRect(c * cs, r * cs, cs, cs);
            }
        }

        // 2. Voronoi overlay (semi-transparent)
        if (showVoronoi && rag != null && rag.isReady()) {
            int[][] rm = rag.getRegionMap();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (maze.isWall(r, c) || rm == null) continue;
                    int id = rm[r][c];
                    if (id == 0) continue;
                    Color base = rag.getRegionColor(id);
                    g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 80));
                    g.fillRect(c * cs, r * cs, cs, cs);
                }
            }
        }

        // 2b. REMEDY_VORONOI: highlight selected region at full opacity
        if (currentMode == EditMode.REMEDY_VORONOI && remedySelectedJunctionId != -1
                && rag != null && rag.isReady()) {
            int[][] rm = rag.getRegionMap();
            if (rm != null) {
                Color base = rag.getRegionColor(remedySelectedJunctionId);
                for (int r = 0; r < rows; r++)
                    for (int c = 0; c < cols; c++)
                        if (rm[r][c] == remedySelectedJunctionId) {
                            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 200));
                            g.fillRect(c * cs, r * cs, cs, cs);
                        }
            }
        }

        // 3. Skeleton overlay (light red)
        if (showSkeleton && rag != null && rag.hasSkeleton()) {
            boolean[][] skel = rag.getSkeleton().getSkeleton();
            g.setColor(new Color(255, 80, 80, 160));
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    if (skel != null && skel[r][c])
                        g.fillRect(c * cs, r * cs, cs, cs);
        }

        // 4. Degree overlay (color by degree: 1=green, 2=blue, 3+=red)
        if (showDegrees && rag != null && rag.hasSkeleton()) {
            boolean[][] skel = rag.getSkeleton().getSkeleton();
            int[][]     deg  = rag.getSkeleton().getDegree();
            if (skel != null && deg != null) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (!skel[r][c]) continue;
                        int d = deg[r][c];
                        Color dc = d == 1 ? new Color(0,200,0,200)
                                 : d == 2 ? new Color(0,100,255,200)
                                 :          new Color(255,0,0,200);
                        g.setColor(dc);
                        g.fillRect(c * cs, r * cs, cs, cs);
                    }
                }
            }
        }

        // 5. Start cell highlight
        if (startRow >= 0 && startCol >= 0) {
            g.setColor(new Color(255, 255, 0, 128));
            g.fillRect(startCol * cs, startRow * cs, cs, cs);
            g.setColor(Color.RED);
            g.drawRect(startCol * cs, startRow * cs, cs, cs);
        }

        // 6. Exit cells
        for (Point ep : exitPoints) {
            g.setColor(new Color(0, 255, 0, 120));
            g.fillRect(ep.x * cs, ep.y * cs, cs, cs);
            g.setColor(new Color(0, 128, 0));
            g.drawRect(ep.x * cs, ep.y * cs, cs, cs);
        }

        // 7. Selected cell(s)
        if (selectedRow >= 0 && selectedCol >= 0) {
            g2.setColor(Color.CYAN);
            g2.setStroke(new BasicStroke(2f));
            if (!selectedCells.isEmpty())
                for (Point cell : selectedCells)
                    g2.drawRect(cell.x * cs, cell.y * cs, cs, cs);
            else
                g2.drawRect(selectedCol * cs, selectedRow * cs, cs, cs);
        }

        // 8. Trajectory lines
        g2.setStroke(new BasicStroke(1f));
        if (showTrajectories) {
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                if (i == hoveredBacteriumIdx) continue;
                drawTrajectory(g2, i, cs, 1.5f, 0.75f);
            }
        }
        if (hoveredBacteriumIdx >= 0 && hoveredBacteriumIdx < maze.getBacteriaCount())
            drawTrajectory(g2, hoveredBacteriumIdx, cs, 3.5f, 1.0f);

        // 9. Bacteria ovals
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            int[] pos   = b.getPosition();
            g.setColor(b.getColor());
            g.fillOval(pos[1] * cs, pos[0] * cs, cs, cs);
            if (i == hoveredBacteriumIdx) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(pos[1] * cs, pos[0] * cs, cs, cs);
                g2.setStroke(new BasicStroke(1f));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Bacterium hover HTML
    // -------------------------------------------------------------------------
    private String buildBacteriumHtml(int bIdx) {
        Bacterium b = maze.getBacterium(bIdx);
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<b>Bacterium ").append(bIdx + 1).append("</b>");
        sb.append(" | ").append(b.getSpecies().getName());
        sb.append(" | Steps: ").append(b.getTime());
        sb.append(" | Exited: ").append(b.hasExited());

        if (b.isLastVectorsSet()) {
            String model = b.getLastModelType() != null ? b.getLastModelType() : "?";
            sb.append("<br><b>--- ").append(model).append(" vectors (next step preview) ---</b>");

            double[] dist = b.getLastRawDistances();
            sb.append("<br>Distances: up=").append(fmt(dist[0]))
              .append(" dn=").append(fmt(dist[1]))
              .append(" rt=").append(fmt(dist[2]))
              .append(" lt=").append(fmt(dist[3]));

            double[] raw = b.getLastRawValues();
            String rawLabel = "Weibull".equals(model) ? "PDF" : "Force";
            sb.append("<br>").append(rawLabel).append(": up=").append(fmt(raw[0]))
              .append(" dn=").append(fmt(raw[1]))
              .append(" rt=").append(fmt(raw[2]))
              .append(" lt=").append(fmt(raw[3]));

            double[] wall = b.getLastWallVector();
            sb.append("<br>Wall vec: (r=").append(fmt(wall[0])).append(", c=").append(fmt(wall[1])).append(")");

            double[] noise = b.getLastNoiseVector();
            sb.append("<br>Noise vec: (r=").append(fmt(noise[0])).append(", c=").append(fmt(noise[1])).append(")");

            sb.append("<br>Inertia:   (r=").append(fmt(b.getHeadingRow()))
              .append(", c=").append(fmt(b.getHeadingCol())).append(")");
            sb.append("<br>Step dist: ").append(fmt(b.getLastSampledDisplacement())).append(" px");

            int[] probe = b.getProbeNextPos();
            if (probe != null) {
                if (b.isProbeHadCollision()) {
                    String si = b.getProbeSlideInfo();
                    if (si != null) {
                        String[] entries = si.split("\n");
                        for (int i = 0; i < entries.length; i++) {
                            String[] parts = entries[i].split(";", 3);
                            if (parts.length == 3)
                                sb.append("<br>Col ").append(i + 1)
                                  .append(": [").append(parts[0]).append("]")
                                  .append(" Free: [").append(parts[1]).append("]")
                                  .append(" ").append(parts[2]);
                            else
                                sb.append("<br>Col ").append(i + 1).append(": ").append(entries[i]);
                        }
                    }
                }
                sb.append("<br>Predicted pos: [").append(probe[0]).append(", ").append(probe[1]).append("]");
            }
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }

    // -------------------------------------------------------------------------
    // Trajectory drawing
    // -------------------------------------------------------------------------
    private Color trajectoryColor(int bIdx) {
        float hue = (bIdx * 0.618033f) % 1.0f;
        return Color.getHSBColor(hue, 0.85f, 0.75f);
    }

    private void drawTrajectory(Graphics2D g2, int bIdx, int cs, float strokeWidth, float alpha) {
        List<int[]> traj = maze.getBacterium(bIdx).getTrajectory();
        if (traj.size() < 2) return;
        Color base = trajectoryColor(bIdx);
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, (int)(255 * alpha))));
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int j = 1; j < traj.size(); j++) {
            g2.drawLine(traj.get(j-1)[1]*cs + cs/2, traj.get(j-1)[0]*cs + cs/2,
                        traj.get(j)  [1]*cs + cs/2, traj.get(j)  [0]*cs + cs/2);
        }
    }

    private int findHoveredBacterium(int mx, int my) {
        int cs = maze.getDisplayPixelSize();
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            int[] pos = maze.getBacterium(i).getPosition();
            if (Math.hypot(mx - (pos[1]*cs + cs/2.0), my - (pos[0]*cs + cs/2.0)) < cs * 0.75) return i;
        }
        return -1;
    }

    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2-x1, dy = y2-y1;
        if (dx == 0 && dy == 0) return Math.hypot(px-x1, py-y1);
        double t = Math.max(0, Math.min(1, ((px-x1)*dx + (py-y1)*dy) / (dx*dx + dy*dy)));
        return Math.hypot(px-(x1+t*dx), py-(y1+t*dy));
    }

    private String buildCellHtml(int row, int col) {
        Pixel p = maze.getPixel(row, col);
        return "<html><b>[" + row + "," + col + "]</b>"
             + "<br>Wall: "  + p.isWall()
             + "<br>Count: " + p.getCount() + "</html>";
    }

    // -------------------------------------------------------------------------
    // Edit helpers
    // -------------------------------------------------------------------------
    private void applyWallAt(int row, int col) {
        if (!maze.isValid(row, col)) return;
        maze.getPixel(row, col).setWall(true);
        maze.getPixel(row, col).setExit(false);
        if (startRow == row && startCol == col) { startRow = -1; startCol = -1; }
        exitPoints.removeIf(p -> p.x == col && p.y == row);
    }

    private void applyExitAt(int row, int col) {
        if (!maze.isValid(row, col)) return;
        if (startRow == row && startCol == col) { startRow = -1; startCol = -1; }
        maze.getPixel(row, col).setWall(false);
        maze.getPixel(row, col).setExit(true);
        Point ep = new Point(col, row);
        if (exitPoints.stream().noneMatch(p -> p.x == ep.x && p.y == ep.y)) exitPoints.add(ep);
        if (exitPointListener != null) exitPointListener.accept(ep);
    }

    private void applyEraseWallAt(int row, int col) {
        if (maze.isValid(row, col)) maze.getPixel(row, col).setWall(false);
    }

    private boolean isPaintMode() {
        return currentMode == EditMode.WALL || currentMode == EditMode.EXIT || currentMode == EditMode.ERASE_WALL;
    }

    private void setEditMode(EditMode mode) { this.currentMode = mode; }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    public void enterSelectMode()         { setEditMode(EditMode.SELECT);         hideHoverWindow(); repaint(); }
    public void enterWallMode()           { setEditMode(EditMode.WALL);           hideHoverWindow(); repaint(); }
    public void enterExitMode()           { setEditMode(EditMode.EXIT);           hideHoverWindow(); repaint(); }
    public void enterEraseWallMode()      { setEditMode(EditMode.ERASE_WALL);     hideHoverWindow(); repaint(); }
    public void enterRemedyVoronoiMode()  { setEditMode(EditMode.REMEDY_VORONOI); remedySelectedJunctionId = -1; showVoronoi = true; hideHoverWindow(); repaint(); }
    public void enterDeleteJunctionMode() { setEditMode(EditMode.DELETE_JUNCTION); hideHoverWindow(); repaint(); }

    public void setAnalyzer(RAG rag) { this.rag = rag; }

    public void toggleSkeleton()     { showSkeleton     = !showSkeleton;     repaint(); }
    public void toggleVoronoi()      { showVoronoi      = !showVoronoi;      repaint(); }
    public void toggleTrajectories() { showTrajectories = !showTrajectories; repaint(); }
    public void toggleDegrees()      { showDegrees      = !showDegrees;      repaint(); }
    public void toggleGrid()         { showGrid         = !showGrid;         repaint(); }

    public boolean isShowSkeleton()     { return showSkeleton; }
    public boolean isShowVoronoi()      { return showVoronoi; }
    public boolean isShowTrajectories() { return showTrajectories; }
    public boolean isShowDegrees()      { return showDegrees; }

    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }

    public void setMaze(Maze newMaze) {
        this.maze = newMaze;
        exitPoints.clear();
        rag = null;
        bacteriumHoverWindow.setVisible(false);
        updatePreferredSize();
        revalidate();
    }

    private void updatePreferredSize() {
        int cs = maze.getDisplayPixelSize();
        setPreferredSize(new Dimension(maze.getNumCols() * cs, maze.getNumRows() * cs));
    }

    public void setStartPointListener(Consumer<Point> l) { startPointListener = l; }
    public void setExitPointListener(Consumer<Point>  l) { exitPointListener  = l; }

    public void addExitFromMain(int row, int col) { if (maze != null) { applyExitAt(row, col); repaint(); } }

    public void clearVisuals() { startRow = -1; startCol = -1; exitPoints.clear(); repaint(); }

    private void hideHoverWindow() {
        hoverInfoWindow.setVisible(false);
        popupPinned = false; isDragging = false;
        selectedCells.clear();
        selectedRow = -1; selectedCol = -1;
        lastRow     = -1; lastCol     = -1;
        hoveredRow  = -1; hoveredCol  = -1;
    }
}

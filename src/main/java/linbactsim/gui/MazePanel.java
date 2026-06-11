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
//   • SkeletonAnalyzer replaced by RAG (all analysis overlay code is no-op until RAG is implemented)
public class MazePanel extends JPanel {

    private Maze maze;
    private boolean showGrid, showSkeleton, showVoronoi, showTrajectories, showDegrees;
    private RAG rag;

    public enum EditMode { SELECT, WALL, EXIT, ERASE_WALL, REMEDY_VORONOI, DELETE_JUNCTION }
    private EditMode currentMode = EditMode.SELECT;

    private int  remedySelectedJunctionId = -1;
    private boolean remedyDragging        = false;
    private int  hoveredBacteriumIdx      = -1;

    private JWindow hoverInfoWindow;
    private JLabel  hoverLabel;
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

        setPreferredSize(new Dimension(1000, 700));
        setToolTipText(" ");
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        ToolTipManager.sharedInstance().setInitialDelay(200);

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
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int cs  = maze.getDisplayPixelSize();
                int col = e.getX() / cs, row = e.getY() / cs;
                if (!maze.isValid(row, col)) return;
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
                int cs  = maze.getDisplayPixelSize();
                int col = e.getX() / cs, row = e.getY() / cs;
                if (currentMode != EditMode.SELECT || !SwingUtilities.isLeftMouseButton(e)) return;
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
                if (hoveredBacteriumIdx != -1) { hoveredBacteriumIdx = -1; repaint(); }
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
    // Tooltip
    // -------------------------------------------------------------------------
    @Override
    public String getToolTipText(MouseEvent e) {
        int cs  = maze.getDisplayPixelSize();
        int col = e.getX() / cs, row = e.getY() / cs;
        if (!maze.isValid(row, col) || maze.isWall(row, col)) return null;
        int bIdx = findHoveredBacterium(e.getX(), e.getY());
        if (bIdx >= 0) {
            Bacterium b = maze.getBacterium(bIdx);
            return "<html>Bacterium " + (bIdx + 1)
                + " | " + b.getSpecies().getName()
                + " | Steps: " + b.getTime()
                + " | Exited: " + b.hasExited() + "</html>";
        }
        return null;
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

        // 2. Start cell highlight
        if (startRow >= 0 && startCol >= 0) {
            g.setColor(new Color(255, 255, 0, 128));
            g.fillRect(startCol * cs, startRow * cs, cs, cs);
            g.setColor(Color.RED);
            g.drawRect(startCol * cs, startRow * cs, cs, cs);
        }

        // 3. Exit cells
        for (Point ep : exitPoints) {
            g.setColor(new Color(0, 255, 0, 120));
            g.fillRect(ep.x * cs, ep.y * cs, cs, cs);
            g.setColor(new Color(0, 128, 0));
            g.drawRect(ep.x * cs, ep.y * cs, cs, cs);
        }

        // 4. Selected cell(s)
        if (selectedRow >= 0 && selectedCol >= 0) {
            g2.setColor(Color.CYAN);
            g2.setStroke(new BasicStroke(2f));
            if (!selectedCells.isEmpty())
                for (Point cell : selectedCells)
                    g2.drawRect(cell.x * cs, cell.y * cs, cs, cs);
            else
                g2.drawRect(selectedCol * cs, selectedRow * cs, cs, cs);
        }

        // 5. Trajectory lines
        g2.setStroke(new BasicStroke(1f));
        if (showTrajectories) {
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                if (i == hoveredBacteriumIdx) continue;
                drawTrajectory(g2, i, cs, 1.5f, 0.75f);
            }
        }
        if (hoveredBacteriumIdx >= 0 && hoveredBacteriumIdx < maze.getBacteriaCount())
            drawTrajectory(g2, hoveredBacteriumIdx, cs, 3.5f, 1.0f);

        // 6. Bacteria ovals
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
            g2.drawLine(traj.get(j-1)[1] * cs + cs/2, traj.get(j-1)[0] * cs + cs/2,
                        traj.get(j)[1]   * cs + cs/2, traj.get(j)[0]   * cs + cs/2);
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

    private int findHoveredTrajectory(int mx, int my) {
        int cs = maze.getDisplayPixelSize();
        final double THRESH = Math.max(3, cs * 0.4);
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            List<int[]> traj = maze.getBacterium(i).getTrajectory();
            for (int j = 1; j < traj.size(); j++) {
                double x1 = traj.get(j-1)[1]*cs + cs/2.0, y1 = traj.get(j-1)[0]*cs + cs/2.0;
                double x2 = traj.get(j)[1]  *cs + cs/2.0, y2 = traj.get(j)[0]  *cs + cs/2.0;
                if (distToSegment(mx, my, x1, y1, x2, y2) < THRESH) return i;
            }
        }
        return -1;
    }

    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2-x1, dy = y2-y1;
        if (dx == 0 && dy == 0) return Math.hypot(px-x1, py-y1);
        double t = Math.max(0, Math.min(1, ((px-x1)*dx + (py-y1)*dy) / (dx*dx + dy*dy)));
        return Math.hypot(px-(x1+t*dx), py-(y1+t*dy));
    }

    private String buildTrajectoryTooltip(int bIdx, List<Integer> rt) {
        return "Bacterium " + (bIdx + 1);
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
    public void enterRemedyVoronoiMode()  { setEditMode(EditMode.REMEDY_VORONOI); remedySelectedJunctionId = -1; hideHoverWindow(); repaint(); }
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

    public void setMaze(Maze newMaze) { this.maze = newMaze; exitPoints.clear(); rag = null; }

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

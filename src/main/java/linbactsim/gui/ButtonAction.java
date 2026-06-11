package linbactsim.gui;

import linbactsim.analysis.RAG;
import linbactsim.io.MazeIO;
import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.resources.BacteriumSpecies;
import linbactsim.resources.MazeMaps;
import linbactsim.resources.UserGuide;
import linbactsim.simulation.ForceModel;
import linbactsim.simulation.SimulationParameters;
import linbactsim.simulation.SimulationRunner;
import linbactsim.simulation.WeibullModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

// Builds the application window and wires all button actions.
// Source: SURE.Main#main() — all JFrame setup, button declarations, and
// action listeners extracted here (~lines 85–1331).
// Analysis buttons (skeleton/voronoi/RAG) are present but disabled until
// the analysis layer is implemented.
public class ButtonAction {

    // ---- Live simulation state -----------------------------------------------
    private final Maze[] mazeRef = { null };
    private int[][]      savedMazeGrid;
    private MazePanel    panel;
    private SimulationRunner runner;
    private RAG[]        ragRef = { null };
    private JFrame       frame;

    // ---- Input fields (created here so listeners can share them) -------------
    private final JTextField velocityField = new JTextField("8",   6);
    private final JTextField stdDevField   = new JTextField("3",   6);
    private final JTextField noiseField    = new JTextField("0.5", 6);
    private final JTextField dtField       = new JTextField("1",   6);
    private final JTextField durationField = new JTextField("1000",6);
    private final JTextField wMemoryField  = new JTextField("0.70",6);
    private final JTextField wNoiseField   = new JTextField("0.10",6);
    private final JTextField wWallField    = new JTextField("0.20",6);
    private final JTextField rowField      = new JTextField("-1",  5);
    private final JTextField colField      = new JTextField("-1",  5);
    private final JTextField countField    = new JTextField("1",   5);
    private final JTextField lengthField   = new JTextField("1",   5);
    private final JTextField widthField    = new JTextField("1",   5);

    @SuppressWarnings("unchecked")
    private final JComboBox<BacteriumSpecies> speciesComboBox =
            new JComboBox<>(BacteriumSpecies.DEFAULT_SPECIES);
    private final JComboBox<String> modelComboBox =
            new JComboBox<>(new String[]{"Weibull", "Force"});

    // -------------------------------------------------------------------------
    // Entry point — called from Main.main()
    // -------------------------------------------------------------------------
    public static void launch() { new ButtonAction().run(); }

    private void run() {
        // Default maze: plaza
        Maze initial = new Maze(100, 100, 7);
        MazeIO.applyWallList(MazeMaps.buildPlazaWalls(), initial);
        savedMazeGrid = captureMazeGrid(initial);
        mazeRef[0] = initial;

        panel  = new MazePanel(initial);
        runner = new SimulationRunner(new WeibullModel());

        // Pre-populate velocity/stdDev from the initially selected species
        BacteriumSpecies s0 = (BacteriumSpecies) speciesComboBox.getSelectedItem();
        if (s0 != null) {
            velocityField.setText(String.valueOf(s0.getVelocity()));
            stdDevField.setText(String.valueOf(s0.getVelocityStdDev()));
        }

        // Auto-populate velocity/stdDev whenever the species selection changes
        speciesComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                BacteriumSpecies s = (BacteriumSpecies) e.getItem();
                velocityField.setText(String.valueOf(s.getVelocity()));
                stdDevField.setText(String.valueOf(s.getVelocityStdDev()));
            }
        });

        // Switch movement model at runtime
        modelComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                runner.setMovementModel("Force".equals(e.getItem()) ? new ForceModel() : new WeibullModel());
        });

        frame = buildMainFrame();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Window construction
    // -------------------------------------------------------------------------
    private JFrame buildMainFrame() {
        JFrame f = new JFrame("LinBactSim");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout(4, 4));
        f.add(buildToolbar(),    BorderLayout.NORTH);
        f.add(new JScrollPane(panel), BorderLayout.CENTER);
        f.add(buildInputPanel(), BorderLayout.EAST);
        return f;
    }

    private JPanel buildToolbar() {
        // Maze loading row
        JPanel mazeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        mazeBar.setBorder(BorderFactory.createTitledBorder("Maze"));

        JButton plazaBtn      = new JButton("Plaza");
        JButton uniformBtn    = new JButton("Uniform");
        JButton nonUniformBtn = new JButton("Non-Uniform");
        JButton customBtn     = new JButton("Load CSV…");
        JButton saveBtn       = new JButton("Save CSV…");

        plazaBtn.addActionListener(e -> loadPlaza());
        uniformBtn.addActionListener(e -> loadUniform());
        nonUniformBtn.addActionListener(e -> loadNonUniform());
        customBtn.addActionListener(e -> loadCustomCSV());
        saveBtn.addActionListener(e -> onSaveMaze());

        for (JButton b : new JButton[]{plazaBtn, uniformBtn, nonUniformBtn, customBtn, saveBtn})
            mazeBar.add(b);

        // Edit mode row
        JPanel editBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        editBar.setBorder(BorderFactory.createTitledBorder("Edit"));

        JToggleButton selectBtn = new JToggleButton("Select",    true);
        JToggleButton wallBtn   = new JToggleButton("Draw Wall");
        JToggleButton exitBtn   = new JToggleButton("Draw Exit");
        JToggleButton eraseBtn  = new JToggleButton("Erase");

        ButtonGroup editGroup = new ButtonGroup();
        for (JToggleButton tb : new JToggleButton[]{selectBtn, wallBtn, exitBtn, eraseBtn}) {
            editGroup.add(tb);
            editBar.add(tb);
        }
        selectBtn.addActionListener(e -> panel.enterSelectMode());
        wallBtn.addActionListener(e -> panel.enterWallMode());
        exitBtn.addActionListener(e -> panel.enterExitMode());
        eraseBtn.addActionListener(e -> panel.enterEraseWallMode());

        // View toggles
        JPanel viewBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        viewBar.setBorder(BorderFactory.createTitledBorder("View"));

        JButton gridBtn = new JButton("Grid");
        JButton trajBtn = new JButton("Trajectories");
        gridBtn.addActionListener(e -> panel.toggleGrid());
        trajBtn.addActionListener(e -> panel.toggleTrajectories());
        viewBar.add(gridBtn);
        viewBar.add(trajBtn);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        toolbar.add(mazeBar);
        toolbar.add(editBar);
        toolbar.add(viewBar);
        return toolbar;
    }

    private JPanel buildInputPanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setPreferredSize(new Dimension(255, 700));
        outer.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // --- Species & Model --------------------------------------------------
        JPanel sp = titled("Species & Model");
        sp.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        addRow(sp, g, 0, "Species:", speciesComboBox);
        addRow(sp, g, 1, "Model:",   modelComboBox);
        addRow(sp, g, 2, "Velocity:",velocityField);
        addRow(sp, g, 3, "Std Dev:", stdDevField);
        outer.add(sp);

        // --- Simulation parameters --------------------------------------------
        JPanel pp = titled("Parameters");
        pp.setLayout(new GridBagLayout());
        GridBagConstraints g2 = gbc();
        addRow(pp, g2, 0, "Noise:",    noiseField);
        addRow(pp, g2, 1, "dt:",       dtField);
        addRow(pp, g2, 2, "Duration:", durationField);
        addRow(pp, g2, 3, "w Memory:", wMemoryField);
        addRow(pp, g2, 4, "w Noise:",  wNoiseField);
        addRow(pp, g2, 5, "w Wall:",   wWallField);
        outer.add(pp);

        // --- Bacteria placement -----------------------------------------------
        JPanel pl = titled("Place Bacteria");
        pl.setLayout(new GridBagLayout());
        GridBagConstraints g3 = gbc();
        addRow(pl, g3, 0, "Row (-1=rand):", rowField);
        addRow(pl, g3, 1, "Col (-1=rand):", colField);
        addRow(pl, g3, 2, "Count:",         countField);
        addRow(pl, g3, 3, "Length:",        lengthField);
        addRow(pl, g3, 4, "Width:",         widthField);
        JButton createBtn = new JButton("Create Bacteria");
        createBtn.addActionListener(e -> onCreateBacteria());
        g3.gridx=0; g3.gridy=5; g3.gridwidth=2; g3.weightx=1;
        pl.add(createBtn, g3);
        outer.add(pl);

        // --- Run controls -----------------------------------------------------
        JPanel rp = titled("Run");
        rp.setLayout(new GridLayout(0, 1, 2, 2));

        JButton fastBtn      = new JButton("Run Fast");
        JButton animBtn      = new JButton("Run Animated");
        JButton stopBtn      = new JButton("Stop");
        JButton resetSimBtn  = new JButton("Reset Simulation");
        JButton resetMazeBtn = new JButton("Reset Maze");
        JButton listBtn      = new JButton("Show Bacteria List");
        JButton helpBtn      = new JButton("?  Help");

        fastBtn.addActionListener(e -> onRunFast());
        animBtn.addActionListener(e -> onRunAnimated());
        stopBtn.addActionListener(e -> runner.stop());
        resetSimBtn.addActionListener(e -> onResetSimulation());
        resetMazeBtn.addActionListener(e -> onResetMaze());
        listBtn.addActionListener(e -> onShowBacteriaList());
        helpBtn.addActionListener(e -> onShowHelp());

        for (JButton b : new JButton[]{fastBtn, animBtn, stopBtn, resetSimBtn, resetMazeBtn, listBtn, helpBtn})
            rp.add(b);

        outer.add(rp);
        outer.add(Box.createVerticalGlue());
        return outer;
    }

    // -------------------------------------------------------------------------
    // Maze loading
    // -------------------------------------------------------------------------
    private void loadPlaza() {
        Maze m = new Maze(100, 100, 7);
        MazeIO.applyWallList(MazeMaps.buildPlazaWalls(), m);
        swapMaze(m);
    }

    private void loadUniform() {
        int[][] grid = MazeIO.loadFromResource(MazeMaps.UNIFORM_PATH);
        if (grid == null) { error("Could not load uniform maze resource."); return; }
        Maze m = new Maze(grid.length, grid[0].length, 7);
        MazeIO.applyGrid(grid, m);
        swapMaze(m);
    }

    private void loadNonUniform() {
        int[][] grid = MazeIO.loadFromResource(MazeMaps.NON_UNIFORM_PATH);
        if (grid == null) { error("Could not load non-uniform maze resource."); return; }
        Maze m = new Maze(grid.length, grid[0].length, 7);
        MazeIO.applyGrid(grid, m);
        swapMaze(m);
    }

    private void loadCustomCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Maze CSV");
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            int[][] grid = MazeIO.importFromCSV(fc.getSelectedFile());
            Maze m = new Maze(grid.length, grid[0].length, 7);
            MazeIO.applyGrid(grid, m);
            swapMaze(m);
        } catch (Exception ex) {
            error("Error loading CSV: " + ex.getMessage());
        }
    }

    private void onSaveMaze() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Maze as CSV");
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            MazeIO.saveToCSV(mazeRef[0], fc.getSelectedFile());
        } catch (Exception ex) {
            error("Error saving: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Simulation actions
    // -------------------------------------------------------------------------
    private void onCreateBacteria() {
        SimulationParameters params = collectParams();
        BacteriumSpecies species = (BacteriumSpecies) speciesComboBox.getSelectedItem();
        Maze maze = mazeRef[0];

        int count  = SimulationParameters.parseIntOrDefault(countField.getText(),  1);
        int len    = SimulationParameters.parseIntOrDefault(lengthField.getText(), 1);
        int wid    = SimulationParameters.parseIntOrDefault(widthField.getText(),  1);
        int reqRow = SimulationParameters.parseIntOrDefault(rowField.getText(),   -1);
        int reqCol = SimulationParameters.parseIntOrDefault(colField.getText(),   -1);

        boolean useFixed = reqRow >= 0 && reqCol >= 0
                && maze.isValid(reqRow, reqCol) && !maze.isWall(reqRow, reqCol);

        for (int i = 0; i < count; i++) {
            Bacterium b = useFixed
                    ? new Bacterium(len, wid, reqRow, reqCol, params.getAngleNoise(), species)
                    : new Bacterium(len, wid, maze, params.getAngleNoise(), species);
            b.setDirectionWeights(params.getWMemory(), params.getWNoise(), params.getWWall());
            maze.addBacterium(b);
        }
        panel.repaint();
    }

    private void onRunFast() {
        runner.runFast(mazeRef[0], collectParams());
        panel.repaint();
    }

    private void onRunAnimated() {
        runner.runAnimated(mazeRef[0], collectParams(), panel);
    }

    // Clears all bacteria; resets velocity/stdDev fields to current species defaults.
    private void onResetSimulation() {
        runner.stop();
        mazeRef[0].clearBacteria();
        mazeRef[0].clearDensity();
        BacteriumSpecies s = (BacteriumSpecies) speciesComboBox.getSelectedItem();
        if (s != null) {
            velocityField.setText(String.valueOf(s.getVelocity()));
            stdDevField.setText(String.valueOf(s.getVelocityStdDev()));
        }
        panel.repaint();
    }

    // Reverts maze to its last-loaded state; does NOT touch bacteria or velocity fields.
    private void onResetMaze() {
        if (savedMazeGrid == null) return;
        runner.stop();
        Maze m = new Maze(savedMazeGrid.length, savedMazeGrid[0].length,
                          mazeRef[0].getDisplayPixelSize());
        MazeIO.applyGrid(savedMazeGrid, m);
        mazeRef[0] = m;
        panel.setMaze(m);
        ragRef[0] = null;
        panel.repaint();
    }

    private void onShowBacteriaList() {
        Maze maze = mazeRef[0];
        String[] cols = {"#", "Species", "Time", "Steps", "Exited", "Trajectory"};
        Object[][] data = new Object[maze.getBacteriaCount()][6];
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            data[i][0] = i + 1;
            data[i][1] = b.getSpecies().getName();
            data[i][2] = b.getTime();
            data[i][3] = b.getTrajectoryLength();
            data[i][4] = b.hasExited();
            data[i][5] = "Show Trajectory";
        }
        JTable table = new JTable(data, cols);
        table.getColumn("Trajectory").setCellRenderer(new ButtonRenderer());
        table.getColumn("Trajectory").setCellEditor(new ButtonEditor(maze));
        table.setRowHeight(24);

        JFrame listFrame = new JFrame("Bacteria List");
        listFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        listFrame.add(new JScrollPane(table));
        listFrame.setSize(560, 300);
        listFrame.setLocationRelativeTo(frame);
        listFrame.setVisible(true);
    }

    private void onShowHelp() {
        String text = UserGuide.TEXT.isEmpty()
                ? "LinBactSim — Bacterial simulation platform.\n\nSee README for usage details."
                : UserGuide.TEXT;
        JOptionPane.showMessageDialog(frame, text, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // Swap the active maze, save its grid for potential maze-reset, update the panel.
    private void swapMaze(Maze m) {
        runner.stop();
        savedMazeGrid = captureMazeGrid(m);
        mazeRef[0] = m;
        panel.setMaze(m);
        ragRef[0] = null;
        panel.repaint();
    }

    private SimulationParameters collectParams() {
        SimulationParameters p = new SimulationParameters();
        p.setDt(SimulationParameters.parseIntOrDefault(dtField.getText(), 1));
        p.setDuration(SimulationParameters.parseIntOrDefault(durationField.getText(), 1000));
        p.setVelocity(SimulationParameters.parseOrDefault(velocityField.getText(), 8));
        p.setStdDev(SimulationParameters.parseOrDefault(stdDevField.getText(), 3));
        p.setAngleNoise(SimulationParameters.parseOrDefault(noiseField.getText(), 0.5));
        p.setWMemory(SimulationParameters.parseOrDefault(wMemoryField.getText(), 0.70));
        p.setWNoise(SimulationParameters.parseOrDefault(wNoiseField.getText(), 0.10));
        p.setWWall(SimulationParameters.parseOrDefault(wWallField.getText(), 0.20));
        return p;
    }

    private int[][] captureMazeGrid(Maze maze) {
        int rows = maze.getNumRows(), cols = maze.getNumCols();
        int[][] grid = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = maze.isWall(r, c) ? 1 : 0;
        return grid;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel titled(String title) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill   = GridBagConstraints.HORIZONTAL;
        return g;
    }

    private static void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridwidth = 1;
        g.gridx=0; g.gridy=row; g.weightx=0; p.add(new JLabel(label), g);
        g.gridx=1;              g.weightx=1; p.add(field, g);
    }
}

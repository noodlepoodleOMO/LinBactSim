package linbactsim.gui;

import linbactsim.analysis.RAG;
import linbactsim.analysis.Skeleton;
import linbactsim.analysis.Voronoi;
import linbactsim.io.Analysis;
import linbactsim.io.MazeIO;
import linbactsim.io.VoronoiMap;
import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import linbactsim.resources.BacteriumSpecies;
import linbactsim.resources.MazeMaps;
import linbactsim.resources.UserGuide;
import linbactsim.simulation.ForceModel;
import linbactsim.simulation.ForceModel4Ray;
import linbactsim.simulation.ForceModel4RayCutoff;
import linbactsim.simulation.MovementModel;
import linbactsim.simulation.WeibullForceModel;
import linbactsim.simulation.SimulationParameters;
import linbactsim.simulation.SimulationRunner;
import linbactsim.simulation.WeibullModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;

// Builds the application window and wires all button actions.
// Source: SURE.Main#main() — all JFrame setup, button declarations, and
// action listeners extracted here (~lines 85–1331).
public class ButtonAction {

    // ---- Live simulation state -----------------------------------------------
    private final Maze[] mazeRef = { null };
    private int[][]      savedMazeGrid;
    private MazePanel    panel;
    private SimulationRunner runner;
    private RAG[]        ragRef = { null };
    private JFrame       frame;

    // ---- File-chooser memory -------------------------------------------------
    private File lastCsvDir     = null;
    private File lastVoronoiDir = null;

    // ---- Voronoi import state ------------------------------------------------
    private int[][] importedVoronoiGrid = null;

    // ---- Input fields --------------------------------------------------------
    private final JTextField velocityField = new JTextField("8",   6);
    private final JTextField stdDevField   = new JTextField("3",   6);
    private final JTextField noiseField    = new JTextField("0.6", 6);
    private final JTextField dtField       = new JTextField("1",   6);
    private final JTextField durationField = new JTextField("1000",6);
    private final JTextField wMemoryField  = new JTextField("0.50",6);
    private final JTextField wNoiseField   = new JTextField("0.20",6);
    private final JTextField wWallField    = new JTextField("0.30",6);
    private final JTextField rowField      = new JTextField("-1",  5);
    private final JTextField colField      = new JTextField("-1",  5);
    private final JTextField countField    = new JTextField("1",   5);
    private final JTextField lengthField   = new JTextField("1",   5);
    private final JTextField widthField    = new JTextField("1",   5);

    @SuppressWarnings("unchecked")
    private final JComboBox<BacteriumSpecies> speciesComboBox =
            new JComboBox<>(BacteriumSpecies.DEFAULT_SPECIES);
    private final JComboBox<String> modelComboBox =
            new JComboBox<>(new String[]{"Weibull", "Force 4", "Force 360", "Force 4 (50µm)", "Weibull Force"});

    // -------------------------------------------------------------------------
    // Entry point — called from Main.main()
    // -------------------------------------------------------------------------
    public static void launch() { new ButtonAction().run(); }

    private void run() {
        Maze initial = new Maze(100, 100, 7);
        MazeIO.applyWallList(MazeMaps.buildPlazaWalls(), initial);
        savedMazeGrid = captureMazeGrid(initial);
        mazeRef[0]    = initial;

        panel  = new MazePanel(initial);
        runner = new SimulationRunner(new WeibullModel());

        // Set probe callback — fires after every fast or animated run
        runner.setOnComplete(() -> {
            int dt = runner.getLastDt();
            MovementModel model = runner.getMovementModel();
            Maze maze = mazeRef[0];
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                Bacterium b = maze.getBacterium(i);
                if (!b.hasExited()) model.probeFullStep(b, maze, dt);
            }
            panel.repaint();
        });

        BacteriumSpecies s0 = (BacteriumSpecies) speciesComboBox.getSelectedItem();
        if (s0 != null) {
            velocityField.setText(String.valueOf(s0.getVelocity()));
            stdDevField.setText(String.valueOf(s0.getVelocityStdDev()));
        }
        speciesComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                BacteriumSpecies s = (BacteriumSpecies) e.getItem();
                velocityField.setText(String.valueOf(s.getVelocity()));
                stdDevField.setText(String.valueOf(s.getVelocityStdDev()));
            }
        });
        modelComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String sel = (String) e.getItem();
                if      ("Force 360".equals(sel))    runner.setMovementModel(new ForceModel());
                else if ("Force 4".equals(sel))      runner.setMovementModel(new ForceModel4Ray());
                else if ("Force 4 (50µm)".equals(sel))  runner.setMovementModel(new ForceModel4RayCutoff());
                else if ("Weibull Force".equals(sel))   runner.setMovementModel(new WeibullForceModel());
                else                                    runner.setMovementModel(new WeibullModel());
            }
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
        f.add(buildToolbar(),         BorderLayout.NORTH);
        f.add(new JScrollPane(panel), BorderLayout.CENTER);
        f.add(buildInputPanel(),      BorderLayout.EAST);
        return f;
    }

    private JPanel buildToolbar() {
        // ---- Maze loading bar ------------------------------------------------
        JPanel mazeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        mazeBar.setBorder(BorderFactory.createTitledBorder("Maze"));

        JButton plazaBtn      = new JButton("Plaza");
        JButton uniformBtn    = new JButton("Uniform");
        JButton nonUniformBtn = new JButton("Non-Uniform");
        JButton customBtn     = new JButton("Load CSV…");
        JButton saveBtn       = new JButton("Save CSV…");

        plazaBtn.addActionListener(e      -> loadPlaza());
        uniformBtn.addActionListener(e    -> loadUniform());
        nonUniformBtn.addActionListener(e -> loadNonUniform());
        customBtn.addActionListener(e     -> loadCustomCSV());
        saveBtn.addActionListener(e       -> onSaveMaze());
        for (JButton b : new JButton[]{plazaBtn, uniformBtn, nonUniformBtn, customBtn, saveBtn})
            mazeBar.add(b);

        // ---- Edit mode bar ---------------------------------------------------
        JPanel editBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        editBar.setBorder(BorderFactory.createTitledBorder("Edit"));

        JToggleButton selectBtn  = new JToggleButton("Select", true);
        JToggleButton wallBtn    = new JToggleButton("Draw Wall");
        JToggleButton exitBtn    = new JToggleButton("Draw Exit");
        JToggleButton eraseBtn   = new JToggleButton("Erase");

        // remedyBtn and deleteJBtn live in the Analysis bar but share this group
        JToggleButton remedyBtn  = new JToggleButton("Remedy Voronoi");
        JToggleButton deleteJBtn = new JToggleButton("Delete Junction");

        ButtonGroup editGroup = new ButtonGroup();
        for (JToggleButton tb : new JToggleButton[]{selectBtn, wallBtn, exitBtn, eraseBtn, remedyBtn, deleteJBtn})
            editGroup.add(tb);
        for (JToggleButton tb : new JToggleButton[]{selectBtn, wallBtn, exitBtn, eraseBtn})
            editBar.add(tb);

        selectBtn .addActionListener(e -> panel.enterSelectMode());
        wallBtn   .addActionListener(e -> panel.enterWallMode());
        exitBtn   .addActionListener(e -> panel.enterExitMode());
        eraseBtn  .addActionListener(e -> panel.enterEraseWallMode());
        remedyBtn .addActionListener(e -> panel.enterRemedyVoronoiMode());
        deleteJBtn.addActionListener(e -> panel.enterDeleteJunctionMode());

        // ---- View bar --------------------------------------------------------
        JPanel viewBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        viewBar.setBorder(BorderFactory.createTitledBorder("View"));

        JButton gridBtn    = new JButton("Grid");
        JButton trajBtn    = new JButton("Trajectories");
        JButton densityBtn = new JButton("Density Map");

        gridBtn.addActionListener(e    -> panel.toggleGrid());
        trajBtn.addActionListener(e    -> panel.toggleTrajectories());
        densityBtn.addActionListener(e -> onShowDensityMap());
        viewBar.add(gridBtn);
        viewBar.add(trajBtn);
        viewBar.add(densityBtn);

        // ---- Analysis bar ----------------------------------------------------
        JPanel analysisBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        analysisBar.setBorder(BorderFactory.createTitledBorder("Analysis"));

        JToggleButton skelBtn    = new JToggleButton("Skeleton");
        JToggleButton vorBtn     = new JToggleButton("Voronoi");
        JToggleButton degBtn     = new JToggleButton("Degrees");
        JButton       ragBtn     = new JButton("Show RAG");
        JButton       saveVorBtn = new JButton("Save Voronoi…");
        JButton       loadVorBtn = new JButton("Load Voronoi…");
        JButton       resetVorBtn= new JButton("Reset Voronoi");

        skelBtn.addActionListener(e -> { ensureAnalyzed(); panel.toggleSkeleton(); });
        vorBtn .addActionListener(e -> { ensureAnalyzed(); panel.toggleVoronoi(); });
        degBtn .addActionListener(e -> { ensureAnalyzed(); panel.toggleDegrees(); });
        ragBtn    .addActionListener(e -> onShowRAG());
        saveVorBtn.addActionListener(e -> onSaveVoronoi());
        loadVorBtn.addActionListener(e -> onLoadVoronoi());
        resetVorBtn.addActionListener(e -> onResetVoronoi());

        for (JComponent c : new JComponent[]{skelBtn, vorBtn, degBtn, ragBtn, saveVorBtn, loadVorBtn, resetVorBtn, remedyBtn, deleteJBtn})
            analysisBar.add(c);

        // ---- Assemble rows ---------------------------------------------------
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row1.add(mazeBar);
        row1.add(editBar);
        row1.add(viewBar);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row2.add(analysisBar);

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.add(row1);
        toolbar.add(row2);
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

        fastBtn.addActionListener(e     -> onRunFast());
        animBtn.addActionListener(e     -> onRunAnimated());
        stopBtn.addActionListener(e     -> runner.stop());
        resetSimBtn.addActionListener(e -> onResetSimulation());
        resetMazeBtn.addActionListener(e-> onResetMaze());
        listBtn.addActionListener(e     -> onShowBacteriaList());
        helpBtn.addActionListener(e     -> onShowHelp());

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
        if (lastCsvDir != null) fc.setCurrentDirectory(lastCsvDir);
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        lastCsvDir = fc.getSelectedFile().getParentFile();
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
        if (lastCsvDir != null) fc.setCurrentDirectory(lastCsvDir);
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        lastCsvDir = fc.getSelectedFile().getParentFile();
        try {
            MazeIO.saveToCSV(mazeRef[0], fc.getSelectedFile());
        } catch (Exception ex) {
            error("Error saving: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Analysis actions
    // -------------------------------------------------------------------------
    private RAG ensureAnalyzed() {
        if (ragRef[0] == null || !ragRef[0].isReady()) {
            RAG rag = new RAG(new Voronoi(new Skeleton()));
            rag.build(mazeRef[0]);
            ragRef[0] = rag;
            panel.setAnalyzer(rag);
        }
        return ragRef[0];
    }

    private void onLoadVoronoi() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Voronoi CSV");
        if (lastVoronoiDir != null) fc.setCurrentDirectory(lastVoronoiDir);
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        lastVoronoiDir = fc.getSelectedFile().getParentFile();
        try {
            int[][] grid = VoronoiMap.load(fc.getSelectedFile());
            importedVoronoiGrid = VoronoiMap.deepCopy(grid);
            RAG rag = new RAG(new Voronoi(new Skeleton()));
            rag.loadFromVoronoi(grid, mazeRef[0]);
            ragRef[0] = rag;
            panel.setAnalyzer(rag);
            panel.repaint();
        } catch (Exception ex) {
            error("Error loading voronoi: " + ex.getMessage());
        }
    }

    private void onSaveVoronoi() {
        if (ragRef[0] == null || !ragRef[0].isReady()) {
            error("No voronoi data to save. Run analysis or load a voronoi file first."); return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Voronoi as CSV");
        if (lastVoronoiDir != null) fc.setCurrentDirectory(lastVoronoiDir);
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        lastVoronoiDir = fc.getSelectedFile().getParentFile();
        try {
            VoronoiMap.save(ragRef[0].getRegionMap(), fc.getSelectedFile());
        } catch (Exception ex) {
            error("Error saving voronoi: " + ex.getMessage());
        }
    }

    private void onResetVoronoi() {
        if (ragRef[0] == null) return;
        ragRef[0].clearVoronoi();
        panel.repaint();
    }

    private void onShowRAG() {
        RAG rag = ensureAnalyzed();
        JFrame ragFrame = new JFrame("RAG Graph");
        ragFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ragFrame.add(new JScrollPane(new RAGPanel(mazeRef[0], rag)));
        ragFrame.pack();
        ragFrame.setLocationRelativeTo(frame);
        ragFrame.setVisible(true);
    }

    private void onShowDensityMap() {
        JFrame dmFrame = new JFrame("Density Map");
        dmFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dmFrame.add(new JScrollPane(new DensityMapPanel(mazeRef[0])));
        dmFrame.pack();
        dmFrame.setLocationRelativeTo(frame);
        dmFrame.setVisible(true);
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

        String[] cols = {"Index", "Velocity", "StdDev", "k", "Lambda", "Multiplier", "Baseline", "Noise", "Step Number", "Trajectory Length", "Trajectory"};
        Object[][] data = new Object[maze.getBacteriaCount()][11];
        java.util.Map<String, java.util.List<Integer>> trajectoryLengthsBySpecies = new java.util.LinkedHashMap<>();
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            data[i][0]  = i + 1;
            data[i][1]  = b.getVelocity();
            data[i][2]  = b.getStdDev();
            data[i][3]  = b.getK();
            data[i][4]  = b.getLambda();
            data[i][5]  = b.getMultiplier();
            data[i][6]  = b.getBaseline();
            data[i][7]  = b.getNoise();
            data[i][8]  = b.getTime();
            data[i][9]  = b.getTrajectory().size();
            data[i][10] = "Show Trajectory";
            trajectoryLengthsBySpecies
                    .computeIfAbsent(b.getSpecies().getName(), k -> new java.util.ArrayList<>())
                    .add(b.getTrajectory().size());
        }

        JTable table = new JTable(data, cols) {
            public boolean isCellEditable(int row, int column) { return column == 10; }
        };
        table.getColumn("Trajectory").setCellRenderer(new ButtonRenderer());
        table.getColumn("Trajectory").setCellEditor(new ButtonEditor(maze));

        JFrame listFrame = new JFrame("Bacteria Properties");
        listFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton downloadButton = new JButton("Download Trajectories");
        downloadButton.addActionListener(e -> {
            JFileChooser fc = chooser("Save Excel File");
            if (fc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            try {
                Analysis.exportTrajectories(maze, fc.getSelectedFile());
                JOptionPane.showMessageDialog(listFrame, "Trajectories exported successfully!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(listFrame, "Error saving Excel file."); }
        });

        JButton downloadSummaryButton = new JButton("Download Step Summary");
        downloadSummaryButton.addActionListener(e -> {
            JFileChooser fc = chooser("Save Step Summary File");
            if (fc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            try {
                Analysis.exportStepSummary(maze, fc.getSelectedFile());
                JOptionPane.showMessageDialog(listFrame, "Step summary exported successfully!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(listFrame, "Error saving summary file."); }
        });

        JButton averageTrajectoryButton = new JButton("Average Trajectory");
        averageTrajectoryButton.addActionListener(e -> {
            JFrame averageFrame = new JFrame("Average Trajectory by Species");
            averageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            String[] avgCols = {"Species", "Average Trajectory"};
            Object[][] avgData = new Object[trajectoryLengthsBySpecies.size()][2];
            int rowIndex = 0;
            for (java.util.Map.Entry<String, java.util.List<Integer>> entry : trajectoryLengthsBySpecies.entrySet()) {
                java.util.List<Integer> lengths = entry.getValue();
                int sum = 0; for (int len : lengths) sum += len;
                avgData[rowIndex][0] = entry.getKey();
                avgData[rowIndex][1] = lengths.isEmpty() ? 0.0 : (double) sum / lengths.size();
                rowIndex++;
            }
            JTable avgTable = new JTable(avgData, avgCols);
            averageFrame.setLayout(new BorderLayout());
            averageFrame.add(new JScrollPane(avgTable), BorderLayout.CENTER);
            averageFrame.setSize(500, 300);
            averageFrame.setLocationRelativeTo(listFrame);
            averageFrame.setVisible(true);
        });

        JButton exportVisitMatrixButton = new JButton("Export Visit Matrix");
        exportVisitMatrixButton.addActionListener(ev -> {
            if (ragRef[0] == null || !ragRef[0].isReady()) {
                JOptionPane.showMessageDialog(listFrame, "Run graph analysis first (Show Skeleton or Load Voronoi).");
                return;
            }
            JFileChooser fc = chooser("Save Visit Matrix CSV");
            if (fc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            try {
                Analysis.exportVisitMatrix(maze, ragRef[0], fc.getSelectedFile());
                JOptionPane.showMessageDialog(listFrame, "Visit matrix exported to " + fc.getSelectedFile().getName());
            } catch (Exception ex) { JOptionPane.showMessageDialog(listFrame, "Error: " + ex.getMessage()); }
        });

        JButton exportSuccessfulVisitMatrixButton = new JButton("Export Successful Visit Matrix");
        exportSuccessfulVisitMatrixButton.addActionListener(ev -> {
            if (ragRef[0] == null || !ragRef[0].isReady()) {
                JOptionPane.showMessageDialog(listFrame, "Run graph analysis first (Show Skeleton or Load Voronoi).");
                return;
            }
            JFileChooser fc = chooser("Save Successful Visit Matrix CSV");
            if (fc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            try {
                Analysis.exportSuccessfulVisitMatrix(maze, ragRef[0], fc.getSelectedFile());
                JOptionPane.showMessageDialog(listFrame, "Successful visit matrix exported to " + fc.getSelectedFile().getName());
            } catch (Exception ex) { JOptionPane.showMessageDialog(listFrame, "Error: " + ex.getMessage()); }
        });

        JButton exportVertexCountButton = new JButton("Export Vertex Count");
        exportVertexCountButton.addActionListener(ev -> {
            if (ragRef[0] == null || !ragRef[0].isReady()) {
                JOptionPane.showMessageDialog(listFrame, "Run graph analysis first (Show Skeleton or Load Voronoi).");
                return;
            }
            JFileChooser fc = chooser("Save Vertex Count Histogram CSV");
            if (fc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            try {
                linbactsim.analysis.VertexCount vc = new linbactsim.analysis.VertexCount();
                vc.compute(maze, ragRef[0], false);
                vc.exportCsv(fc.getSelectedFile());
                JOptionPane.showMessageDialog(listFrame, "Vertex count histogram exported.");
            } catch (Exception ex) { JOptionPane.showMessageDialog(listFrame, "Error: " + ex.getMessage()); }
        });

        JButton exportSuccessfulVertexCountButton = new JButton("Export Successful Vertex Count");
        exportSuccessfulVertexCountButton.addActionListener(ev -> {
            if (ragRef[0] == null || !ragRef[0].isReady()) {
                JOptionPane.showMessageDialog(listFrame, "Run graph analysis first (Show Skeleton or Load Voronoi).");
                return;
            }
            JFileChooser fc = chooser("Save Successful Vertex Count Histogram CSV");
            if (fc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            try {
                linbactsim.analysis.VertexCount vc = new linbactsim.analysis.VertexCount();
                vc.compute(maze, ragRef[0], true);
                vc.exportCsv(fc.getSelectedFile());
                JOptionPane.showMessageDialog(listFrame, "Successful vertex count histogram exported.");
            } catch (Exception ex) { JOptionPane.showMessageDialog(listFrame, "Error: " + ex.getMessage()); }
        });

        JButton runBulkAnalysisButton = new JButton("Run Bulk Analysis...");
        runBulkAnalysisButton.addActionListener(ev -> {
            if (ragRef[0] == null || !ragRef[0].isReady()) {
                JOptionPane.showMessageDialog(listFrame, "Run graph analysis first (Show Skeleton or Load Voronoi).");
                return;
            }
            if (maze.getBacteriaCount() == 0) {
                JOptionPane.showMessageDialog(listFrame, "Create bacteria first.");
                return;
            }
            JFileChooser expFc = chooser("Select Experimental Vertex Count (CSV or XLSX)");
            expFc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Histogram files (*.csv, *.xlsx, *.xls)", "csv", "xlsx", "xls"));
            if (expFc.showOpenDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            File expFile = expFc.getSelectedFile();

            JFileChooser outFc = chooser("Save Bulk Analysis Results CSV");
            if (outFc.showSaveDialog(listFrame) != JFileChooser.APPROVE_OPTION) return;
            File outFile = outFc.getSelectedFile();

            JDialog progress = new JDialog(listFrame, "Running Bulk Analysis", false);
            JLabel progressLabel = new JLabel("Starting...", SwingConstants.CENTER);
            progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
            progress.add(progressLabel);
            progress.pack();
            progress.setLocationRelativeTo(listFrame);
            progress.setVisible(true);

            SimulationParameters bulkParams = collectParams();
            SwingWorker<java.util.List<linbactsim.analysis.BulkSimulation.ComboResult>, Integer> worker =
                new SwingWorker<>() {
                    java.util.Map<Integer, Integer> expHistogram;
                    @Override
                    protected java.util.List<linbactsim.analysis.BulkSimulation.ComboResult> doInBackground() throws Exception {
                        expHistogram = linbactsim.analysis.HistogramSimilarity.loadFromFile(expFile);
                        return linbactsim.analysis.BulkSimulation.run(
                                maze, ragRef[0], runner, bulkParams, expHistogram,
                                i -> publish(i));
                    }
                    @Override protected void process(java.util.List<Integer> chunks) {
                        int latest = chunks.get(chunks.size() - 1);
                        progressLabel.setText("Running combo " + (latest + 1) + " / "
                                + linbactsim.analysis.BulkSimulation.totalCombos() + "...");
                    }
                    @Override protected void done() {
                        progress.dispose();
                        try {
                            java.util.List<linbactsim.analysis.BulkSimulation.ComboResult> results = get();
                            linbactsim.analysis.BulkSimulation.exportCsv(results, outFile);
                            linbactsim.gui.BulkResultsFrame.show(results, expHistogram);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(listFrame, "Error: " + ex.getMessage());
                        }
                    }
                };
            worker.execute();
        });

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(downloadButton);
        row1.add(downloadSummaryButton);
        row1.add(averageTrajectoryButton);
        row1.add(exportVisitMatrixButton);
        row1.add(exportSuccessfulVisitMatrixButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(exportVertexCountButton);
        row2.add(exportSuccessfulVertexCountButton);
        row2.add(runBulkAnalysisButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(row1);
        bottomPanel.add(row2);

        listFrame.setLayout(new BorderLayout());
        listFrame.add(new JScrollPane(table), BorderLayout.CENTER);
        listFrame.add(bottomPanel, BorderLayout.SOUTH);
        listFrame.setSize(1200, 460);
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
    private void swapMaze(Maze m) {
        runner.stop();
        savedMazeGrid = captureMazeGrid(m);
        mazeRef[0] = m;
        panel.setMaze(m);
        ragRef[0] = null;
        importedVoronoiGrid = null;
        panel.repaint();
    }

    private JFileChooser chooser(String title) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        return fc;
    }

    private SimulationParameters collectParams() {
        SimulationParameters p = new SimulationParameters();
        p.setDt(SimulationParameters.parseIntOrDefault(dtField.getText(), 1));
        p.setDuration(SimulationParameters.parseIntOrDefault(durationField.getText(), 1000));
        p.setVelocity(SimulationParameters.parseOrDefault(velocityField.getText(), 8));
        p.setStdDev(SimulationParameters.parseOrDefault(stdDevField.getText(), 3));
        p.setAngleNoise(SimulationParameters.parseOrDefault(noiseField.getText(), 0.6));
        p.setWMemory(SimulationParameters.parseOrDefault(wMemoryField.getText(), 0.50));
        p.setWNoise(SimulationParameters.parseOrDefault(wNoiseField.getText(), 0.20));
        p.setWWall(SimulationParameters.parseOrDefault(wWallField.getText(), 0.30));
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

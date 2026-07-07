package linbactsim.gui;

import linbactsim.analysis.BulkSimulation.ComboResult;
import linbactsim.analysis.HistogramSimilarity;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// Shows bulk analysis results: ranked table on the left, histogram comparison chart on the right.
// Top 5 rows are highlighted green. Selecting a row updates the chart.
public class BulkResultsFrame extends JFrame {

    private static final Color TOP5_COLOR = new Color(180, 230, 180);

    private final List<ComboResult> results;
    private final Map<Integer, Integer> expHistogram;
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private JFreeChart chart;
    private JLabel detailLabel;

    private BulkResultsFrame(List<ComboResult> results, Map<Integer, Integer> expHistogram) {
        super("Bulk Analysis Results");
        this.results = results;
        this.expHistogram = expHistogram;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        buildUI();
    }

    public static void show(List<ComboResult> results, Map<Integer, Integer> expHistogram) {
        SwingUtilities.invokeLater(() -> new BulkResultsFrame(results, expHistogram).setVisible(true));
    }

    private void buildUI() {
        // --- Table ---
        String[] cols = {"Rank", "wM_norm", "wN_norm", "wW_norm", "Angle", "Score"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < results.size(); i++) {
            ComboResult r = results.get(i);
            model.addRow(new Object[]{
                    i + 1,
                    String.format("%.3f", r.wMemoryNorm()),
                    String.format("%.3f", r.wNoiseNorm()),
                    String.format("%.3f", r.wWallNorm()),
                    String.format("%.2f", r.noiseAngle()),
                    String.format("%.4f", r.score())
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) c.setBackground(row < 5 ? TOP5_COLOR : Color.WHITE);
                return c;
            }
        });

        // --- Chart ---
        chart = ChartFactory.createBarChart(
                "Vertex Count Distribution", "Unique Vertices Visited",
                "Proportion of Bacteria", dataset,
                PlotOrientation.VERTICAL, true, true, false);

        styleChart();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(550, 450));

        // --- Detail label ---
        detailLabel = new JLabel("Select a row to view histogram comparison.");
        detailLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // --- Layout ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), chartPanel);
        split.setDividerLocation(380);

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        add(detailLabel, BorderLayout.SOUTH);

        // --- Selection listener ---
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0 && row < results.size()) updateChart(results.get(row), row + 1);
        });

        // Show first row by default
        if (!results.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            updateChart(results.get(0), 1);
        }
    }

    private void updateChart(ComboResult r, int rank) {
        dataset.clear();

        // Collect all x-axis keys from both histograms
        TreeSet<Integer> allKeys = new TreeSet<>(expHistogram.keySet());
        allKeys.addAll(r.histogram().keySet());

        double simTotal  = r.histogram().values().stream().mapToInt(Integer::intValue).sum();
        double expTotal  = expHistogram.values().stream().mapToInt(Integer::intValue).sum();
        if (simTotal  == 0) simTotal  = 1;
        if (expTotal  == 0) expTotal  = 1;

        for (int k : allKeys) {
            String cat = String.valueOf(k);
            dataset.addValue(r.histogram().getOrDefault(k, 0) / simTotal,  "Simulated",    cat);
            dataset.addValue(expHistogram.getOrDefault(k, 0) / expTotal,   "Experimental", cat);
        }

        double emd = 1.0 / r.score() - 1.0;
        detailLabel.setText(String.format(
                "Rank %d  |  wMemory=%.3f  wNoise=%.3f  wWall=%.3f  angle=%.2f rad  " +
                "|  score=%.4f  (EMD=%.4f)",
                rank, r.wMemoryNorm(), r.wNoiseNorm(), r.wWallNorm(), r.noiseAngle(),
                r.score(), emd));

        chart.setTitle(String.format("Rank %d — Score %.4f", rank, r.score()));
        chart.fireChartChanged();
    }

    private void styleChart() {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(70, 130, 180));   // simulated — steel blue
        renderer.setSeriesPaint(1, new Color(220, 80, 60));    // experimental — red-orange
        renderer.setMaximumBarWidth(0.08);
        renderer.setShadowVisible(false);
    }
}

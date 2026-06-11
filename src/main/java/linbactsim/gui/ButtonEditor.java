package linbactsim.gui;

import linbactsim.model.Bacterium;
import linbactsim.model.Maze;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

// Source: SURE.ButtonEditor (direct carry, package move only)
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor {

    private final JButton button = new JButton("Show Trajectory");
    private final Maze maze;
    private int row;

    public ButtonEditor(Maze maze) {
        this.maze = maze;
        button.addActionListener(e -> {
            Bacterium b = maze.getBacterium(row);
            JFrame trajFrame = new JFrame("Trajectory — Bacterium " + (row + 1));
            trajFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JTextArea area = new JTextArea(java.util.Arrays.deepToString(
                    b.getTrajectory().toArray(new int[0][])));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            trajFrame.add(new JScrollPane(area));
            trajFrame.setSize(600, 300);
            trajFrame.setVisible(true);
            fireEditingStopped();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                  boolean isSelected, int row, int column) {
        this.row = row;
        return button;
    }

    @Override public Object getCellEditorValue()          { return "Show Trajectory"; }
    @Override public boolean isCellEditable(EventObject e){ return true; }
}

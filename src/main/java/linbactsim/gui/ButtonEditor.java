package linbactsim.gui;

import linbactsim.model.Maze;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

// Source: SURE.ButtonEditor (direct carry, package move only)
// Custom JTable cell editor that shows a "Show Trajectory" button
// and opens a trajectory view when clicked.
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor {

    // Source: SURE.ButtonEditor fields
    private final JButton button = new JButton("Show Trajectory");
    private final Maze maze;
    private int row;

    // Source: SURE.ButtonEditor(Maze)
    public ButtonEditor(Maze maze) {
        this.maze = maze;
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.ButtonEditor#getTableCellEditorComponent(...)
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                  boolean isSelected,
                                                  int row, int column) {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.ButtonEditor#getCellEditorValue()
    @Override
    public Object getCellEditorValue() {
        return "Show Trajectory";
    }

    // Source: SURE.ButtonEditor#isCellEditable(EventObject)
    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }
}

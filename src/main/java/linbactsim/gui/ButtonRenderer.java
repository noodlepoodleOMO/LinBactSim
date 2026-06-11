package linbactsim.gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

// Source: SURE.ButtonRenderer (direct carry, package move only)
public class ButtonRenderer extends JButton implements TableCellRenderer {

    public ButtonRenderer() { setOpaque(true); }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                    boolean isSelected, boolean hasFocus,
                                                    int row, int column) {
        setText(value == null ? "Show Trajectory" : value.toString());
        return this;
    }
}

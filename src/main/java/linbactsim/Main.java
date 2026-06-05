package linbactsim;

import linbactsim.gui.ButtonAction;
import javax.swing.SwingUtilities;

// Entry point only. All GUI construction and wiring lives in ButtonAction.
// Source: SURE.Main#main() — stripped of everything except the invokeLater call.
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ButtonAction::launch);
    }
}

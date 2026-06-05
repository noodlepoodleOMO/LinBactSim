package linbactsim.model;

import java.util.List;
import java.util.ArrayList;

// New class — no direct equivalent in SURE.
// Replaces the ad-hoc startRow/startCol + exitPoints list scattered across
// SURE.MazePanel and SURE.Main. Allows multiple independent entry/exit
// configurations to coexist in one simulation run.
public class EntryExitPair {

    private final String label;
    private int entryRow, entryCol;
    private final List<int[]> exitPoints;

    public EntryExitPair(String label, int entryRow, int entryCol) {
        this.label = label;
        this.entryRow = entryRow;
        this.entryCol = entryCol;
        this.exitPoints = new ArrayList<>();
    }

    public void addExit(int row, int col) {
        exitPoints.add(new int[]{row, col});
    }

    public void clearExits() {
        exitPoints.clear();
    }

    public boolean isExit(int row, int col) {
        for (int[] p : exitPoints)
            if (p[0] == row && p[1] == col) return true;
        return false;
    }

    public String  getLabel()      { return label; }
    public int     getEntryRow()   { return entryRow; }
    public int     getEntryCol()   { return entryCol; }
    public List<int[]> getExitPoints() { return exitPoints; }

    public void setEntry(int row, int col) {
        this.entryRow = row;
        this.entryCol = col;
    }
}

package linbactsim.io;

import linbactsim.analysis.RAG;
import linbactsim.model.Maze;

import java.io.File;
import java.io.IOException;

// Exports simulation analysis results to Excel (.xlsx) and CSV files.
// Source: all export buttons inside SURE.Main listButton action (~lines 757–1012)
//         and densityButton exportCountsButton (~lines 616–658).
public class Analysis {

    // Source: SURE.Main exportCountsButton ActionListener (~lines 616–658)
    // Exports per-pixel visit counts as an Excel sheet (-1 for walls).
    public static void exportPixelCounts(Maze maze, File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Main downloadButton ActionListener (~lines 758–798)
    // Exports full [row, col] trajectory for every bacterium (one column each).
    public static void exportTrajectories(Maze maze, File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Main downloadSummaryButton ActionListener (~lines 804–840)
    // Exports bacteria_index, step_number, trajectory_length per bacterium.
    public static void exportStepSummary(Maze maze, File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Main exportVisitMatrixButton ActionListener (~lines 887–942)
    // Exports visit-count matrix: rows = bacteria, columns = junction vertices.
    public static void exportVisitMatrix(Maze maze, RAG rag, File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // Source: SURE.Main exportSuccessfulVisitMatrixButton ActionListener (~lines 950–1012)
    // Same as exportVisitMatrix but restricted to bacteria that reached an exit.
    public static void exportSuccessfulVisitMatrix(Maze maze, RAG rag, File file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }
}

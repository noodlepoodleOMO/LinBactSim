package linbactsim.io;

import linbactsim.analysis.RAG;
import linbactsim.model.Bacterium;
import linbactsim.model.Maze;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

// Exports simulation analysis results to Excel (.xlsx) files.
// Source: all export buttons inside SURE.Main listButton action (~lines 757–1012)
//         and densityButton exportCountsButton (~lines 616–658).
public class Analysis {

    // Source: SURE.Main exportCountsButton ActionListener (~lines 616–658)
    // Exports per-pixel visit counts as an Excel sheet (-1 for walls).
    public static void exportPixelCounts(Maze maze, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Pixel Counts");
            for (int r = 0; r < maze.getNumRows(); r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < maze.getNumCols(); c++) {
                    int val = maze.isWall(r, c) ? -1 : maze.getPixel(r, c).getCount();
                    row.createCell(c).setCellValue(val);
                }
            }
            writeWorkbook(wb, file);
        }
    }

    // Source: SURE.Main downloadButton ActionListener (~lines 758–798)
    // Exports full [row, col] trajectory for every bacterium (two columns each).
    public static void exportTrajectories(Maze maze, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Trajectories");
            // Header row: B1_row, B1_col, B2_row, B2_col, ...
            Row header = sheet.createRow(0);
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                header.createCell(i * 2).setCellValue("B" + (i + 1) + "_row");
                header.createCell(i * 2 + 1).setCellValue("B" + (i + 1) + "_col");
            }
            // Determine max trajectory length
            int maxLen = 0;
            for (int i = 0; i < maze.getBacteriaCount(); i++)
                maxLen = Math.max(maxLen, maze.getBacterium(i).getTrajectory().size());
            // Data rows
            for (int step = 0; step < maxLen; step++) {
                Row row = sheet.createRow(step + 1);
                for (int i = 0; i < maze.getBacteriaCount(); i++) {
                    List<int[]> traj = maze.getBacterium(i).getTrajectory();
                    if (step < traj.size()) {
                        row.createCell(i * 2).setCellValue(traj.get(step)[0]);
                        row.createCell(i * 2 + 1).setCellValue(traj.get(step)[1]);
                    }
                }
            }
            writeWorkbook(wb, file);
        }
    }

    // Source: SURE.Main downloadSummaryButton ActionListener (~lines 804–840)
    // Exports bacteria_index, step_count, trajectory_length per bacterium.
    public static void exportStepSummary(Maze maze, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Step Summary");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("bacteria_index");
            header.createCell(1).setCellValue("step_count");
            header.createCell(2).setCellValue("trajectory_length");
            header.createCell(3).setCellValue("exited");
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                Bacterium b = maze.getBacterium(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(b.getTime());
                row.createCell(2).setCellValue(b.getTrajectoryLength());
                row.createCell(3).setCellValue(b.hasExited() ? 1 : 0);
            }
            writeWorkbook(wb, file);
        }
    }

    // Source: SURE.Main exportVisitMatrixButton ActionListener (~lines 887–942)
    // Exports visit-count matrix: rows = bacteria, columns = junction vertices.
    public static void exportVisitMatrix(Maze maze, RAG rag, File file) throws IOException {
        writeVisitMatrix(maze, rag, file, false);
    }

    // Source: SURE.Main exportSuccessfulVisitMatrixButton ActionListener (~lines 950–1012)
    // Same as exportVisitMatrix but restricted to bacteria that reached an exit.
    public static void exportSuccessfulVisitMatrix(Maze maze, RAG rag, File file) throws IOException {
        writeVisitMatrix(maze, rag, file, true);
    }

    // -------------------------------------------------------------------------
    // Shared visit-matrix writer
    // -------------------------------------------------------------------------
    private static void writeVisitMatrix(Maze maze, RAG rag, File file, boolean exitedOnly)
            throws IOException {
        // Collect all junction IDs present in adjacency map (positive IDs only)
        List<Integer> vertexList = new ArrayList<>();
        if (rag != null && rag.getAdjacency() != null)
            vertexList.addAll(rag.getAdjacency().keySet());
        Collections.sort(vertexList);

        // Collect region trajectories for each bacterium
        List<List<Integer>> allTrajectories = new ArrayList<>();
        for (int i = 0; i < maze.getBacteriaCount(); i++) {
            Bacterium b = maze.getBacterium(i);
            if (exitedOnly && !b.hasExited()) {
                allTrajectories.add(null);
            } else {
                allTrajectories.add(rag != null ? rag.computeRegionTrajectory(b, maze) : List.of());
            }
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(exitedOnly ? "Successful Visit Matrix" : "Visit Matrix");
            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("bacteria_index");
            for (int j = 0; j < vertexList.size(); j++)
                header.createCell(j + 1).setCellValue("v" + vertexList.get(j));

            int rowIdx = 1;
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                List<Integer> traj = allTrajectories.get(i);
                if (traj == null) continue;
                // Count junction visits
                Map<Integer, Integer> counts = new HashMap<>();
                for (int id : traj)
                    if (id > 0) counts.merge(id, 1, Integer::sum);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(i + 1);
                for (int j = 0; j < vertexList.size(); j++)
                    row.createCell(j + 1).setCellValue(counts.getOrDefault(vertexList.get(j), 0));
            }
            writeWorkbook(wb, file);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private static void writeWorkbook(Workbook wb, File file) throws IOException {
        File out = file.getName().toLowerCase().endsWith(".xlsx")
                ? file : new File(file.getAbsolutePath() + ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            wb.write(fos);
        }
    }
}

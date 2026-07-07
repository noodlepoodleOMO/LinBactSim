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
    // Exports full [row, col] trajectory for every bacterium (one column per bacterium,
    // cells as "[row, col]" strings — matches original export format).
    public static void exportTrajectories(Maze maze, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Trajectories");
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                List<int[]> trajectory = maze.getBacterium(i).getTrajectory();
                for (int j = 0; j < trajectory.size(); j++) {
                    Row row = sheet.getRow(j) != null ? sheet.getRow(j) : sheet.createRow(j);
                    int[] pos = trajectory.get(j);
                    row.createCell(i).setCellValue("[" + pos[0] + ", " + pos[1] + "]");
                }
            }
            writeWorkbook(wb, file);
        }
    }

    // Source: SURE.Main downloadSummaryButton ActionListener (~lines 804–840)
    public static void exportStepSummary(Maze maze, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Bacteria Summary");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Bacteria Index");
            header.createCell(1).setCellValue("Step Number");
            header.createCell(2).setCellValue("Trajectory Length");
            for (int i = 0; i < maze.getBacteriaCount(); i++) {
                Bacterium b = maze.getBacterium(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(b.getTime());
                row.createCell(2).setCellValue(b.getTrajectory().size());
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
    // Shared visit-matrix writer — CSV output (matches original export format)
    // -------------------------------------------------------------------------
    private static void writeVisitMatrix(Maze maze, RAG rag, File file, boolean exitedOnly)
            throws IOException {
        int n = maze.getBacteriaCount();

        List<List<Integer>> allTrajectories = new ArrayList<>();
        List<Integer> exitedIndices = new ArrayList<>();
        TreeSet<Integer> allVertices = new TreeSet<>();

        for (int i = 0; i < n; i++) {
            Bacterium b = maze.getBacterium(i);
            if (exitedOnly && !b.hasExited()) continue;
            exitedIndices.add(i);
            List<Integer> traj = rag != null ? rag.computeRegionTrajectory(b, maze) : List.of();
            List<Integer> junctionTraj = new ArrayList<>();
            for (int id : traj) {
                if (id > 0) { junctionTraj.add(id); allVertices.add(id); }
            }
            allTrajectories.add(junctionTraj);
        }

        List<Integer> vertexList = new ArrayList<>(allVertices);
        File out = file.getName().toLowerCase().endsWith(".csv")
                ? file : new File(file.getAbsolutePath() + ".csv");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(out)) {
            StringBuilder header = new StringBuilder("bacteria_index");
            for (int vId : vertexList) header.append(",v").append(vId);
            pw.println(header);

            for (int i = 0; i < exitedIndices.size(); i++) {
                Map<Integer, Integer> counts = new HashMap<>();
                for (int vId : allTrajectories.get(i)) counts.merge(vId, 1, Integer::sum);
                StringBuilder row = new StringBuilder(String.valueOf(exitedIndices.get(i) + 1));
                for (int vId : vertexList) row.append(",").append(counts.getOrDefault(vId, 0));
                pw.println(row);
            }
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

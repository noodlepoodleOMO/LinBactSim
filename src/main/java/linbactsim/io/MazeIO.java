package linbactsim.io;

import linbactsim.model.Maze;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Source: SURE.MazeFileLoader + SURE.MazeImporter + SURE.Main saveMapButton
public class MazeIO {

    // Source: SURE.MazeImporter#importFromCSV — reads a plain 0/1 CSV file
    public static int[][] importFromCSV(File file) throws IOException {
        List<int[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++)
                    row[i] = Integer.parseInt(tokens[i].trim());
                rows.add(row);
            }
        }
        return rows.toArray(new int[0][]);
    }

    // Source: SURE.MazeImporter#importFromImage — not yet implemented
    public static int[][] importFromImage(File file) throws IOException {
        throw new UnsupportedOperationException("Image import not yet implemented");
    }

    // Source: SURE.MazeFileLoader#loadMazeFromResource — reads a bundled resource
    // (Java-array format with braces, or plain comma/space-separated matrix)
    public static int[][] loadFromResource(String resourcePath) {
        InputStream open = tryOpen(resourcePath);
        if (open == null && !resourcePath.startsWith("/")) open = tryOpen("/" + resourcePath);
        if (open == null) open = MazeIO.class.getResourceAsStream(resourcePath);
        if (open == null && !resourcePath.startsWith("/")) open = MazeIO.class.getResourceAsStream("/" + resourcePath);
        if (open == null) {
            System.err.println("[MazeIO] Resource not found: " + resourcePath);
            return null;
        }

        // Pass A: Java-array brace format
        try (BufferedReader br = new BufferedReader(new InputStreamReader(open, StandardCharsets.UTF_8))) {
            List<int[]> rows = new ArrayList<>();
            boolean inRow = false;
            String token = "";
            List<Integer> curr = new ArrayList<>();
            boolean sawAnyBrace = false;

            String line;
            while ((line = br.readLine()) != null) {
                int cmt = line.indexOf("//");
                if (cmt >= 0) line = line.substring(0, cmt);
                if (line.contains("int[")) continue;

                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    if (ch == '{') {
                        sawAnyBrace = true; inRow = true; curr.clear(); token = "";
                    } else if (ch == '}') {
                        if (!token.isEmpty()) { curr.add(Integer.parseInt(token)); token = ""; }
                        if (inRow) { rows.add(curr.stream().mapToInt(Integer::intValue).toArray()); curr = new ArrayList<>(); inRow = false; }
                    } else if (inRow) {
                        if (Character.isDigit(ch) || ch == '-') token += ch;
                        else if (!token.isEmpty()) { curr.add(Integer.parseInt(token)); token = ""; }
                    }
                }
            }
            if (sawAnyBrace && !rows.isEmpty()) return rows.toArray(new int[0][]);
        } catch (Exception ex) { ex.printStackTrace(); return null; }

        // Pass B: plain matrix fallback
        InputStream open2 = tryOpen(resourcePath);
        if (open2 == null && !resourcePath.startsWith("/")) open2 = tryOpen("/" + resourcePath);
        if (open2 == null) open2 = MazeIO.class.getResourceAsStream(resourcePath);
        if (open2 == null && !resourcePath.startsWith("/")) open2 = MazeIO.class.getResourceAsStream("/" + resourcePath);
        if (open2 == null) return null;

        try (BufferedReader br2 = new BufferedReader(new InputStreamReader(open2, StandardCharsets.UTF_8))) {
            List<int[]> rows = new ArrayList<>();
            String line;
            while ((line = br2.readLine()) != null) {
                int cmt = line.indexOf("//");
                if (cmt >= 0) line = line.substring(0, cmt);
                String cleaned = line.replace('{', ' ').replace('}', ' ').replace(';', ' ')
                        .replaceAll("[^0-9,\\- ]", " ").trim();
                if (cleaned.isEmpty()) continue;
                String[] parts = cleaned.split("[,\\s]+");
                int[] row = new int[parts.length];
                for (int i = 0; i < parts.length; i++) row[i] = Integer.parseInt(parts[i]);
                rows.add(row);
            }
            if (rows.isEmpty()) return null;
            return rows.toArray(new int[0][]);
        } catch (Exception ex) { ex.printStackTrace(); return null; }
    }

    // Source: SURE.MazeUtils#loadMazeFromFile — reads from an arbitrary file path
    public static int[][] loadFromFile(String filePath) {
        try { return importFromCSV(new File(filePath)); }
        catch (IOException ex) { ex.printStackTrace(); return null; }
    }

    // Source: SURE.Main saveMapButton (~lines 1299–1328) — writes 0/1 CSV
    public static void saveToCSV(Maze maze, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file)) {
            for (int r = 0; r < maze.getNumRows(); r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < maze.getNumCols(); c++) {
                    if (c > 0) sb.append(',');
                    sb.append(maze.getPixel(r, c).isWall() ? 1 : 0);
                }
                pw.println(sb);
            }
        }
    }

    // Applies a 0/1 grid onto a Maze, setting walls where value == 1.
    public static void applyGrid(int[][] grid, Maze maze) {
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[r].length; c++)
                if (grid[r][c] == 1) maze.setWall(r, c);
    }

    // Applies a wall-position list (as returned by MazeMaps.buildPlazaWalls) onto a Maze.
    public static void applyWallList(int[][] wallList, Maze maze) {
        for (int[] pos : wallList) maze.setWall(pos[0], pos[1]);
    }

    private static InputStream tryOpen(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return (cl != null) ? cl.getResourceAsStream(path) : null;
    }
}

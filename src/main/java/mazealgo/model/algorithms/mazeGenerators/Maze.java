package mazealgo.model.algorithms.mazeGenerators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * A 2D maze: an int grid where 1 is a wall and 0 is a passable cell,
 * with a start position and a goal position.
 *
 * <p>Two wire formats:
 * <ul>
 *   <li>Default Java serialization (via {@link Serializable}) — convenient
 *       but verbose. Used by SolveMazeStrategy where the maze travels
 *       client-to-server once and the overhead doesn't dominate.
 *   <li>{@link #toByteArray()} / {@link #Maze(byte[])} — compact, fixed
 *       header + flat grid bytes. Wrapped by MyCompressorOutputStream's
 *       run-length encoding when shipping back from GenerateMazeStrategy.
 * </ul>
 */
public class Maze implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int rows;
    private final int columns;
    private final int[][] grid;
    private Position start;
    private Position goal;

    public Maze(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.grid = new int[rows][columns];
    }

    /**
     * Reconstructs a maze from {@link #toByteArray()}. Throws
     * {@link IllegalArgumentException} if the byte stream is truncated
     * or malformed (typically: caller passed something that wasn't
     * produced by toByteArray).
     */
    public Maze(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            this.rows = in.readInt();
            this.columns = in.readInt();
            int startRow = in.readInt();
            int startCol = in.readInt();
            int goalRow = in.readInt();
            int goalCol = in.readInt();
            this.grid = new int[rows][columns];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    grid[r][c] = in.readByte() & 0xFF;
                }
            }
            this.start = new Position(startRow, startCol);
            this.goal = new Position(goalRow, goalCol);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid maze byte representation", e);
        }
    }

    /**
     * Compact byte representation: six big-endian ints (rows, columns,
     * start row/col, goal row/col) followed by rows*columns single bytes
     * — one per cell, value 0 or 1. Mirrored by {@link #Maze(byte[])}.
     *
     * <p>The grid portion is what {@code MyCompressorOutputStream} runs
     * RLE over; the header is varied enough that compression doesn't
     * help it, but the grid (long runs of 0 or 1) shrinks dramatically.
     */
    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(rows);
            out.writeInt(columns);
            out.writeInt(start.getRowIndex());
            out.writeInt(start.getColumnIndex());
            out.writeInt(goal.getRowIndex());
            out.writeInt(goal.getColumnIndex());
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    out.writeByte(grid[r][c]);
                }
            }
        } catch (IOException impossible) {
            // ByteArrayOutputStream never throws — assert and move on.
            throw new AssertionError(impossible);
        }
        return baos.toByteArray();
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getCell(int row, int column) {
        return grid[row][column];
    }

    public void setCell(int row, int column, int value) {
        grid[row][column] = value;
    }

    public Position getStartPosition() {
        return start;
    }

    public void setStartPosition(Position start) {
        this.start = start;
    }

    public Position getGoalPosition() {
        return goal;
    }

    public void setGoalPosition(Position goal) {
        this.goal = goal;
    }

    /**
     * Prints the maze. Walls are 1, passages are 0, the start is marked S
     * and the goal is marked E.
     */
    public void print() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                System.out.print(charFor(r, c) + " ");
            }
            System.out.println();
        }
    }

    private String charFor(int r, int c) {
        if (start != null && r == start.getRowIndex() && c == start.getColumnIndex()) {
            return "S";
        }
        if (goal != null && r == goal.getRowIndex() && c == goal.getColumnIndex()) {
            return "E";
        }
        return String.valueOf(grid[r][c]);
    }
}

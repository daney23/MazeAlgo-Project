package mazealgo.model.algorithms.maze3D;

import java.io.Serializable;

/**
 * A 3D maze: an int cube where 1 is a wall and 0 is a passable cell,
 * with a start position and a goal position.
 *
 * <p>Serializable so the server can return a generated 3D maze to the
 * client over an ObjectOutputStream.
 */
public class Maze3D implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int depth;
    private final int rows;
    private final int columns;
    private final int[][][] map;
    private Position3D start;
    private Position3D goal;

    public Maze3D(int depth, int rows, int columns) {
        this.depth = depth;
        this.rows = rows;
        this.columns = columns;
        this.map = new int[depth][rows][columns];
    }

    public int getDepth() {
        return depth;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    /**
     * Required by the spec (page 15): exposes the underlying 3D grid.
     * Callers should treat the returned array as read-only.
     */
    public int[][][] getMap() {
        return map;
    }

    public int getCell(int depth, int row, int column) {
        return map[depth][row][column];
    }

    public void setCell(int depth, int row, int column, int value) {
        map[depth][row][column] = value;
    }

    public Position3D getStartPosition() {
        return start;
    }

    public void setStartPosition(Position3D start) {
        this.start = start;
    }

    public Position3D getGoalPosition() {
        return goal;
    }

    public void setGoalPosition(Position3D goal) {
        this.goal = goal;
    }

    /**
     * Prints the maze layer by layer. Walls are 1, passages are 0,
     * the start is marked S and the goal is marked E.
     */
    public void print() {
        for (int d = 0; d < depth; d++) {
            System.out.println("layer " + d + ":");
            printLayer(d);
        }
    }

    private void printLayer(int d) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                System.out.print(charFor(d, r, c) + " ");
            }
            System.out.println();
        }
    }

    private String charFor(int d, int r, int c) {
        if (matches(start, d, r, c)) return "S";
        if (matches(goal, d, r, c)) return "E";
        return String.valueOf(map[d][r][c]);
    }

    private static boolean matches(Position3D p, int d, int r, int c) {
        return p != null
                && p.getDepthIndex() == d
                && p.getRowIndex() == r
                && p.getColumnIndex() == c;
    }
}

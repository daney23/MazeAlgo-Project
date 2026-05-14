package algorithms.mazeGenerators;

/**
 * A 2D maze: an int grid where 1 is a wall and 0 is a passable cell,
 * with a start position and a goal position.
 */
public class Maze {
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

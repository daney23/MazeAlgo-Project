package algorithms.search;

/**
 * A state in the maze search problem: a (row, column) cell.
 * Equality is based on the cell coordinates so the same cell reached
 * via different paths is treated as one state.
 */
public class MazeState extends AState {
    private final int row;
    private final int column;

    public MazeState(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MazeState)) return false;
        MazeState m = (MazeState) other;
        return row == m.row && column == m.column;
    }

    @Override
    public int hashCode() {
        return row * 31 + column;
    }

    @Override
    public String toString() {
        return "{" + row + "," + column + "}";
    }
}

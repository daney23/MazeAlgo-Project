package mazealgo.model.algorithms.mazeGenerators;

import java.io.Serializable;

/**
 * A row/column position inside a Maze.
 */
public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int column;

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRowIndex() {
        return row;
    }

    public int getColumnIndex() {
        return column;
    }

    @Override
    public String toString() {
        return "{" + row + "," + column + "}";
    }
}

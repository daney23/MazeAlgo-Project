package io.github.daney23.mazealgo.model.algorithms.maze3D;

import java.io.Serializable;

/**
 * A depth/row/column position inside a Maze3D.
 */
public class Position3D implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int depth;
    private final int row;
    private final int column;

    public Position3D(int depth, int row, int column) {
        this.depth = depth;
        this.row = row;
        this.column = column;
    }

    public int getDepthIndex() {
        return depth;
    }

    public int getRowIndex() {
        return row;
    }

    public int getColumnIndex() {
        return column;
    }

    @Override
    public String toString() {
        return "{" + depth + "," + row + "," + column + "}";
    }
}

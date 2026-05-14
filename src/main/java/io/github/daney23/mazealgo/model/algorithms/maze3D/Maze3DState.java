package io.github.daney23.mazealgo.model.algorithms.maze3D;

import io.github.daney23.mazealgo.model.algorithms.search.AState;

/**
 * A state in the 3D maze search problem: a (depth, row, column) cell.
 * Equality is based on the cell coordinates so the same cell reached via
 * different paths is treated as one state.
 */
public class Maze3DState extends AState {
    private static final long serialVersionUID = 1L;

    private final int depth;
    private final int row;
    private final int column;

    public Maze3DState(int depth, int row, int column) {
        this.depth = depth;
        this.row = row;
        this.column = column;
    }

    public int getDepth() {
        return depth;
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
        if (!(other instanceof Maze3DState)) return false;
        Maze3DState m = (Maze3DState) other;
        return depth == m.depth && row == m.row && column == m.column;
    }

    @Override
    public int hashCode() {
        return (depth * 31 + row) * 31 + column;
    }

    @Override
    public String toString() {
        return "{" + depth + "," + row + "," + column + "}";
    }
}

package io.github.daney23.mazealgo.model.algorithms.mazeGenerators;

/**
 * Generates an empty maze (no walls). Start is the top-left cell,
 * goal is the bottom-right cell.
 */
public class EmptyMazeGenerator extends AMazeGenerator {

    @Override
    public Maze generate(int rows, int columns) {
        int[] dims = sanitize(rows, columns);
        rows = dims[0];
        columns = dims[1];
        Maze maze = new Maze(rows, columns);
        maze.setStartPosition(new Position(0, 0));
        maze.setGoalPosition(new Position(rows - 1, columns - 1));
        return maze;
    }
}

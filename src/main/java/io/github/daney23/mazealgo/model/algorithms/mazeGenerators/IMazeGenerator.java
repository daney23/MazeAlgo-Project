package io.github.daney23.mazealgo.model.algorithms.mazeGenerators;

/**
 * A maze generation algorithm.
 */
public interface IMazeGenerator {

    /**
     * Generates a maze with the given number of rows and columns.
     */
    Maze generate(int rows, int columns);

    /**
     * Returns how long generate(rows, columns) takes, in milliseconds.
     */
    long measureAlgorithmTimeMillis(int rows, int columns);
}

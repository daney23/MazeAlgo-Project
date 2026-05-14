package algorithms.maze3D;

/**
 * A 3D maze generation algorithm.
 */
public interface IMaze3DGenerator {

    /**
     * Generates a 3D maze with the given depth, rows and columns.
     */
    Maze3D generate(int depth, int row, int column);

    /**
     * Returns how long generate(depth, rows, columns) takes, in milliseconds.
     */
    long measureAlgorithmTimeMillis(int depth, int row, int column);
}

package algorithms.mazeGenerators;

/**
 * Base class for maze generators.
 * Subclasses implement generate(); time measurement is shared here.
 */
public abstract class AMazeGenerator implements IMazeGenerator {

    @Override
    public abstract Maze generate(int rows, int columns);

    @Override
    public long measureAlgorithmTimeMillis(int rows, int columns) {
        long start = System.currentTimeMillis();
        generate(rows, columns);
        return System.currentTimeMillis() - start;
    }

    // Anything smaller than 2x2 has no room for a real maze; fall back to 3x3.
    protected static int[] sanitize(int rows, int columns) {
        if (rows < 2 || columns < 2) {
            return new int[]{3, 3};
        }
        return new int[]{rows, columns};
    }
}

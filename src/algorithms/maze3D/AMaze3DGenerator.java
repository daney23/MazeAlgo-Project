package algorithms.maze3D;

/**
 * Base class for 3D maze generators.
 * Subclasses implement generate(); time measurement and dimension
 * sanitization are shared here.
 */
public abstract class AMaze3DGenerator implements IMaze3DGenerator {

    @Override
    public abstract Maze3D generate(int depth, int rows, int columns);

    @Override
    public long measureAlgorithmTimeMillis(int depth, int rows, int columns) {
        long start = System.currentTimeMillis();
        generate(depth, rows, columns);
        return System.currentTimeMillis() - start;
    }

    // Anything smaller than 2 on any axis has no room for a real maze;
    // fall back to a 3x3x3 cube.
    protected static int[] sanitize(int depth, int rows, int columns) {
        if (depth < 2 || rows < 2 || columns < 2) {
            return new int[]{3, 3, 3};
        }
        return new int[]{depth, rows, columns};
    }
}

package mazealgo.model.algorithms.mazeGenerators;

import java.util.Random;

/**
 * Scatters walls randomly across the grid, then carves a guaranteed path
 * from start to goal so the maze always has a solution (and usually many).
 */
public class SimpleMazeGenerator extends AMazeGenerator {
    private static final double WALL_DENSITY_IN_MAZE = 0.3; // the amount of walls in the maze
    private final Random random = new Random();

    @Override
    public Maze generate(int rows, int columns) {
        int[] dims = sanitize(rows, columns);
        rows = dims[0];
        columns = dims[1];
        Maze maze = new Maze(rows, columns);
        scatterWalls(maze);
        carvePath(maze);
        maze.setStartPosition(new Position(0, 0));
        maze.setGoalPosition(new Position(rows - 1, columns - 1));
        return maze;
    }

    private void scatterWalls(Maze maze) {
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getColumns(); c++) {
                if (random.nextDouble() < WALL_DENSITY_IN_MAZE) {
                    maze.setCell(r, c, 1);
                }
            }
        }
    }

    // walks from (0,0) to the bottom-right corner, clearing cells along the way
    private void carvePath(Maze maze) {
        int r = 0;
        int c = 0;
        int lastRow = maze.getRows() - 1;
        int lastCol = maze.getColumns() - 1;
        while (r < lastRow || c < lastCol) {
            maze.setCell(r, c, 0);
            if (r == lastRow) {
                c++;
            } else if (c == lastCol) {
                r++;
            } else if (random.nextBoolean()) {
                c++;
            } else {
                r++;
            }
        }
        maze.setCell(lastRow, lastCol, 0);
    }
}

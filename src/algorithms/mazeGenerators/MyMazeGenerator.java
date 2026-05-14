package algorithms.mazeGenerators;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Iterative randomized DFS (recursive backtracker).
 * Cells live at even (row, column) positions. The grid starts as all walls,
 * then the algorithm walks from cell to cell knocking down the wall between
 * the current cell and a random unvisited neighbour, backtracking through
 * a stack when a cell has no unvisited neighbours left.
 */
public class MyMazeGenerator extends AMazeGenerator {

    private static final int[][] STEP = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
    private final Random random = new Random();

    @Override
    public Maze generate(int rows, int columns) {
        int[] dims = sanitize(rows, columns);
        rows = dims[0];
        columns = dims[1];
        Maze maze = new Maze(rows, columns);
        fillWithWalls(maze);
        carve(maze);
        Position start = new Position(0, 0);
        Position goal = new Position(rows - 1, columns - 1);
        maze.setCell(start.getRowIndex(), start.getColumnIndex(), 0);
        // for even dimensions the carved area stops one cell short of the
        // bottom/right edge, so bridge from the last carved cell to the corner
        maze.setCell(rows - 1, lastCell(columns), 0);
        maze.setCell(lastCell(rows), columns - 1, 0);
        maze.setCell(goal.getRowIndex(), goal.getColumnIndex(), 0);
        maze.setStartPosition(start);
        maze.setGoalPosition(goal);
        return maze;
    }

    private void fillWithWalls(Maze maze) {
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getColumns(); c++) {
                maze.setCell(r, c, 1);
            }
        }
    }

    private void carve(Maze maze) {
        int rows = maze.getRows();
        int columns = maze.getColumns();
        boolean[][] visited = new boolean[rows][columns];
        Deque<int[]> stack = new ArrayDeque<>();
        visit(maze, visited, stack, 0, 0);

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            List<int[]> options = unvisitedNeighbours(current, visited, rows, columns);
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = options.get(random.nextInt(options.size()));
            // knock down the wall sitting between current and next
            maze.setCell((current[0] + next[0]) / 2, (current[1] + next[1]) / 2, 0);
            visit(maze, visited, stack, next[0], next[1]);
        }
    }

    private void visit(Maze maze, boolean[][] visited, Deque<int[]> stack, int r, int c) {
        maze.setCell(r, c, 0);
        visited[r][c] = true;
        stack.push(new int[]{r, c});
    }

    private List<int[]> unvisitedNeighbours(int[] cell, boolean[][] visited, int rows, int columns) {
        List<int[]> result = new ArrayList<>(4);
        for (int[] d : STEP) {
            int nr = cell[0] + d[0];
            int nc = cell[1] + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < columns && !visited[nr][nc]) {
                result.add(new int[]{nr, nc});
            }
        }
        return result;
    }

    // last even index that fits inside a dimension of the given size
    private int lastCell(int dimension) {
        return ((dimension - 1) / 2) * 2;
    }
}

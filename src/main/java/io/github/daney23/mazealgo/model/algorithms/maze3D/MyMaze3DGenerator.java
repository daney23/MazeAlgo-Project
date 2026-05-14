package io.github.daney23.mazealgo.model.algorithms.maze3D;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Iterative randomized DFS (recursive backtracker) extended to 3D.
 * Cells live at even (depth, row, column) positions. The cube starts as all
 * walls; the algorithm walks from cell to cell knocking down the wall between
 * the current cell and a random unvisited neighbour. Six step directions
 * (up/down a layer, plus the four 2D directions) instead of the 2D four.
 */
public class MyMaze3DGenerator extends AMaze3DGenerator {

    private static final int[][] STEP = {
            {-2, 0, 0}, {2, 0, 0},
            {0, -2, 0}, {0, 2, 0},
            {0, 0, -2}, {0, 0, 2}
    };
    private final Random random = new Random();

    @Override
    public Maze3D generate(int depth, int rows, int columns) {
        int[] dims = sanitize(depth, rows, columns);
        depth = dims[0];
        rows = dims[1];
        columns = dims[2];
        Maze3D maze = new Maze3D(depth, rows, columns);
        fillWithWalls(maze);
        carve(maze);
        Position3D start = new Position3D(0, 0, 0);
        Position3D goal = new Position3D(depth - 1, rows - 1, columns - 1);
        maze.setCell(0, 0, 0, 0);
        // for even dimensions the carved area stops one cell short of the far
        // edge on that axis. Open the whole corner sub-cube so the goal
        // connects back into the carved network through (lastCell,...,...).
        for (int d = lastCell(depth); d < depth; d++) {
            for (int r = lastCell(rows); r < rows; r++) {
                for (int c = lastCell(columns); c < columns; c++) {
                    maze.setCell(d, r, c, 0);
                }
            }
        }
        maze.setStartPosition(start);
        maze.setGoalPosition(goal);
        return maze;
    }

    private void fillWithWalls(Maze3D maze) {
        for (int d = 0; d < maze.getDepth(); d++) {
            for (int r = 0; r < maze.getRows(); r++) {
                for (int c = 0; c < maze.getColumns(); c++) {
                    maze.setCell(d, r, c, 1);
                }
            }
        }
    }

    private void carve(Maze3D maze) {
        int depth = maze.getDepth();
        int rows = maze.getRows();
        int columns = maze.getColumns();
        boolean[][][] visited = new boolean[depth][rows][columns];
        Deque<int[]> stack = new ArrayDeque<>();
        visit(maze, visited, stack, 0, 0, 0);

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            List<int[]> options = unvisitedNeighbours(current, visited, depth, rows, columns);
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = options.get(random.nextInt(options.size()));
            // knock down the wall between current and next
            maze.setCell(
                    (current[0] + next[0]) / 2,
                    (current[1] + next[1]) / 2,
                    (current[2] + next[2]) / 2,
                    0);
            visit(maze, visited, stack, next[0], next[1], next[2]);
        }
    }

    private void visit(Maze3D maze, boolean[][][] visited, Deque<int[]> stack, int d, int r, int c) {
        maze.setCell(d, r, c, 0);
        visited[d][r][c] = true;
        stack.push(new int[]{d, r, c});
    }

    private List<int[]> unvisitedNeighbours(int[] cell, boolean[][][] visited, int depth, int rows, int columns) {
        List<int[]> result = new ArrayList<>(6);
        for (int[] s : STEP) {
            int nd = cell[0] + s[0];
            int nr = cell[1] + s[1];
            int nc = cell[2] + s[2];
            if (nd >= 0 && nd < depth
                    && nr >= 0 && nr < rows
                    && nc >= 0 && nc < columns
                    && !visited[nd][nr][nc]) {
                result.add(new int[]{nd, nr, nc});
            }
        }
        return result;
    }

    // last even index that fits inside a dimension of the given size
    private int lastCell(int dimension) {
        return ((dimension - 1) / 2) * 2;
    }
}

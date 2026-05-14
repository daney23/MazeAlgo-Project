package mazealgo.model.algorithms.search;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.Position;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BestFirstSearchTest {

    private BestFirstSearch searcher;

    @BeforeEach
    void setUp() {
        searcher = new BestFirstSearch();
    }

    @Test
    void getName_returnsBestFirstSearch() {
        assertEquals("BestFirstSearch", searcher.getName());
    }

    @Test
    void nodesEvaluated_isZeroBeforeSolve() {
        assertEquals(0, searcher.getNumberOfNodesEvaluated());
    }

    @Test
    void nodesEvaluated_isPositiveAfterSolve() {
        searcher.solve(new SearchableMaze(openMaze(3, 3)));
        assertTrue(searcher.getNumberOfNodesEvaluated() > 0);
    }

    @Test
    void solve_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> searcher.solve(null));
    }

    @Test
    void solve_singleCellMaze_returnsPathOfLengthOne() {
        Maze maze = new Maze(1, 1);
        maze.setStartPosition(new Position(0, 0));
        maze.setGoalPosition(new Position(0, 0));

        Solution solution = searcher.solve(new SearchableMaze(maze));

        ArrayList<AState> path = solution.getSolutionPath();
        assertEquals(1, path.size());
        MazeState only = (MazeState) path.get(0);
        assertEquals(0, only.getRow());
        assertEquals(0, only.getColumn());
    }

    @Test
    void solve_unsolvableMaze_returnsNonNullEmptyPath() {
        // start at (0,0) walled in on every side, goal somewhere unreachable
        Maze maze = new Maze(3, 3);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                maze.setCell(r, c, 1);
            }
        }
        maze.setCell(0, 0, 0);
        maze.setCell(2, 2, 0);
        maze.setStartPosition(new Position(0, 0));
        maze.setGoalPosition(new Position(2, 2));

        Solution solution = searcher.solve(new SearchableMaze(maze));

        assertNotNull(solution);
        assertTrue(solution.getSolutionPath().isEmpty());
    }

    @Test
    void solve_solvableMaze_pathStartsAtStartAndEndsAtGoal() {
        Maze maze = openMaze(3, 3);
        Solution solution = searcher.solve(new SearchableMaze(maze));
        ArrayList<AState> path = solution.getSolutionPath();

        assertFalse(path.isEmpty());
        MazeState first = (MazeState) path.get(0);
        MazeState last = (MazeState) path.get(path.size() - 1);
        assertEquals(0, first.getRow());
        assertEquals(0, first.getColumn());
        assertEquals(2, last.getRow());
        assertEquals(2, last.getColumn());
    }

    @Test
    void solve_solvableMaze_consecutiveStatesAreAdjacent() {
        Maze maze = openMaze(5, 5);
        Solution solution = searcher.solve(new SearchableMaze(maze));
        ArrayList<AState> path = solution.getSolutionPath();

        for (int i = 1; i < path.size(); i++) {
            MazeState prev = (MazeState) path.get(i - 1);
            MazeState curr = (MazeState) path.get(i);
            int dr = Math.abs(prev.getRow() - curr.getRow());
            int dc = Math.abs(prev.getColumn() - curr.getColumn());
            assertTrue(dr <= 1 && dc <= 1);
            assertFalse(dr == 0 && dc == 0);
        }
    }

    // fully open rows x columns maze, start (0,0), goal bottom-right corner
    private Maze openMaze(int rows, int columns) {
        Maze maze = new Maze(rows, columns);
        maze.setStartPosition(new Position(0, 0));
        maze.setGoalPosition(new Position(rows - 1, columns - 1));
        return maze;
    }
}

package mazealgo.model.algorithms.search;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listener is what powers the UI's "Watch Search" visualization,
 * so these tests pin its contract:
 *  - fired once per node committed to the visited set,
 *  - count matches {@link ISearchingAlgorithm#getNumberOfNodesEvaluated()},
 *  - default is a no-op (no NPE) when no listener is registered.
 */
class NodeEvaluatedListenerTest {

    private static Maze openMaze(int rows, int cols) {
        Maze m = new Maze(rows, cols);
        m.setStartPosition(new Position(0, 0));
        m.setGoalPosition(new Position(rows - 1, cols - 1));
        return m;
    }

    @Test
    void breadthFirst_firesListener_oncePerEvaluatedNode() {
        BreadthFirstSearch searcher = new BreadthFirstSearch();
        List<AState> visited = new ArrayList<>();
        searcher.setNodeEvaluatedListener(visited::add);

        searcher.solve(new SearchableMaze(openMaze(4, 4)));

        assertEquals(searcher.getNumberOfNodesEvaluated(), visited.size(),
                "listener fires count should match getNumberOfNodesEvaluated");
        assertTrue(visited.size() > 0);
    }

    @Test
    void depthFirst_firesListener_oncePerEvaluatedNode() {
        DepthFirstSearch searcher = new DepthFirstSearch();
        List<AState> visited = new ArrayList<>();
        searcher.setNodeEvaluatedListener(visited::add);

        searcher.solve(new SearchableMaze(openMaze(4, 4)));

        assertEquals(searcher.getNumberOfNodesEvaluated(), visited.size());
    }

    @Test
    void bestFirst_firesListener_oncePerEvaluatedNode() {
        BestFirstSearch searcher = new BestFirstSearch();
        List<AState> visited = new ArrayList<>();
        searcher.setNodeEvaluatedListener(visited::add);

        searcher.solve(new SearchableMaze(openMaze(5, 5)));

        assertEquals(searcher.getNumberOfNodesEvaluated(), visited.size());
    }

    @Test
    void noListener_isNoOp() {
        // Smoke test: solving without a registered listener must not throw.
        new BreadthFirstSearch().solve(new SearchableMaze(openMaze(3, 3)));
        new DepthFirstSearch().solve(new SearchableMaze(openMaze(3, 3)));
        new BestFirstSearch().solve(new SearchableMaze(openMaze(3, 3)));
    }

    @Test
    void firstNotification_isAlwaysTheStartState() {
        BestFirstSearch searcher = new BestFirstSearch();
        List<AState> visited = new ArrayList<>();
        searcher.setNodeEvaluatedListener(visited::add);

        searcher.solve(new SearchableMaze(openMaze(4, 4)));

        assertEquals(new MazeState(0, 0), visited.get(0));
    }

    @Test
    void nullListener_resetsToNoOp() {
        BestFirstSearch searcher = new BestFirstSearch();
        List<AState> recorded = new ArrayList<>();
        searcher.setNodeEvaluatedListener(recorded::add);
        searcher.setNodeEvaluatedListener(null);

        searcher.solve(new SearchableMaze(openMaze(3, 3)));

        assertTrue(recorded.isEmpty(), "after null-reset, no notifications");
    }
}

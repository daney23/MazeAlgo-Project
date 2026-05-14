package mazealgo.model.algorithms.search;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Breadth First Search. Uses a FIFO queue as the open list, so the
 * first time a goal cell is reached it has the fewest edges from
 * the start. The same skeleton is reused by BestFirstSearch, which
 * only swaps the queue for a priority queue.
 */
public class BreadthFirstSearch extends ASearchingAlgorithm {
    protected Queue<AState> openList;

    public BreadthFirstSearch() {
        openList = new LinkedList<>();
    }

    @Override
    protected void reset() {
        super.reset();
        // If solve() returned early on a previous run, the queue can
        // still hold leftover states; clear it so reusing this instance
        // (e.g. in a server thread pool) is safe.
        openList.clear();
    }

    @Override
    public Solution solve(ISearchable domain) {
        reset();
        AState start = domain.getStartState();
        AState goal = domain.getGoalState();
        openList.add(start);
        Set<AState> visited = new HashSet<>();

        while (!openList.isEmpty()) {
            AState current = openList.poll();
            if (!visited.add(current)) continue;
            numberOfNodesEvaluated++;
            if (current.equals(goal)) {
                return new Solution(current);
            }
            for (AState next : domain.getAllPossibleStates(current)) {
                if (!visited.contains(next)) {
                    openList.add(next);
                }
            }
        }
        return new Solution(null);
    }

    @Override
    public String getName() {
        return "BreadthFirstSearch";
    }
}

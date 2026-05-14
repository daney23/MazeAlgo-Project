package mazealgo.model.algorithms.search;

/**
 * Base class for search algorithms. Owns the node-evaluation counter and
 * a {@link #reset()} hook that subclasses must call at the top of
 * {@link #solve(ISearchable)}.
 *
 * <p>Resetting matters for the server-client layer: a {@code SolveMazeStrategy}
 * may pool searcher instances across many client requests, and a leftover
 * counter (or, for BFS, a non-empty open list) from a previous solve would
 * silently corrupt the next response.
 */
public abstract class ASearchingAlgorithm implements ISearchingAlgorithm {
    protected int numberOfNodesEvaluated = 0;

    /**
     * Resets per-solve state. Subclasses with their own mutable state
     * (e.g. an open list) should override this and call {@code super.reset()}.
     */
    protected void reset() {
        numberOfNodesEvaluated = 0;
    }

    @Override
    public int getNumberOfNodesEvaluated() {
        return numberOfNodesEvaluated;
    }
}

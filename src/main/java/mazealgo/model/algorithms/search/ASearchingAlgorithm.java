package mazealgo.model.algorithms.search;

import java.util.function.Consumer;

/**
 * Base class for search algorithms. Owns the node-evaluation counter,
 * a {@link #reset()} hook that subclasses must call at the top of
 * {@link #solve(ISearchable)}, and an optional listener that lets a
 * UI watch the search happen in real time.
 *
 * <p>Resetting matters for the server-client layer: a {@code SolveMazeStrategy}
 * may pool searcher instances across many client requests, and a leftover
 * counter (or, for BFS, a non-empty open list) from a previous solve would
 * silently corrupt the next response.
 *
 * <p>The listener is fired once per state the algorithm pops off its open
 * structure and commits to (i.e. each time {@code numberOfNodesEvaluated}
 * is incremented). Defaults to a no-op so the server path pays nothing.
 */
public abstract class ASearchingAlgorithm implements ISearchingAlgorithm {
    private static final Consumer<AState> NO_OP = state -> {};

    protected int numberOfNodesEvaluated = 0;
    private Consumer<AState> nodeEvaluatedListener = NO_OP;

    /**
     * Registers a listener invoked once per node the algorithm visits
     * (synchronously, on the thread running {@link #solve(ISearchable)}).
     * Pass {@code null} to clear. Used by the UI's "Watch Search" mode
     * to paint visited cells as the search proceeds.
     */
    public void setNodeEvaluatedListener(Consumer<AState> listener) {
        this.nodeEvaluatedListener = listener != null ? listener : NO_OP;
    }

    /** Called by subclasses each time a node is committed to the visited set. */
    protected void onNodeEvaluated(AState state) {
        nodeEvaluatedListener.accept(state);
    }

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

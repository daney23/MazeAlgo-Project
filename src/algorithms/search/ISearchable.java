package algorithms.search;

import java.util.List;

/**
 * A search problem: knows its start, its goal, and how to enumerate
 * the legal successors of any given state. The domain also owns its
 * own heuristic so informed searchers (e.g. BestFirstSearch) don't
 * need to know the concrete state types.
 */
public interface ISearchable {
    AState getStartState();

    AState getGoalState();

    List<AState> getAllPossibleStates(AState state);

    /**
     * Estimated remaining cost from {@code from} to {@code to}, used by
     * informed searchers. Must be admissible (never overestimate) for
     * A* to return an optimal path. Default is 0, which makes any
     * informed searcher fall back to plain Dijkstra.
     */
    default double heuristic(AState from, AState to) {
        return 0;
    }
}

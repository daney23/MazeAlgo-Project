package algorithms.search;

/**
 * Base class for search algorithms. Owns the node-evaluation counter;
 * subclasses provide the actual search strategy and a name.
 */
public abstract class ASearchingAlgorithm implements ISearchingAlgorithm {
    protected int numberOfNodesEvaluated = 0;

    @Override
    public int getNumberOfNodesEvaluated() {
        return numberOfNodesEvaluated;
    }
}

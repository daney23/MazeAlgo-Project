package mazealgo.model.algorithms.search;

/**
 * A search algorithm: solves a problem given as an ISearchable and
 * reports a name and the number of states it evaluated along the way.
 */
public interface ISearchingAlgorithm {
    Solution solve(ISearchable domain);

    String getName();

    int getNumberOfNodesEvaluated();
}

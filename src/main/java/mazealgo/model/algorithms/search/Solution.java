package mazealgo.model.algorithms.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The path returned by a search algorithm: an ordered list of states
 * from start to goal (inclusive). Built by walking {@code cameFrom}
 * from the goal back to the start, then reversing.
 *
 * <p>Carries the number of nodes the producing algorithm evaluated so
 * the UI can keep its "Nodes: N" counter accurate even when the solve
 * happened on the server (the count survives the
 * {@link java.io.ObjectOutputStream} roundtrip).
 *
 * <p>Once constructed, the Solution is fully self-contained — it holds
 * its own list of states and no longer depends on the search algorithm
 * or the visited set.
 */
public class Solution implements Serializable {
    private static final long serialVersionUID = 2L;

    private final ArrayList<AState> path;
    private int nodesEvaluated;

    public Solution(AState goal) {
        path = new ArrayList<>();
        AState cur = goal;
        while (cur != null) {
            path.add(cur);
            cur = cur.getCameFrom();
        }
        Collections.reverse(path);
    }

    public ArrayList<AState> getSolutionPath() {
        return path;
    }

    public int getNodesEvaluated() {
        return nodesEvaluated;
    }

    public void setNodesEvaluated(int nodesEvaluated) {
        this.nodesEvaluated = nodesEvaluated;
    }
}

package mazealgo.model.algorithms.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The path returned by a search algorithm: an ordered list of states
 * from start to goal (inclusive). Built by walking cameFrom from the
 * goal back to the start, then reversing.
 *
 * <p>Serializable so the server can return a solved path to the client
 * over an ObjectOutputStream. Once constructed, the Solution is fully
 * self-contained - it holds its own list of states and no longer
 * depends on the search algorithm or the visited set.
 */
public class Solution implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<AState> path;

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
}

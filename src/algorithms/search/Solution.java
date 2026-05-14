package algorithms.search;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The path returned by a search algorithm: an ordered list of states
 * from start to goal (inclusive). Built by walking cameFrom from the
 * goal back to the start, then reversing.
 */
public class Solution {
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

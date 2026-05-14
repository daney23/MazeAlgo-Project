package io.github.daney23.mazealgo.model.algorithms.search;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Depth First Search. Uses an explicit LIFO stack instead of recursion
 * so it can handle large mazes (e.g. 1000x1000) without blowing the
 * call stack. The path it finds is some path to the goal, not
 * necessarily the shortest.
 */
public class DepthFirstSearch extends ASearchingAlgorithm {

    @Override
    public Solution solve(ISearchable domain) {
        reset();
        AState start = domain.getStartState();
        AState goal = domain.getGoalState();
        Deque<AState> stack = new ArrayDeque<>();
        stack.push(start);
        Set<AState> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            AState current = stack.pop();
            if (!visited.add(current)) continue;
            numberOfNodesEvaluated++;
            if (current.equals(goal)) {
                return new Solution(current);
            }
            for (AState next : domain.getAllPossibleStates(current)) {
                if (!visited.contains(next)) {
                    stack.push(next);
                }
            }
        }
        return new Solution(null);
    }

    @Override
    public String getName() {
        return "DepthFirstSearch";
    }
}

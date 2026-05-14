package io.github.daney23.mazealgo.model.algorithms.search;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Best First Search algorithm.
 * It works just like Breadth First Search (BFS), but instead of exploring equally in all directions,
 * it uses a "smart" queue (a priority queue) to decide where to go next.
 * It prioritizes states based on their actual cost so far PLUS an estimated cost to the goal,
 * obtained from the domain's own heuristic. This makes it act like A*, finding the
 * cheapest path while exploring far fewer nodes than BFS - assuming the domain
 * provides an admissible heuristic.
 */
public class BestFirstSearch extends BreadthFirstSearch {

    public BestFirstSearch() {
        super();
    }

    @Override
    public Solution solve(ISearchable domain) {
        // We need to know where we are going so we can calculate the distance to it.
        AState goal = domain.getGoalState();

        // We override the openList from BFS with a PriorityQueue.
        // The "smart" part: we sort the queue by total cost = (cost so far) + (domain's
        // estimate of remaining cost). The domain - not this class - knows its own
        // state types and cost weights, so we stay decoupled from any concrete state.
        openList = new PriorityQueue<>(
                Comparator.comparingDouble(s -> s.getCost() + domain.heuristic(s, goal))
        );

        // After setting up our smart queue, we just let the normal BFS logic run.
        return super.solve(domain);
    }

    @Override
    public String getName() {
        return "BestFirstSearch";
    }
}

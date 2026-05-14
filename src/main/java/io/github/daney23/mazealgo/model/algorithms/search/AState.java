package io.github.daney23.mazealgo.model.algorithms.search;

import java.io.Serializable;

/**
 * A state in a search problem. Carries the parent it was reached from
 * (cameFrom) and the accumulated path cost from the start.
 *
 * <p>Serializable so a {@link Solution} can be sent across the wire from
 * the server to the client. The {@code cameFrom} chain is walked at
 * Solution-construction time, so serializing a Solution doesn't drag
 * along the entire visited graph - just the path states.
 */
public abstract class AState implements Serializable {
    private static final long serialVersionUID = 1L;

    private AState cameFrom;
    private double cost;

    public AState getCameFrom() {
        return cameFrom;
    }

    public void setCameFrom(AState cameFrom) {
        this.cameFrom = cameFrom;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}

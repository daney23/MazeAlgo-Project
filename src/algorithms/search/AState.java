package algorithms.search;

/**
 * A state in a search problem. Carries the parent it was reached from
 * (cameFrom) and the accumulated path cost from the start.
 */
public abstract class AState {
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

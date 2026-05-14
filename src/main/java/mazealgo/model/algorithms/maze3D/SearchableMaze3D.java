package mazealgo.model.algorithms.maze3D;

import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.ISearchable;

import java.util.ArrayList;
import java.util.List;

/**
 * Object Adapter that exposes a Maze3D as an ISearchable. Six straight
 * neighbours per cell (one step on each axis); no diagonals in 3D.
 */
public class SearchableMaze3D implements ISearchable {
    private static final double STRAIGHT_COST = 10;

    private final Maze3D maze;
    private final Maze3DState start;
    private final Maze3DState goal;

    public SearchableMaze3D(Maze3D maze) {
        this.maze = maze;
        Position3D s = maze.getStartPosition();
        Position3D g = maze.getGoalPosition();
        this.start = new Maze3DState(s.getDepthIndex(), s.getRowIndex(), s.getColumnIndex());
        this.goal = new Maze3DState(g.getDepthIndex(), g.getRowIndex(), g.getColumnIndex());
    }

    @Override
    public AState getStartState() {
        return start;
    }

    @Override
    public AState getGoalState() {
        return goal;
    }

    @Override
    public List<AState> getAllPossibleStates(AState state) {
        List<AState> result = new ArrayList<>();
        if (!(state instanceof Maze3DState)) return result;
        Maze3DState ms = (Maze3DState) state;
        int d = ms.getDepth();
        int r = ms.getRow();
        int c = ms.getColumn();

        addStraight(state, d - 1, r, c, result);
        addStraight(state, d + 1, r, c, result);
        addStraight(state, d, r - 1, c, result);
        addStraight(state, d, r + 1, c, result);
        addStraight(state, d, r, c - 1, result);
        addStraight(state, d, r, c + 1, result);

        return result;
    }

    private void addStraight(AState parent, int nd, int nr, int nc, List<AState> out) {
        if (passable(nd, nr, nc)) {
            Maze3DState child = new Maze3DState(nd, nr, nc);
            child.setCameFrom(parent);
            child.setCost(parent.getCost() + STRAIGHT_COST);
            out.add(child);
        }
    }

    private boolean passable(int d, int r, int c) {
        return d >= 0 && d < maze.getDepth()
                && r >= 0 && r < maze.getRows()
                && c >= 0 && c < maze.getColumns()
                && maze.getCell(d, r, c) == 0;
    }

    /**
     * Manhattan distance scaled by the straight-step cost. Admissible
     * because there are no diagonals in 3D - every step costs exactly
     * STRAIGHT_COST, so |dd|+|dr|+|dc| straight steps is the cheapest
     * any path can possibly be.
     */
    @Override
    public double heuristic(AState from, AState to) {
        if (!(from instanceof Maze3DState) || !(to instanceof Maze3DState)) return 0;
        Maze3DState a = (Maze3DState) from;
        Maze3DState b = (Maze3DState) to;
        int dd = Math.abs(a.getDepth() - b.getDepth());
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getColumn() - b.getColumn());
        return STRAIGHT_COST * (dd + dr + dc);
    }
}

package io.github.daney23.mazealgo.model.algorithms.search;

import io.github.daney23.mazealgo.model.algorithms.mazeGenerators.Maze;
import io.github.daney23.mazealgo.model.algorithms.mazeGenerators.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Object Adapter that exposes a Maze as an ISearchable. Straight moves
 * (up/down/left/right) cost less than diagonal moves; a diagonal move
 * is only legal if at least one of the two orthogonal cells next to
 * the corner is also open, so the path cannot squeeze through a
 * pinhole between two walls.
 */
public class SearchableMaze implements ISearchable {
    private static final double STRAIGHT_COST = 10;
    private static final double DIAGONAL_COST = 15;

    private final Maze maze;
    private final MazeState start;
    private final MazeState goal;

    public SearchableMaze(Maze maze) {
        this.maze = maze;
        Position s = maze.getStartPosition();
        Position g = maze.getGoalPosition();
        this.start = new MazeState(s.getRowIndex(), s.getColumnIndex());
        this.goal = new MazeState(g.getRowIndex(), g.getColumnIndex());
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
        if (!(state instanceof MazeState)) return result;
        MazeState ms = (MazeState) state;
        int r = ms.getRow();
        int c = ms.getColumn();

        addStraight(state, r - 1, c, result);
        addStraight(state, r, c + 1, result);
        addStraight(state, r + 1, c, result);
        addStraight(state, r, c - 1, result);

        addDiagonal(state, r, c, r - 1, c + 1, result);
        addDiagonal(state, r, c, r + 1, c + 1, result);
        addDiagonal(state, r, c, r + 1, c - 1, result);
        addDiagonal(state, r, c, r - 1, c - 1, result);

        return result;
    }

    private void addStraight(AState parent, int nr, int nc, List<AState> out) {
        if (passable(nr, nc)) {
            out.add(makeChild(parent, nr, nc, STRAIGHT_COST));
        }
    }

    private void addDiagonal(AState parent, int r, int c, int nr, int nc, List<AState> out) {
        if (passable(nr, nc) && (passable(r, nc) || passable(nr, c))) {
            out.add(makeChild(parent, nr, nc, DIAGONAL_COST));
        }
    }

    private MazeState makeChild(AState parent, int nr, int nc, double edgeCost) {
        MazeState child = new MazeState(nr, nc);
        child.setCameFrom(parent);
        child.setCost(parent.getCost() + edgeCost);
        return child;
    }

    private boolean passable(int r, int c) {
        return r >= 0 && r < maze.getRows()
                && c >= 0 && c < maze.getColumns()
                && maze.getCell(r, c) == 0;
    }

    /**
     * Octile distance scaled to the straight/diagonal cost weights.
     * Equivalent to 15*min(|dr|,|dc|) + 10*(max-min), which is the
     * exact optimal cost on an unobstructed grid - so it's admissible.
     */
    @Override
    public double heuristic(AState from, AState to) {
        if (!(from instanceof MazeState) || !(to instanceof MazeState)) return 0;
        MazeState a = (MazeState) from;
        MazeState b = (MazeState) to;
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getColumn() - b.getColumn());
        return 10 * Math.max(dr, dc) + 5 * Math.min(dr, dc);
    }
}

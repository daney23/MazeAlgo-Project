package mazealgo.viewmodel;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import mazealgo.model.MazeModel;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge between {@link MazeModel} and the JavaFX View. Holds observable
 * properties the view binds to (current maze, current solution, player
 * row / column, victory flag, nodes-evaluated counter, visited states)
 * and exposes commands (generate, move, solve, visualize) that the
 * controller calls in response to user input.
 *
 * <p>The view never touches the model directly — it only reads and
 * writes these properties and invokes commands here.
 */
public class MazeViewModel {
    private final MazeModel model;

    private final ObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final IntegerProperty playerRow = new SimpleIntegerProperty();
    private final IntegerProperty playerColumn = new SimpleIntegerProperty();
    private final BooleanProperty solved = new SimpleBooleanProperty(false);

    // ReadOnly because the controller should only observe these, not poke them.
    private final ReadOnlyBooleanWrapper victory = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyIntegerWrapper nodesEvaluated = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyBooleanWrapper visualizing = new ReadOnlyBooleanWrapper(false);

    private final ObservableSet<AState> visitedStates = FXCollections.observableSet(new HashSet<>());
    private final AtomicBoolean cancelVisualization = new AtomicBoolean(false);

    public MazeViewModel(MazeModel model) {
        this.model = model;
    }

    // ─── Commands ────────────────────────────────────────────────────

    public void generate(int rows, int columns) {
        cancelVisualization.set(true);
        Maze m = model.generate(rows, columns);
        maze.set(m);
        playerRow.set(m.getStartPosition().getRowIndex());
        playerColumn.set(m.getStartPosition().getColumnIndex());
        solution.set(null);
        solved.set(false);
        victory.set(false);
        nodesEvaluated.set(0);
        visitedStates.clear();
    }

    /**
     * Attempts to move the player one cell in the given direction. No-op
     * if the target cell is out of bounds, a wall, or — for diagonals —
     * if both adjacent orthogonal cells around the corner are walls
     * (the same pinhole rule {@code SearchableMaze} enforces, so the
     * UI move set matches what the search algorithms consider legal).
     */
    public void movePlayer(MovementDirection dir) {
        Maze m = maze.get();
        if (m == null || victory.get()) return;

        int r = playerRow.get();
        int c = playerColumn.get();
        int nr = r + dir.dr;
        int nc = c + dir.dc;

        if (!passable(m, nr, nc)) return;
        if (dir.diagonal && !passable(m, r, nc) && !passable(m, nr, c)) return;

        playerRow.set(nr);
        playerColumn.set(nc);

        if (atGoal(m, nr, nc)) {
            victory.set(true);
        }
    }

    /** Solves synchronously and shows the path. Used by the "Solution Hint" button. */
    public void solveCurrent() {
        Maze m = maze.get();
        if (m == null) return;
        BestFirstSearch searcher = new BestFirstSearch();
        Solution sol = searcher.solve(new SearchableMaze(m));
        solution.set(sol);
        nodesEvaluated.set(searcher.getNumberOfNodesEvaluated());
        solved.set(true);
    }

    /**
     * Runs Best-First Search on a daemon thread and feeds each visited
     * state into {@link #visitedStates} on the JavaFX thread, with an
     * adaptive per-cell delay so the animation always fits a bounded
     * budget regardless of maze size.
     *
     * <p>Calling this while a visualization is in flight cancels the
     * previous one (the listener observes {@link #cancelVisualization}
     * and bails). Same on {@link #generate(int, int)}.
     */
    public void visualizeSearchAsync() {
        Maze m = maze.get();
        if (m == null || visualizing.get()) return;

        cancelVisualization.set(false);
        visualizing.set(true);
        solution.set(null);
        visitedStates.clear();
        nodesEvaluated.set(0);

        int totalCells = m.getRows() * m.getColumns();
        // 3-second budget across the search, clamped to [1, 25]ms per node.
        long perCellDelayMs = Math.max(1, Math.min(25, 3000L / Math.max(1, totalCells)));

        Thread t = new Thread(() -> {
            BestFirstSearch searcher = new BestFirstSearch();
            searcher.setNodeEvaluatedListener(state -> {
                if (cancelVisualization.get()) {
                    throw new VisualizationCancelledException();
                }
                int count = searcher.getNumberOfNodesEvaluated();
                Platform.runLater(() -> {
                    visitedStates.add(state);
                    nodesEvaluated.set(count);
                });
                try {
                    Thread.sleep(perCellDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            });
            Solution sol;
            try {
                sol = searcher.solve(new SearchableMaze(m));
            } catch (VisualizationCancelledException cancelled) {
                Platform.runLater(() -> visualizing.set(false));
                return;
            }
            int finalCount = searcher.getNumberOfNodesEvaluated();
            Platform.runLater(() -> {
                solution.set(sol);
                nodesEvaluated.set(finalCount);
                visualizing.set(false);
                solved.set(true);
            });
        }, "VisualizeSearch");
        t.setDaemon(true);
        t.start();
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private static boolean passable(Maze m, int r, int c) {
        return r >= 0 && r < m.getRows()
                && c >= 0 && c < m.getColumns()
                && m.getCell(r, c) == 0;
    }

    private static boolean atGoal(Maze m, int r, int c) {
        return r == m.getGoalPosition().getRowIndex()
                && c == m.getGoalPosition().getColumnIndex();
    }

    /** Thrown from the listener to unwind the search loop when cancelled. */
    private static final class VisualizationCancelledException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    // ─── Properties (for binding) ────────────────────────────────────

    public ObjectProperty<Maze> mazeProperty() { return maze; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public IntegerProperty playerRowProperty() { return playerRow; }
    public IntegerProperty playerColumnProperty() { return playerColumn; }
    public BooleanProperty solvedProperty() { return solved; }
    public ReadOnlyBooleanProperty victoryProperty() { return victory.getReadOnlyProperty(); }
    public ReadOnlyIntegerProperty nodesEvaluatedProperty() { return nodesEvaluated.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty visualizingProperty() { return visualizing.getReadOnlyProperty(); }
    public ObservableSet<AState> getVisitedStates() { return visitedStates; }
}

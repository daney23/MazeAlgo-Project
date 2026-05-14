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
import mazealgo.model.algorithms.maze3D.Maze3D;
import mazealgo.model.algorithms.maze3D.MyMaze3DGenerator;
import mazealgo.model.algorithms.maze3D.SearchableMaze3D;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.BreadthFirstSearch;
import mazealgo.model.algorithms.search.DepthFirstSearch;
import mazealgo.model.algorithms.search.ISearchable;
import mazealgo.model.algorithms.search.ISearchingAlgorithm;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge between {@link MazeModel} and the JavaFX View. Holds the full
 * UI-observable state (2D and 3D maze, current solution, player row /
 * column, victory flag, nodes-evaluated counter, visited states,
 * dimension mode, algorithm choice) and exposes commands (generate,
 * move, solve, visualize) that the controller calls in response to
 * user input.
 *
 * <p>Two dimensions:
 * <ul>
 *   <li><b>2D mode</b> — full gameplay; player navigates with WASD /
 *       arrows; Generate / Solution Hint can go through the embedded
 *       server when Best-First is selected (preserves the Phase 2
 *       cache demo).</li>
 *   <li><b>3D mode</b> — visualization only (no player movement); the
 *       3D displayer renders every layer side-by-side. Generate / Solve
 *       / Watch run in-process via {@link SearchableMaze3D} (the server
 *       protocol is 2D only).</li>
 * </ul>
 *
 * <p>Three algorithm choices:
 * <ul>
 *   <li>{@link #ALGO_BEST_FIRST} (default) — A*-like with admissible
 *       heuristics; returns optimal path.</li>
 *   <li>{@link #ALGO_BFS} — uninformed but optimal in number of edges.</li>
 *   <li>{@link #ALGO_DFS} — finds <i>some</i> path, not the shortest.</li>
 * </ul>
 */
public class MazeViewModel {
    public enum Mode { TWO_D, THREE_D }

    public static final String ALGO_BFS = "BFS";
    public static final String ALGO_DFS = "DFS";
    public static final String ALGO_BEST_FIRST = "Best-First";

    private final MazeModel model;

    // Mode.
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.TWO_D);

    // 2D state (player gameplay).
    private final ObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    private final IntegerProperty playerRow = new SimpleIntegerProperty();
    private final IntegerProperty playerColumn = new SimpleIntegerProperty();
    private final BooleanProperty solved = new SimpleBooleanProperty(false);
    private final ReadOnlyBooleanWrapper victory = new ReadOnlyBooleanWrapper(false);

    // 3D state (visualization only — no player movement in 3D mode).
    private final ObjectProperty<Maze3D> maze3D = new SimpleObjectProperty<>();

    // Shared across both modes.
    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final ReadOnlyIntegerWrapper nodesEvaluated = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyBooleanWrapper visualizing = new ReadOnlyBooleanWrapper(false);
    private final ObservableSet<AState> visitedStates = FXCollections.observableSet(new HashSet<>());

    // Algorithm choice.
    private final ObjectProperty<String> algorithmChoice = new SimpleObjectProperty<>(ALGO_BEST_FIRST);

    private final AtomicBoolean cancelVisualization = new AtomicBoolean(false);

    public MazeViewModel(MazeModel model) {
        this.model = model;
    }

    // ─── 2D commands ─────────────────────────────────────────────────

    public void generate(int rows, int columns) {
        cancelVisualization.set(true);
        Maze m = model.generate(rows, columns);
        mode.set(Mode.TWO_D);
        maze.set(m);
        maze3D.set(null);
        playerRow.set(m.getStartPosition().getRowIndex());
        playerColumn.set(m.getStartPosition().getColumnIndex());
        resetSolveState();
    }

    /**
     * Attempts to move the player one cell in the given direction. No-op
     * if the target cell is out of bounds, a wall, or — for diagonals —
     * if both adjacent orthogonal cells around the corner are walls
     * (the same pinhole rule {@link SearchableMaze} enforces).
     *
     * <p>3D mode is visualization-only; movePlayer is a no-op there.
     */
    public void movePlayer(MovementDirection dir) {
        if (mode.get() != Mode.TWO_D) return;

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

    // ─── 3D commands ─────────────────────────────────────────────────

    public void generate3D(int depth, int rows, int columns) {
        cancelVisualization.set(true);
        Maze3D m3 = new MyMaze3DGenerator().generate(depth, rows, columns);
        mode.set(Mode.THREE_D);
        maze3D.set(m3);
        maze.set(null);
        resetSolveState();
    }

    // ─── Shared commands (dispatch on mode) ──────────────────────────

    /**
     * Solves synchronously and shows the path. Used by the Solution
     * Hint button.
     *
     * <p>In 2D with Best-First, routes through {@link MazeModel#solve(Maze)} —
     * which in the running app means a socket roundtrip through the
     * embedded SolveMazeStrategy + SHA-256 cache. Other algorithm
     * choices and all 3D solves run in-process (the server protocol
     * is 2D + Best-First only).
     */
    public void solveCurrent() {
        ISearchable domain = currentDomain();
        if (domain == null) return;

        Solution sol;
        if (mode.get() == Mode.TWO_D && ALGO_BEST_FIRST.equals(algorithmChoice.get())) {
            sol = model.solve(maze.get());
        } else {
            ISearchingAlgorithm algo = createAlgorithm();
            sol = algo.solve(domain);
        }
        solution.set(sol);
        nodesEvaluated.set(sol.getNodesEvaluated());
        solved.set(true);
    }

    /**
     * Runs the selected algorithm on a daemon thread and feeds each
     * visited state into {@link #visitedStates} on the JavaFX thread,
     * with an adaptive per-cell delay so the animation always fits a
     * bounded budget regardless of maze size.
     */
    public void visualizeSearchAsync() {
        ISearchable domain = currentDomain();
        if (domain == null || visualizing.get()) return;

        cancelVisualization.set(false);
        visualizing.set(true);
        solution.set(null);
        visitedStates.clear();
        nodesEvaluated.set(0);

        int totalCells = currentTotalCells();
        long perCellDelayMs = Math.max(1, Math.min(25, 3000L / Math.max(1, totalCells)));

        Thread t = new Thread(() -> {
            ISearchingAlgorithm searcher = createAlgorithm();
            if (searcher instanceof mazealgo.model.algorithms.search.ASearchingAlgorithm asa) {
                asa.setNodeEvaluatedListener(state -> {
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
            }
            Solution sol;
            try {
                sol = searcher.solve(domain);
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

    private ISearchable currentDomain() {
        if (mode.get() == Mode.TWO_D) {
            Maze m = maze.get();
            return m == null ? null : new SearchableMaze(m);
        }
        Maze3D m3 = maze3D.get();
        return m3 == null ? null : new SearchableMaze3D(m3);
    }

    private int currentTotalCells() {
        if (mode.get() == Mode.TWO_D) {
            Maze m = maze.get();
            return m == null ? 0 : m.getRows() * m.getColumns();
        }
        Maze3D m3 = maze3D.get();
        return m3 == null ? 0 : m3.getDepth() * m3.getRows() * m3.getColumns();
    }

    private ISearchingAlgorithm createAlgorithm() {
        return switch (algorithmChoice.get()) {
            case ALGO_BFS -> new BreadthFirstSearch();
            case ALGO_DFS -> new DepthFirstSearch();
            default -> new BestFirstSearch();
        };
    }

    private void resetSolveState() {
        solution.set(null);
        solved.set(false);
        victory.set(false);
        nodesEvaluated.set(0);
        visitedStates.clear();
    }

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

    public ObjectProperty<Mode> modeProperty() { return mode; }
    public ObjectProperty<Maze> mazeProperty() { return maze; }
    public ObjectProperty<Maze3D> maze3DProperty() { return maze3D; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public IntegerProperty playerRowProperty() { return playerRow; }
    public IntegerProperty playerColumnProperty() { return playerColumn; }
    public BooleanProperty solvedProperty() { return solved; }
    public ReadOnlyBooleanProperty victoryProperty() { return victory.getReadOnlyProperty(); }
    public ReadOnlyIntegerProperty nodesEvaluatedProperty() { return nodesEvaluated.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty visualizingProperty() { return visualizing.getReadOnlyProperty(); }
    public ObservableSet<AState> getVisitedStates() { return visitedStates; }
    public ObjectProperty<String> algorithmChoiceProperty() { return algorithmChoice; }
}

package mazealgo.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import mazealgo.model.MazeModel;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.search.Solution;

/**
 * Bridge between {@link MazeModel} and the JavaFX View. Holds observable
 * properties the view binds to (current maze, current solution, player
 * row / column, "you won" flag) and exposes commands (generate, move,
 * solve) that the controller calls in response to user input.
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

    // ReadOnly because the controller should only observe victory, not poke it.
    private final ReadOnlyBooleanWrapper victory = new ReadOnlyBooleanWrapper(false);

    public MazeViewModel(MazeModel model) {
        this.model = model;
    }

    // ─── Commands ────────────────────────────────────────────────────

    public void generate(int rows, int columns) {
        Maze m = model.generate(rows, columns);
        maze.set(m);
        playerRow.set(m.getStartPosition().getRowIndex());
        playerColumn.set(m.getStartPosition().getColumnIndex());
        solution.set(null);
        solved.set(false);
        victory.set(false);
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

    public void solveCurrent() {
        Maze m = maze.get();
        if (m == null) return;
        solution.set(model.solve(m));
        solved.set(true);
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

    // ─── Properties (for binding) ────────────────────────────────────

    public ObjectProperty<Maze> mazeProperty() { return maze; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public IntegerProperty playerRowProperty() { return playerRow; }
    public IntegerProperty playerColumnProperty() { return playerColumn; }
    public BooleanProperty solvedProperty() { return solved; }
    public ReadOnlyBooleanProperty victoryProperty() { return victory.getReadOnlyProperty(); }
}

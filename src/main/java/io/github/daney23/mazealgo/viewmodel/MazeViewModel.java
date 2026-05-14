package io.github.daney23.mazealgo.viewmodel;

import io.github.daney23.mazealgo.model.MazeModel;
import io.github.daney23.mazealgo.model.algorithms.mazeGenerators.Maze;
import io.github.daney23.mazealgo.model.algorithms.search.Solution;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Bridge between {@link MazeModel} and the JavaFX View. Holds observable
 * properties (current maze, current solution, player row/column) that the
 * view binds to. The view never touches the model directly - it only
 * reads/writes these properties and calls commands on this view-model.
 */
public class MazeViewModel {
    private final MazeModel model;

    private final ObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final IntegerProperty playerRow = new SimpleIntegerProperty();
    private final IntegerProperty playerColumn = new SimpleIntegerProperty();

    public MazeViewModel(MazeModel model) {
        this.model = model;
    }

    public void generate(int rows, int columns) {
        Maze m = model.generate(rows, columns);
        maze.set(m);
        playerRow.set(m.getStartPosition().getRowIndex());
        playerColumn.set(m.getStartPosition().getColumnIndex());
        solution.set(null);
    }

    public void solveCurrent() {
        Maze m = maze.get();
        if (m != null) {
            solution.set(model.solve(m));
        }
    }

    public ObjectProperty<Maze> mazeProperty() { return maze; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public IntegerProperty playerRowProperty() { return playerRow; }
    public IntegerProperty playerColumnProperty() { return playerColumn; }
}

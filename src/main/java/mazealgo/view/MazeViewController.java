package mazealgo.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import mazealgo.model.MazeModel;
import mazealgo.model.algorithms.search.AState;
import mazealgo.viewmodel.MazeViewModel;
import mazealgo.viewmodel.MovementDirection;

/**
 * Controller for {@code MazeView.fxml}. Owns the {@link MazeViewModel}
 * and wires it to the FXML-injected nodes: spinners for dimensions,
 * the {@link MazeDisplayer} canvas, the Generate / Solution Hint /
 * Watch Search buttons, and the status / nodes / zoom labels.
 *
 * <p>Input handling lives here:
 * <ul>
 *   <li>WASD or arrow keys → {@code movePlayer} in the four cardinal directions</li>
 *   <li>Ctrl + scroll wheel anywhere on the canvas → zoom in/out</li>
 *   <li>Generate → new maze with the spinner-selected dimensions</li>
 *   <li>Solution Hint → run Best-First synchronously, draw the path</li>
 *   <li>Watch Search → run Best-First async, paint cells as they're visited</li>
 * </ul>
 *
 * <p>The controller listens to the ViewModel's {@code victoryProperty}
 * and triggers the chime — keeping the ViewModel free of UI / audio
 * dependencies.
 */
public class MazeViewController {

    private static final double MIN_ZOOM = 0.4;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 1.1;

    private final MazeViewModel viewModel = new MazeViewModel(new MazeModel());
    private final SoundPlayer soundPlayer = new SoundPlayer();

    @FXML private BorderPane root;
    @FXML private Pane mazeContainer;
    @FXML private MazeDisplayer mazeDisplayer;
    @FXML private Spinner<Integer> rowsSpinner;
    @FXML private Spinner<Integer> colsSpinner;
    @FXML private Button generateButton;
    @FXML private Button hintButton;
    @FXML private Button watchButton;
    @FXML private Label statusLabel;
    @FXML private Label nodesLabel;
    @FXML private Label zoomLabel;

    @FXML
    private void initialize() {
        // Resize the canvas with its parent pane. Canvas isn't auto-resizable
        // in a Pane, so bind manually — the displayer's listeners pick it up
        // and redraw.
        mazeDisplayer.widthProperty().bind(mazeContainer.widthProperty());
        mazeDisplayer.heightProperty().bind(mazeContainer.heightProperty());

        // Spinner factories: 5..200 with sensible defaults.
        rowsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 200, 20));
        colsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 200, 20));

        // Bind displayer to ViewModel — read-only direction for maze/player/solution.
        mazeDisplayer.mazeProperty().bind(viewModel.mazeProperty());
        mazeDisplayer.playerRowProperty().bind(viewModel.playerRowProperty());
        mazeDisplayer.playerColumnProperty().bind(viewModel.playerColumnProperty());
        mazeDisplayer.solutionProperty().bind(viewModel.solutionProperty());

        // Mirror the ViewModel's visited set into the displayer (ObservableSet
        // has no built-in bindContent, so route changes via a listener).
        viewModel.getVisitedStates().addListener((SetChangeListener<AState>) change -> {
            if (change.wasAdded())   mazeDisplayer.getVisitedCells().add(change.getElementAdded());
            if (change.wasRemoved()) mazeDisplayer.getVisitedCells().remove(change.getElementRemoved());
        });

        // Labels.
        zoomLabel.textProperty().bind(Bindings.format("Zoom %.0f%%",
                mazeDisplayer.zoomProperty().multiply(100)));
        nodesLabel.textProperty().bind(Bindings.format("Nodes: %d",
                viewModel.nodesEvaluatedProperty()));

        // Disable Watch Search while a previous visualization is in flight.
        watchButton.disableProperty().bind(viewModel.visualizingProperty());

        // Victory chime on transition to true.
        viewModel.victoryProperty().addListener((obs, was, now) -> {
            if (now && !was) {
                statusLabel.setText("Victory! Hit Generate for a new maze.");
                statusLabel.getStyleClass().add("victory-label");
                soundPlayer.playVictory();
            } else if (!now) {
                statusLabel.getStyleClass().remove("victory-label");
            }
        });

        // The root pane needs keyboard focus to receive key events. Request
        // it once the scene is attached; before that, the root has no scene
        // and focus requests are no-ops.
        Platform.runLater(() -> {
            if (root.getScene() != null) {
                root.setFocusTraversable(true);
                root.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                root.addEventFilter(ScrollEvent.SCROLL, this::onScroll);
                root.requestFocus();
            }
        });

        soundPlayer.playBackground();
    }

    @FXML
    private void onGenerate() {
        int rows = rowsSpinner.getValue();
        int cols = colsSpinner.getValue();
        viewModel.generate(rows, cols);
        mazeDisplayer.getVisitedCells().clear();
        statusLabel.setText(String.format("Generated %d×%d. WASD or Arrow keys to move.", rows, cols));
        root.requestFocus();
    }

    @FXML
    private void onSolutionHint() {
        viewModel.solveCurrent();
        statusLabel.setText(String.format("Best-First found a path in %d nodes.",
                viewModel.nodesEvaluatedProperty().get()));
        root.requestFocus();
    }

    @FXML
    private void onWatchSearch() {
        mazeDisplayer.getVisitedCells().clear();
        statusLabel.setText("Watching Best-First search…");
        viewModel.visualizeSearchAsync();
        root.requestFocus();
    }

    private void onKeyPressed(KeyEvent e) {
        MovementDirection dir = directionForKey(e.getCode());
        if (dir != null) {
            viewModel.movePlayer(dir);
            e.consume();
        }
    }

    private void onScroll(ScrollEvent e) {
        if (!e.isControlDown()) return;
        double current = mazeDisplayer.zoomProperty().get();
        double next = e.getDeltaY() > 0 ? current * ZOOM_STEP : current / ZOOM_STEP;
        next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, next));
        mazeDisplayer.zoomProperty().set(next);
        e.consume();
    }

    /**
     * WASD and arrow keys both drive the four cardinal directions.
     * Diagonals are intentionally unmapped — the diagonal pinhole rule
     * still governs the search algorithm's edges; players move
     * orthogonally and the algorithm decides if a diagonal step would
     * have been legal on the optimal path.
     */
    private static MovementDirection directionForKey(KeyCode code) {
        return switch (code) {
            case W, UP    -> MovementDirection.UP;
            case S, DOWN  -> MovementDirection.DOWN;
            case A, LEFT  -> MovementDirection.LEFT;
            case D, RIGHT -> MovementDirection.RIGHT;
            default       -> null;
        };
    }
}

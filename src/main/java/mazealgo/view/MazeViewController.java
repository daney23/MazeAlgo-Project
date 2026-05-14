package mazealgo.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import mazealgo.model.MazeModel;
import mazealgo.model.algorithms.search.AState;
import mazealgo.viewmodel.MazeViewModel;
import mazealgo.viewmodel.MovementDirection;

/**
 * Controller for {@code MazeView.fxml}. Owns the {@link MazeViewModel}
 * and wires it to the FXML-injected nodes.
 *
 * <p>Input handling lives here:
 * <ul>
 *   <li>2D/3D toggle → switches displayers, shows/hides the Depth spinner</li>
 *   <li>WASD or arrow keys → {@code movePlayer} in the four cardinal directions (2D only)</li>
 *   <li>Scroll wheel anywhere on the canvas → zoom in/out</li>
 *   <li>Primary-button drag on the canvas → pan; right-click recenters</li>
 *   <li>Generate → new 2D maze, or new 3D maze when in 3D mode</li>
 *   <li>Algorithm ComboBox → BFS / DFS / Best-First; affects both Hint and Watch</li>
 *   <li>Solution Hint → solves and draws the path</li>
 *   <li>Watch Search → animates the chosen algorithm visiting cells</li>
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

    private final MazeViewModel viewModel;
    private final SoundPlayer soundPlayer = new SoundPlayer();

    // Drag-to-pan: where the mouse was pressed (scene coords) and the pan
    // offset at that moment, so dragging is a pure delta against the
    // initial press — no accumulated rounding error from per-event deltas.
    private double dragStartX;
    private double dragStartY;
    private double panStartX;
    private double panStartY;
    private boolean panning;

    /** Default ctor — used when FXMLLoader has no controller factory. In-process model. */
    public MazeViewController() {
        this(new MazeModel());
    }

    /**
     * Used by {@link mazealgo.MazeApp} via FXMLLoader.setControllerFactory to
     * inject the server-wired model so Generate / Solution Hint go over sockets.
     */
    public MazeViewController(MazeModel model) {
        this.viewModel = new MazeViewModel(model);
    }

    @FXML private BorderPane root;
    @FXML private StackPane mazeContainer;
    @FXML private Pane maze2DContainer;
    @FXML private Pane maze3DContainer;
    @FXML private MazeDisplayer mazeDisplayer;
    @FXML private Maze3DDisplayer maze3DDisplayer;
    @FXML private Spinner<Integer> rowsSpinner;
    @FXML private Spinner<Integer> colsSpinner;
    @FXML private Spinner<Integer> depthSpinner;
    @FXML private Label depthLabel;
    @FXML private Button generateButton;
    @FXML private Button hintButton;
    @FXML private Button watchButton;
    @FXML private Label statusLabel;
    @FXML private Label nodesLabel;
    @FXML private Label zoomLabel;
    @FXML private ToggleGroup modeGroup;
    @FXML private ToggleButton mode2DButton;
    @FXML private ToggleButton mode3DButton;
    @FXML private ComboBox<String> algorithmCombo;

    @FXML
    private void initialize() {
        // Canvas resize bindings — each displayer fills its own Pane.
        mazeDisplayer.widthProperty().bind(maze2DContainer.widthProperty());
        mazeDisplayer.heightProperty().bind(maze2DContainer.heightProperty());
        maze3DDisplayer.widthProperty().bind(maze3DContainer.widthProperty());
        maze3DDisplayer.heightProperty().bind(maze3DContainer.heightProperty());

        // Spinner factories.
        rowsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 200, 20));
        colsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 200, 20));
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 4));

        // 2D displayer bindings.
        mazeDisplayer.mazeProperty().bind(viewModel.mazeProperty());
        mazeDisplayer.playerRowProperty().bind(viewModel.playerRowProperty());
        mazeDisplayer.playerColumnProperty().bind(viewModel.playerColumnProperty());
        mazeDisplayer.solutionProperty().bind(viewModel.solutionProperty());

        // 3D displayer bindings.
        maze3DDisplayer.maze3DProperty().bind(viewModel.maze3DProperty());
        maze3DDisplayer.solutionProperty().bind(viewModel.solutionProperty());

        // Mirror visited set into both displayers — each ignores states of the
        // wrong type, so they coexist with no extra filtering needed here.
        viewModel.getVisitedStates().addListener((SetChangeListener<AState>) change -> {
            if (change.wasAdded()) {
                mazeDisplayer.getVisitedCells().add(change.getElementAdded());
                maze3DDisplayer.getVisitedCells().add(change.getElementAdded());
            }
            if (change.wasRemoved()) {
                mazeDisplayer.getVisitedCells().remove(change.getElementRemoved());
                maze3DDisplayer.getVisitedCells().remove(change.getElementRemoved());
            }
        });

        // Mode wiring: toggle buttons drive a shared ViewModel state.
        mode2DButton.setOnAction(e -> setMode(MazeViewModel.Mode.TWO_D));
        mode3DButton.setOnAction(e -> setMode(MazeViewModel.Mode.THREE_D));
        viewModel.modeProperty().addListener((obs, oldMode, newMode) -> applyMode(newMode));
        applyMode(viewModel.modeProperty().get());

        // Algorithm combo.
        algorithmCombo.setItems(FXCollections.observableArrayList(
                MazeViewModel.ALGO_BEST_FIRST, MazeViewModel.ALGO_BFS, MazeViewModel.ALGO_DFS));
        algorithmCombo.valueProperty().bindBidirectional(viewModel.algorithmChoiceProperty());

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

        // Keyboard + scroll once the scene is attached.
        Platform.runLater(() -> {
            if (root.getScene() != null) {
                root.setFocusTraversable(true);
                root.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                root.addEventFilter(ScrollEvent.SCROLL, this::onScroll);
                root.requestFocus();
            }
        });

        // Drag-to-pan on either displayer. Wired on the canvases (not the
        // root) so dragging on the toolbar doesn't move the maze.
        installPanHandlers(mazeDisplayer,
                mazeDisplayer.panXProperty(), mazeDisplayer.panYProperty());
        installPanHandlers(maze3DDisplayer,
                maze3DDisplayer.panXProperty(), maze3DDisplayer.panYProperty());

        soundPlayer.playBackground();
    }

    private void setMode(MazeViewModel.Mode m) {
        if (viewModel.modeProperty().get() == m) return;
        viewModel.modeProperty().set(m);
    }

    private void applyMode(MazeViewModel.Mode m) {
        boolean threeD = m == MazeViewModel.Mode.THREE_D;
        mode2DButton.setSelected(!threeD);
        mode3DButton.setSelected(threeD);
        maze2DContainer.setVisible(!threeD);
        maze3DContainer.setVisible(threeD);
        depthLabel.setVisible(threeD);
        depthSpinner.setVisible(threeD);
        depthLabel.setManaged(threeD);
        depthSpinner.setManaged(threeD);
        statusLabel.setText(threeD
                ? "3D mode — Generate makes a Maze3D; player movement is disabled. Use Hint / Watch."
                : "Hit Generate to make a maze, WASD/Arrows to move, scroll to zoom, click-drag to pan (right-click recenters).");
    }

    @FXML
    private void onGenerate() {
        int rows = rowsSpinner.getValue();
        int cols = colsSpinner.getValue();
        mazeDisplayer.getVisitedCells().clear();
        maze3DDisplayer.getVisitedCells().clear();
        // Center the new maze — old pan offset would put it off-screen if the
        // previous drag was aggressive or the previous maze was much bigger.
        mazeDisplayer.resetPan();
        maze3DDisplayer.resetPan();
        if (viewModel.modeProperty().get() == MazeViewModel.Mode.THREE_D) {
            int depth = depthSpinner.getValue();
            viewModel.generate3D(depth, rows, cols);
            statusLabel.setText(String.format("Generated 3D %d×%d×%d (depth×rows×cols).",
                    depth, rows, cols));
        } else {
            viewModel.generate(rows, cols);
            statusLabel.setText(String.format("Generated %d×%d. WASD or Arrow keys to move.", rows, cols));
        }
        root.requestFocus();
    }

    @FXML
    private void onSolutionHint() {
        viewModel.solveCurrent();
        statusLabel.setText(String.format("%s found a path in %d nodes.",
                viewModel.algorithmChoiceProperty().get(),
                viewModel.nodesEvaluatedProperty().get()));
        root.requestFocus();
    }

    @FXML
    private void onWatchSearch() {
        mazeDisplayer.getVisitedCells().clear();
        maze3DDisplayer.getVisitedCells().clear();
        statusLabel.setText(String.format("Watching %s search…",
                viewModel.algorithmChoiceProperty().get()));
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

    /**
     * Wire mouse press/drag/release on the given canvas so a primary-button
     * drag pans the rendered maze. Right-click resets the pan to centered.
     */
    private void installPanHandlers(Canvas canvas,
                                    javafx.beans.property.DoubleProperty panX,
                                    javafx.beans.property.DoubleProperty panY) {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                // Right-click recentres — quick escape if the user drags too far.
                panX.set(0);
                panY.set(0);
                e.consume();
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) return;
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
            panStartX = panX.get();
            panStartY = panY.get();
            panning = true;
            canvas.setCursor(Cursor.CLOSED_HAND);
            e.consume();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!panning) return;
            panX.set(panStartX + (e.getSceneX() - dragStartX));
            panY.set(panStartY + (e.getSceneY() - dragStartY));
            e.consume();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (!panning) return;
            panning = false;
            canvas.setCursor(Cursor.OPEN_HAND);
            // Movement keys (WASD/arrows) need root focus — restore it.
            root.requestFocus();
            e.consume();
        });
        canvas.setCursor(Cursor.OPEN_HAND);
    }

    private void onScroll(ScrollEvent e) {
        // Zoom whichever displayer is active.
        boolean threeD = viewModel.modeProperty().get() == MazeViewModel.Mode.THREE_D;
        double current = threeD ? maze3DDisplayer.zoomProperty().get() : mazeDisplayer.zoomProperty().get();
        double next = e.getDeltaY() > 0 ? current * ZOOM_STEP : current / ZOOM_STEP;
        next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, next));
        if (threeD) maze3DDisplayer.zoomProperty().set(next);
        else mazeDisplayer.zoomProperty().set(next);
        e.consume();
    }

    /**
     * WASD and arrow keys drive the four cardinal directions in 2D mode.
     * 3D mode is visualization-only, so the ViewModel's movePlayer is a
     * no-op there — the event filter still fires but does nothing.
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

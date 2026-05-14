package mazealgo.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import mazealgo.viewmodel.MazeViewModel;
import mazealgo.viewmodel.MovementDirection;

/**
 * Controller for {@code MazeView.fxml}. Owns the {@link MazeViewModel}
 * and wires it to the FXML-injected nodes: spinners for dimensions,
 * the {@link MazeDisplayer} canvas, the Generate button, and the
 * status / zoom labels.
 *
 * <p>Input handling lives here:
 * <ul>
 *   <li>NumPad 1-9 (and the same digits on the main row) → {@code movePlayer}</li>
 *   <li>Ctrl + scroll wheel anywhere on the canvas → zoom in/out</li>
 *   <li>Generate button → new maze with the spinner-selected dimensions</li>
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
    @FXML private Label statusLabel;
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

        // Bind displayer to ViewModel — read-only direction.
        mazeDisplayer.mazeProperty().bind(viewModel.mazeProperty());
        mazeDisplayer.playerRowProperty().bind(viewModel.playerRowProperty());
        mazeDisplayer.playerColumnProperty().bind(viewModel.playerColumnProperty());

        // Zoom label tracks the displayer's current zoom factor.
        zoomLabel.textProperty().bind(Bindings.format("Zoom %.0f%%",
                mazeDisplayer.zoomProperty().multiply(100)));

        // Victory chime on transition to true.
        viewModel.victoryProperty().addListener((obs, was, now) -> {
            if (now && !was) {
                statusLabel.setText("Victory! NumPad 1-9 to move, or Generate for a new maze.");
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
        statusLabel.setText(String.format("Generated %d×%d. NumPad 1-9 to move.", rows, cols));
        root.requestFocus();
    }

    private void onKeyPressed(KeyEvent e) {
        Integer digit = digitFromKey(e.getCode());
        if (digit == null) return;
        MovementDirection dir = MovementDirection.forNumpadDigit(digit);
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

    private static Integer digitFromKey(KeyCode code) {
        // Accept both the NumPad keys and the main-row digits so laptops without
        // a NumPad (or with NumLock off) still work.
        return switch (code) {
            case NUMPAD1, DIGIT1 -> 1;
            case NUMPAD2, DIGIT2 -> 2;
            case NUMPAD3, DIGIT3 -> 3;
            case NUMPAD4, DIGIT4 -> 4;
            case NUMPAD6, DIGIT6 -> 6;
            case NUMPAD7, DIGIT7 -> 7;
            case NUMPAD8, DIGIT8 -> 8;
            case NUMPAD9, DIGIT9 -> 9;
            default -> null;
        };
    }
}

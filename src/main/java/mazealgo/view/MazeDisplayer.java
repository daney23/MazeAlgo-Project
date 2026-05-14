package mazealgo.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.Position;

import java.net.URL;

/**
 * A resizable JavaFX {@link Canvas} that renders a {@link Maze}, the
 * player, and the goal. Exposes JavaFX properties for everything that
 * changes at runtime so the controller can bind it to the ViewModel —
 * setting any property triggers a redraw.
 *
 * <p>Sprite handling: if {@code /mazealgo/view/images/wall.png},
 * {@code /player.png} and {@code /goal.png} are on the classpath
 * they're used; otherwise the displayer falls back to colored shapes
 * (dark slate walls, blue player circle, green flag-style goal). The
 * fallback ships with the repo so the UI works out of the box without
 * sourcing third-party assets.
 *
 * <p>Zoom is a multiplicative factor; the actual cell size also
 * depends on the canvas dimensions. The displayer always preserves
 * the maze's aspect ratio and centers the drawing on the canvas.
 */
public class MazeDisplayer extends Canvas {
    private static final Color FLOOR_COLOR = Color.web("#ecf0f1");
    private static final Color WALL_COLOR = Color.web("#2c3e50");
    private static final Color GRID_COLOR = Color.web("#bdc3c7");
    private static final Color PLAYER_COLOR = Color.web("#2980b9");
    private static final Color PLAYER_OUTLINE = Color.web("#ffffff");
    private static final Color GOAL_COLOR = Color.web("#27ae60");
    private static final Color GOAL_OUTLINE = Color.web("#145a32");

    private final ObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    private final IntegerProperty playerRow = new SimpleIntegerProperty(0);
    private final IntegerProperty playerColumn = new SimpleIntegerProperty(0);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    private Image wallImage;
    private Image playerImage;
    private Image goalImage;

    public MazeDisplayer() {
        loadOptionalSprites();

        // Redraw on any visual change.
        maze.addListener((o, oldV, newV) -> redraw());
        playerRow.addListener((o, oldV, newV) -> redraw());
        playerColumn.addListener((o, oldV, newV) -> redraw());
        zoom.addListener((o, oldV, newV) -> redraw());
        widthProperty().addListener((o, oldV, newV) -> redraw());
        heightProperty().addListener((o, oldV, newV) -> redraw());
    }

    private void loadOptionalSprites() {
        wallImage = tryLoad("/mazealgo/view/images/wall.png");
        playerImage = tryLoad("/mazealgo/view/images/player.png");
        goalImage = tryLoad("/mazealgo/view/images/goal.png");
    }

    private Image tryLoad(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        return url != null ? new Image(url.toExternalForm()) : null;
    }

    @Override public boolean isResizable() { return true; }
    @Override public double prefWidth(double height) { return getWidth(); }
    @Override public double prefHeight(double width) { return getHeight(); }

    private void redraw() {
        Maze m = maze.get();
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        if (m == null || getWidth() <= 0 || getHeight() <= 0) return;

        double cellSize = computeCellSize(m);
        double drawWidth = cellSize * m.getColumns();
        double drawHeight = cellSize * m.getRows();
        double xOff = (getWidth() - drawWidth) / 2;
        double yOff = (getHeight() - drawHeight) / 2;

        // Floor background — one rect avoids per-cell fills.
        gc.setFill(FLOOR_COLOR);
        gc.fillRect(xOff, yOff, drawWidth, drawHeight);

        // Walls.
        for (int r = 0; r < m.getRows(); r++) {
            for (int c = 0; c < m.getColumns(); c++) {
                if (m.getCell(r, c) == 1) {
                    drawWall(gc, r, c, cellSize, xOff, yOff);
                }
            }
        }

        // Light grid lines (only when cells are large enough to see them).
        if (cellSize > 6) {
            gc.setStroke(GRID_COLOR);
            gc.setLineWidth(0.5);
            for (int r = 0; r <= m.getRows(); r++) {
                double y = yOff + r * cellSize;
                gc.strokeLine(xOff, y, xOff + drawWidth, y);
            }
            for (int c = 0; c <= m.getColumns(); c++) {
                double x = xOff + c * cellSize;
                gc.strokeLine(x, yOff, x, yOff + drawHeight);
            }
        }

        drawGoal(gc, m.getGoalPosition(), cellSize, xOff, yOff);
        drawPlayer(gc, playerRow.get(), playerColumn.get(), cellSize, xOff, yOff);
    }

    private double computeCellSize(Maze m) {
        double widthCell = getWidth() / m.getColumns();
        double heightCell = getHeight() / m.getRows();
        return Math.min(widthCell, heightCell) * zoom.get();
    }

    private void drawWall(GraphicsContext gc, int r, int c, double cellSize, double xOff, double yOff) {
        double x = xOff + c * cellSize;
        double y = yOff + r * cellSize;
        if (wallImage != null) {
            gc.drawImage(wallImage, x, y, cellSize, cellSize);
        } else {
            gc.setFill(WALL_COLOR);
            gc.fillRect(x, y, cellSize, cellSize);
        }
    }

    private void drawPlayer(GraphicsContext gc, int r, int c, double cellSize, double xOff, double yOff) {
        double x = xOff + c * cellSize;
        double y = yOff + r * cellSize;
        if (playerImage != null) {
            gc.drawImage(playerImage, x, y, cellSize, cellSize);
            return;
        }
        // Fallback: a filled circle with a white outline so it's visible on both
        // light floor and dark walls (if it ever ends up overlapping one).
        double pad = cellSize * 0.15;
        gc.setFill(PLAYER_COLOR);
        gc.fillOval(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad);
        gc.setStroke(PLAYER_OUTLINE);
        gc.setLineWidth(Math.max(1, cellSize * 0.06));
        gc.strokeOval(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad);
    }

    private void drawGoal(GraphicsContext gc, Position goal, double cellSize, double xOff, double yOff) {
        if (goal == null) return;
        double x = xOff + goal.getColumnIndex() * cellSize;
        double y = yOff + goal.getRowIndex() * cellSize;
        if (goalImage != null) {
            gc.drawImage(goalImage, x, y, cellSize, cellSize);
            return;
        }
        // Fallback: a simple green flag — vertical pole + triangle pennant.
        double pad = cellSize * 0.18;
        double poleX = x + pad;
        double poleTop = y + pad;
        double poleBottom = y + cellSize - pad;
        gc.setStroke(GOAL_OUTLINE);
        gc.setLineWidth(Math.max(1.5, cellSize * 0.08));
        gc.strokeLine(poleX, poleTop, poleX, poleBottom);

        double flagW = cellSize * 0.45;
        double flagH = cellSize * 0.30;
        double[] xs = {poleX, poleX + flagW, poleX};
        double[] ys = {poleTop, poleTop + flagH / 2, poleTop + flagH};
        gc.setFill(GOAL_COLOR);
        gc.fillPolygon(xs, ys, 3);
    }

    // ─── Properties ──────────────────────────────────────────────────

    public ObjectProperty<Maze> mazeProperty() { return maze; }
    public IntegerProperty playerRowProperty() { return playerRow; }
    public IntegerProperty playerColumnProperty() { return playerColumn; }
    public DoubleProperty zoomProperty() { return zoom; }
}

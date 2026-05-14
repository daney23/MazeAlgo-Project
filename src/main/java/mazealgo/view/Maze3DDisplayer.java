package mazealgo.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mazealgo.model.algorithms.maze3D.Maze3D;
import mazealgo.model.algorithms.maze3D.Maze3DState;
import mazealgo.model.algorithms.maze3D.Position3D;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.Solution;

import java.util.HashSet;

/**
 * Renders a {@link Maze3D} as a horizontal strip of layer views — one
 * 2D grid per Z-layer, side by side, labelled "L0" / "L1" / ... above.
 * Visualization-only: no player avatar; the start and goal are drawn
 * with small markers in their respective layers, and the search
 * solution / visited cells are split across layers by their depth.
 *
 * <p>Same property surface as the 2D {@link MazeDisplayer} but keyed on
 * {@link Maze3DState} instead of {@code MazeState}. Setting any of
 * {@code maze3D / solution / visitedCells / zoom} triggers a redraw.
 */
public class Maze3DDisplayer extends Canvas {
    private static final Color FLOOR_COLOR = Color.web("#ecf0f1");
    private static final Color WALL_COLOR = Color.web("#2c3e50");
    private static final Color GRID_COLOR = Color.web("#bdc3c7");
    private static final Color START_COLOR = Color.web("#2980b9");
    private static final Color GOAL_COLOR = Color.web("#27ae60");
    private static final Color GOAL_OUTLINE = Color.web("#145a32");
    private static final Color VISITED_COLOR = Color.web("#f1c40f", 0.35);
    private static final Color SOLUTION_COLOR = Color.web("#e67e22");
    private static final Color LAYER_LABEL_COLOR = Color.web("#34495e");
    private static final double LAYER_GAP_FRACTION = 0.4; // gap between layers, fraction of cell size
    private static final double LABEL_HEIGHT_PX = 22;

    private final ObjectProperty<Maze3D> maze3D = new SimpleObjectProperty<>();
    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
    private final ObservableSet<AState> visitedCells = FXCollections.observableSet(new HashSet<>());

    public Maze3DDisplayer() {
        maze3D.addListener((o, ov, nv) -> redraw());
        solution.addListener((o, ov, nv) -> redraw());
        zoom.addListener((o, ov, nv) -> redraw());
        visitedCells.addListener((SetChangeListener<AState>) c -> redraw());
        widthProperty().addListener((o, ov, nv) -> redraw());
        heightProperty().addListener((o, ov, nv) -> redraw());
    }

    @Override public boolean isResizable() { return true; }
    @Override public double prefWidth(double height) { return getWidth(); }
    @Override public double prefHeight(double width) { return getHeight(); }

    private void redraw() {
        Maze3D m = maze3D.get();
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        if (m == null || getWidth() <= 0 || getHeight() <= 0) return;

        // Fit all layers side-by-side: cellSize is the largest that lets
        // every layer's grid + the gaps + the label-row fit inside the canvas.
        double cellSize = computeCellSize(m);
        double layerWidth = cellSize * m.getColumns();
        double layerHeight = cellSize * m.getRows();
        double gapPx = cellSize * LAYER_GAP_FRACTION;
        double totalStripWidth = m.getDepth() * layerWidth + (m.getDepth() - 1) * gapPx;
        double totalStripHeight = layerHeight + LABEL_HEIGHT_PX;
        double xOff = (getWidth() - totalStripWidth) / 2;
        double yOff = (getHeight() - totalStripHeight) / 2 + LABEL_HEIGHT_PX;

        gc.setFont(Font.font(13));
        gc.setTextAlign(TextAlignment.CENTER);

        for (int d = 0; d < m.getDepth(); d++) {
            double lx = xOff + d * (layerWidth + gapPx);
            drawLayer(gc, m, d, lx, yOff, cellSize, layerWidth, layerHeight);

            // "L0", "L1", ...
            gc.setFill(LAYER_LABEL_COLOR);
            gc.fillText("L" + d, lx + layerWidth / 2, yOff - 6);
        }

        // Cross-layer connecting lines (between consecutive solution states
        // whose layers differ) — drawn last so they sit on top.
        drawCrossLayerSolutionLinks(gc, m, xOff, yOff, cellSize, layerWidth, gapPx);
    }

    private double computeCellSize(Maze3D m) {
        double gapFactorH = LAYER_GAP_FRACTION;
        double availableW = getWidth() / (m.getDepth() * m.getColumns() + (m.getDepth() - 1) * gapFactorH);
        double availableH = (getHeight() - LABEL_HEIGHT_PX) / m.getRows();
        return Math.min(availableW, availableH) * zoom.get();
    }

    private void drawLayer(GraphicsContext gc, Maze3D m, int d, double lx, double ly,
                           double cellSize, double layerWidth, double layerHeight) {
        // Floor.
        gc.setFill(FLOOR_COLOR);
        gc.fillRect(lx, ly, layerWidth, layerHeight);

        // Walls.
        gc.setFill(WALL_COLOR);
        for (int r = 0; r < m.getRows(); r++) {
            for (int c = 0; c < m.getColumns(); c++) {
                if (m.getCell(d, r, c) == 1) {
                    gc.fillRect(lx + c * cellSize, ly + r * cellSize, cellSize, cellSize);
                }
            }
        }

        // Light grid lines when cells are visible enough.
        if (cellSize > 5) {
            gc.setStroke(GRID_COLOR);
            gc.setLineWidth(0.4);
            for (int r = 0; r <= m.getRows(); r++) {
                double y = ly + r * cellSize;
                gc.strokeLine(lx, y, lx + layerWidth, y);
            }
            for (int c = 0; c <= m.getColumns(); c++) {
                double x = lx + c * cellSize;
                gc.strokeLine(x, ly, x, ly + layerHeight);
            }
        }

        // Visited cells for this layer.
        gc.setFill(VISITED_COLOR);
        for (AState s : visitedCells) {
            if (s instanceof Maze3DState ms && ms.getDepth() == d) {
                gc.fillRect(lx + ms.getColumn() * cellSize, ly + ms.getRow() * cellSize,
                        cellSize, cellSize);
            }
        }

        // Solution dots + intra-layer connecting lines.
        Solution sol = solution.get();
        if (sol != null) {
            gc.setStroke(SOLUTION_COLOR);
            gc.setLineWidth(Math.max(1.5, cellSize * 0.15));
            var path = sol.getSolutionPath();
            for (int i = 1; i < path.size(); i++) {
                if (path.get(i - 1) instanceof Maze3DState a && path.get(i) instanceof Maze3DState b
                        && a.getDepth() == d && b.getDepth() == d) {
                    gc.strokeLine(
                            lx + a.getColumn() * cellSize + cellSize / 2,
                            ly + a.getRow() * cellSize + cellSize / 2,
                            lx + b.getColumn() * cellSize + cellSize / 2,
                            ly + b.getRow() * cellSize + cellSize / 2);
                }
            }
            double dot = Math.max(3, cellSize * 0.22);
            gc.setFill(SOLUTION_COLOR);
            for (AState s : path) {
                if (s instanceof Maze3DState ms && ms.getDepth() == d) {
                    double cx = lx + ms.getColumn() * cellSize + cellSize / 2;
                    double cy = ly + ms.getRow() * cellSize + cellSize / 2;
                    gc.fillOval(cx - dot / 2, cy - dot / 2, dot, dot);
                }
            }
        }

        // Start / Goal markers in this layer.
        Position3D start = m.getStartPosition();
        if (start != null && start.getDepthIndex() == d) {
            double pad = cellSize * 0.18;
            gc.setFill(START_COLOR);
            gc.fillOval(
                    lx + start.getColumnIndex() * cellSize + pad,
                    ly + start.getRowIndex() * cellSize + pad,
                    cellSize - 2 * pad, cellSize - 2 * pad);
        }
        Position3D goal = m.getGoalPosition();
        if (goal != null && goal.getDepthIndex() == d) {
            double pad = cellSize * 0.18;
            gc.setFill(GOAL_COLOR);
            gc.fillRect(
                    lx + goal.getColumnIndex() * cellSize + pad,
                    ly + goal.getRowIndex() * cellSize + pad,
                    cellSize - 2 * pad, cellSize - 2 * pad);
            gc.setStroke(GOAL_OUTLINE);
            gc.setLineWidth(Math.max(1, cellSize * 0.06));
            gc.strokeRect(
                    lx + goal.getColumnIndex() * cellSize + pad,
                    ly + goal.getRowIndex() * cellSize + pad,
                    cellSize - 2 * pad, cellSize - 2 * pad);
        }
    }

    private void drawCrossLayerSolutionLinks(GraphicsContext gc, Maze3D m, double xOff, double yOff,
                                              double cellSize, double layerWidth, double gapPx) {
        Solution sol = solution.get();
        if (sol == null) return;
        var path = sol.getSolutionPath();
        gc.setStroke(SOLUTION_COLOR);
        gc.setLineWidth(Math.max(1, cellSize * 0.1));
        // Dashed pattern for cross-layer hops so they're visually distinct.
        gc.setLineDashes(Math.max(3, cellSize * 0.4));
        for (int i = 1; i < path.size(); i++) {
            if (!(path.get(i - 1) instanceof Maze3DState a)) continue;
            if (!(path.get(i) instanceof Maze3DState b)) continue;
            if (a.getDepth() == b.getDepth()) continue;
            double ax = xOff + a.getDepth() * (layerWidth + gapPx)
                    + a.getColumn() * cellSize + cellSize / 2;
            double ay = yOff + a.getRow() * cellSize + cellSize / 2;
            double bx = xOff + b.getDepth() * (layerWidth + gapPx)
                    + b.getColumn() * cellSize + cellSize / 2;
            double by = yOff + b.getRow() * cellSize + cellSize / 2;
            gc.strokeLine(ax, ay, bx, by);
        }
        gc.setLineDashes(0);
    }

    public ObjectProperty<Maze3D> maze3DProperty() { return maze3D; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public DoubleProperty zoomProperty() { return zoom; }
    public ObservableSet<AState> getVisitedCells() { return visitedCells; }
}

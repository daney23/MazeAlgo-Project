package mazealgo.view;

import javafx.animation.AnimationTimer;
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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import mazealgo.model.algorithms.maze3D.Maze3D;
import mazealgo.model.algorithms.maze3D.Maze3DState;
import mazealgo.model.algorithms.maze3D.Position3D;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.Solution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Renders a {@link Maze3D} as a horizontal strip of layer panels — one
 * 2D grid per Z-layer, side by side, each with a "Layer N" header and
 * a depth-indicator dot row. Visualization-only: no player avatar; the
 * start / goal get distinctive markers (a soft-pulsing blue target and
 * a green flag), and the search solution + visited cells split across
 * layers by their depth. Cross-layer path hops are drawn as smooth
 * Bezier arcs through the gaps with arrowheads on the destination.
 *
 * <p>Newly-visited cells fade in over {@value #FADE_DURATION_MS} ms via
 * an {@link AnimationTimer}, so a Watch Search animation reads as a
 * smooth wave instead of cells popping in instantly. The timer self-
 * stops when there's no active animation to save CPU.
 *
 * <p>Same property surface as the 2D {@link MazeDisplayer} but keyed on
 * {@link Maze3DState}. Setting any of {@code maze3D / solution /
 * visitedCells / zoom / panX / panY} triggers a redraw.
 */
public class Maze3DDisplayer extends Canvas {
    private static final Color FLOOR_COLOR = Color.web("#f8f9fa");
    private static final Color LAYER_PANEL_BORDER = Color.web("#d5dbdf");
    private static final Color WALL_COLOR = Color.web("#2c3e50");
    private static final Color GRID_COLOR = Color.web("#bdc3c7");
    private static final Color START_COLOR = Color.web("#2980b9");
    private static final Color START_CORE_INNER = Color.web("#ffffff");
    private static final Color GOAL_COLOR = Color.web("#27ae60");
    private static final Color GOAL_OUTLINE = Color.web("#145a32");
    private static final Color VISITED_BASE = Color.web("#f1c40f");
    private static final Color SOLUTION_COLOR = Color.web("#e67e22");
    private static final Color CROSS_LAYER_COLOR = Color.web("#d35400");
    private static final Color LAYER_LABEL_COLOR = Color.web("#2c3e50");
    private static final Color LAYER_DOT_ACTIVE = Color.web("#3498db");
    private static final Color LAYER_DOT_INACTIVE = Color.web("#bdc3c7");

    private static final double LAYER_GAP_FRACTION = 0.55;
    private static final double LABEL_HEIGHT_PX = 36;
    private static final long FADE_DURATION_MS = 280;
    private static final long PULSE_PERIOD_MS = 1800;
    private static final long PULSE_FADE_OUT_MS = 5000;

    private final ObjectProperty<Maze3D> maze3D = new SimpleObjectProperty<>();
    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
    private final DoubleProperty panX = new SimpleDoubleProperty(0);
    private final DoubleProperty panY = new SimpleDoubleProperty(0);
    private final ObservableSet<AState> visitedCells = FXCollections.observableSet(new HashSet<>());

    // Per-cell add timestamp drives the fade-in animation.
    private final Map<AState, Long> visitedAt = new HashMap<>();
    private long mazeLoadedAt = 0;

    private final AnimationTimer animTimer = new AnimationTimer() {
        @Override public void handle(long now) {
            redraw();
            if (!hasActiveAnimation()) stop();
        }
    };

    public Maze3DDisplayer() {
        maze3D.addListener((o, ov, nv) -> {
            visitedAt.clear();
            mazeLoadedAt = System.currentTimeMillis();
            if (nv != null) animTimer.start();
            redraw();
        });
        solution.addListener((o, ov, nv) -> redraw());
        zoom.addListener((o, ov, nv) -> redraw());
        panX.addListener((o, ov, nv) -> redraw());
        panY.addListener((o, ov, nv) -> redraw());
        visitedCells.addListener((SetChangeListener<AState>) c -> {
            if (c.wasAdded()) {
                visitedAt.put(c.getElementAdded(), System.currentTimeMillis());
                animTimer.start();
            }
            if (c.wasRemoved()) {
                visitedAt.remove(c.getElementRemoved());
            }
            redraw();
        });
        widthProperty().addListener((o, ov, nv) -> redraw());
        heightProperty().addListener((o, ov, nv) -> redraw());
    }

    private boolean hasActiveAnimation() {
        long t = System.currentTimeMillis();
        for (long added : visitedAt.values()) {
            if (t - added < FADE_DURATION_MS) return true;
        }
        return maze3D.get() != null && (t - mazeLoadedAt) < PULSE_FADE_OUT_MS;
    }

    @Override public boolean isResizable() { return true; }
    @Override public double prefWidth(double height) { return getWidth(); }
    @Override public double prefHeight(double width) { return getHeight(); }

    private void redraw() {
        Maze3D m = maze3D.get();
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        if (m == null || getWidth() <= 0 || getHeight() <= 0) return;

        double cellSize = computeCellSize(m);
        double layerWidth = cellSize * m.getColumns();
        double layerHeight = cellSize * m.getRows();
        double gapPx = cellSize * LAYER_GAP_FRACTION;
        double totalStripWidth = m.getDepth() * layerWidth + (m.getDepth() - 1) * gapPx;
        double totalStripHeight = layerHeight + LABEL_HEIGHT_PX;
        double xOff = (getWidth() - totalStripWidth) / 2 + panX.get();
        double yOff = (getHeight() - totalStripHeight) / 2 + LABEL_HEIGHT_PX + panY.get();

        for (int d = 0; d < m.getDepth(); d++) {
            double lx = xOff + d * (layerWidth + gapPx);
            drawLayerHeader(gc, d, m, lx, yOff, layerWidth);
            drawLayer(gc, m, d, lx, yOff, cellSize, layerWidth, layerHeight);
        }

        // Cross-layer arcs sit on top of everything so they read clearly.
        drawCrossLayerSolutionLinks(gc, m, xOff, yOff, cellSize, layerWidth, layerHeight, gapPx);
    }

    private double computeCellSize(Maze3D m) {
        double availableW = getWidth()
                / (m.getDepth() * m.getColumns() + (m.getDepth() - 1) * LAYER_GAP_FRACTION);
        double availableH = (getHeight() - LABEL_HEIGHT_PX) / m.getRows();
        return Math.min(availableW, availableH) * zoom.get();
    }

    private void drawLayerHeader(GraphicsContext gc, int d, Maze3D m,
                                 double lx, double yOff, double layerWidth) {
        Position3D start = m.getStartPosition();
        Position3D goal = m.getGoalPosition();
        boolean isStartHere = start != null && start.getDepthIndex() == d;
        boolean isGoalHere = goal != null && goal.getDepthIndex() == d;

        String label = "Layer " + d;
        if (isStartHere && isGoalHere) label += "  • start + goal";
        else if (isStartHere)          label += "  • start";
        else if (isGoalHere)           label += "  • goal";

        gc.setFill(LAYER_LABEL_COLOR);
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, lx + layerWidth / 2, yOff - 18);

        // Depth dot row — current layer filled in blue, others gray.
        int total = m.getDepth();
        double dotSize = 5;
        double dotGap = 4;
        double totalDotsW = total * dotSize + (total - 1) * dotGap;
        double dotsX = lx + (layerWidth - totalDotsW) / 2;
        double dotsY = yOff - 11;
        for (int i = 0; i < total; i++) {
            gc.setFill(i == d ? LAYER_DOT_ACTIVE : LAYER_DOT_INACTIVE);
            gc.fillOval(dotsX + i * (dotSize + dotGap), dotsY, dotSize, dotSize);
        }
    }

    private void drawLayer(GraphicsContext gc, Maze3D m, int d,
                           double lx, double ly, double cellSize,
                           double layerWidth, double layerHeight) {
        // Layer panel — soft floor with a thin border so each layer reads
        // as its own card rather than free-floating cells.
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

        // Grid lines only when cells are big enough to read.
        if (cellSize > 5) {
            gc.setStroke(GRID_COLOR);
            gc.setLineWidth(0.4);
            for (int r = 1; r < m.getRows(); r++) {
                double y = ly + r * cellSize;
                gc.strokeLine(lx, y, lx + layerWidth, y);
            }
            for (int c = 1; c < m.getColumns(); c++) {
                double x = lx + c * cellSize;
                gc.strokeLine(x, ly, x, ly + layerHeight);
            }
        }

        // Visited cells with smooth fade-in alpha.
        long now = System.currentTimeMillis();
        for (AState s : visitedCells) {
            if (s instanceof Maze3DState ms && ms.getDepth() == d) {
                double a = fadeAlpha(s, now);
                gc.setFill(VISITED_BASE.deriveColor(0, 1, 1, 0.45 * a));
                gc.fillRect(lx + ms.getColumn() * cellSize, ly + ms.getRow() * cellSize,
                        cellSize, cellSize);
            }
        }

        // Intra-layer solution segments + per-cell dots.
        Solution sol = solution.get();
        if (sol != null) {
            gc.setStroke(SOLUTION_COLOR);
            gc.setLineWidth(Math.max(1.8, cellSize * 0.16));
            gc.setLineCap(StrokeLineCap.ROUND);
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
            gc.setLineCap(StrokeLineCap.SQUARE);
        }

        // Layer panel border on top of the floor so wall fills don't bleed
        // visually past it.
        gc.setStroke(LAYER_PANEL_BORDER);
        gc.setLineWidth(1);
        gc.strokeRect(lx, ly, layerWidth, layerHeight);

        // Start / goal sit on top so the search visualization can't cover them.
        drawStartGoal(gc, m, d, lx, ly, cellSize);
    }

    /** Ease-out fade: cells appear quickly, then settle. */
    private double fadeAlpha(AState s, long now) {
        Long added = visitedAt.get(s);
        if (added == null) return 1.0;
        long age = now - added;
        if (age >= FADE_DURATION_MS) return 1.0;
        double t = age / (double) FADE_DURATION_MS;
        return 1.0 - (1.0 - t) * (1.0 - t);
    }

    private void drawStartGoal(GraphicsContext gc, Maze3D m, int d,
                                double lx, double ly, double cellSize) {
        Position3D start = m.getStartPosition();
        if (start != null && start.getDepthIndex() == d) {
            double cx = lx + start.getColumnIndex() * cellSize + cellSize / 2;
            double cy = ly + start.getRowIndex() * cellSize + cellSize / 2;

            // Pulse the halo for the first few seconds after maze load, then
            // settle to a static target marker — keeps it findable without
            // becoming distracting during long sessions.
            long age = System.currentTimeMillis() - mazeLoadedAt;
            double pulseEnvelope = age < PULSE_FADE_OUT_MS
                    ? 1.0 - age / (double) PULSE_FADE_OUT_MS
                    : 0.0;
            double phase = (age % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
            double pulse = (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI)) * pulseEnvelope;
            double rHalo = cellSize * 0.42 * (1.0 + 0.18 * pulse);

            gc.setFill(START_COLOR.deriveColor(0, 1, 1, 0.18 + 0.22 * pulse));
            gc.fillOval(cx - rHalo, cy - rHalo, rHalo * 2, rHalo * 2);

            double rCore = cellSize * 0.30;
            gc.setFill(START_COLOR);
            gc.fillOval(cx - rCore, cy - rCore, rCore * 2, rCore * 2);

            double rInner = cellSize * 0.13;
            gc.setFill(START_CORE_INNER);
            gc.fillOval(cx - rInner, cy - rInner, rInner * 2, rInner * 2);
        }
        Position3D goal = m.getGoalPosition();
        if (goal != null && goal.getDepthIndex() == d) {
            double x = lx + goal.getColumnIndex() * cellSize;
            double y = ly + goal.getRowIndex() * cellSize;
            // Green flag — pole + triangular pennant, matching the 2D displayer.
            double pad = cellSize * 0.20;
            double poleX = x + pad;
            double poleTop = y + pad;
            double poleBottom = y + cellSize - pad;
            gc.setStroke(GOAL_OUTLINE);
            gc.setLineWidth(Math.max(1.2, cellSize * 0.07));
            gc.strokeLine(poleX, poleTop, poleX, poleBottom);

            double flagW = cellSize * 0.42;
            double flagH = cellSize * 0.28;
            double[] xs = {poleX, poleX + flagW, poleX};
            double[] ys = {poleTop, poleTop + flagH / 2, poleTop + flagH};
            gc.setFill(GOAL_COLOR);
            gc.fillPolygon(xs, ys, 3);
        }
    }

    private void drawCrossLayerSolutionLinks(GraphicsContext gc, Maze3D m,
                                              double xOff, double yOff, double cellSize,
                                              double layerWidth, double layerHeight, double gapPx) {
        Solution sol = solution.get();
        if (sol == null) return;
        var path = sol.getSolutionPath();
        gc.setStroke(CROSS_LAYER_COLOR);
        gc.setLineWidth(Math.max(1.5, cellSize * 0.13));
        gc.setLineCap(StrokeLineCap.ROUND);
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

            // Quadratic Bezier arcing above the row — visually distinct from
            // intra-layer straight segments. Control point sits midway in x
            // and lifted by ~30% of layer height, so even short hops curve
            // enough to read as a separate gesture.
            double ctlX = (ax + bx) / 2;
            double ctlY = Math.min(ay, by) - layerHeight * 0.30;
            gc.beginPath();
            gc.moveTo(ax, ay);
            gc.quadraticCurveTo(ctlX, ctlY, bx, by);
            gc.stroke();
            gc.closePath();

            drawArrowHead(gc, ctlX, ctlY, bx, by, Math.max(6, cellSize * 0.45));
        }
        gc.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * Filled triangle pointing at (bx,by) along the direction from the
     * control point (cx,cy). Sized to read at any zoom.
     */
    private void drawArrowHead(GraphicsContext gc, double cx, double cy,
                                double bx, double by, double size) {
        double angle = Math.atan2(by - cy, bx - cx);
        double spread = Math.PI / 7;
        double x1 = bx - size * Math.cos(angle - spread);
        double y1 = by - size * Math.sin(angle - spread);
        double x2 = bx - size * Math.cos(angle + spread);
        double y2 = by - size * Math.sin(angle + spread);
        gc.setFill(CROSS_LAYER_COLOR);
        gc.fillPolygon(new double[]{bx, x1, x2}, new double[]{by, y1, y2}, 3);
    }

    // ─── Properties ──────────────────────────────────────────────────

    public ObjectProperty<Maze3D> maze3DProperty() { return maze3D; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public DoubleProperty zoomProperty() { return zoom; }
    public DoubleProperty panXProperty() { return panX; }
    public DoubleProperty panYProperty() { return panY; }
    public ObservableSet<AState> getVisitedCells() { return visitedCells; }

    /** Reset pan so the next-rendered maze sits centered. */
    public void resetPan() {
        panX.set(0);
        panY.set(0);
    }
}

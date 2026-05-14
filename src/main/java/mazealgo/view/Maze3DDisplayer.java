package mazealgo.view;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import mazealgo.model.algorithms.maze3D.Maze3D;
import mazealgo.model.algorithms.maze3D.Maze3DState;
import mazealgo.model.algorithms.maze3D.Position3D;
import mazealgo.model.algorithms.search.AState;
import mazealgo.model.algorithms.search.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * True-3D maze visualizer built on a JavaFX {@link SubScene} with a
 * {@link PerspectiveCamera}. The maze is rendered as a cube of cells —
 * one Z-slice per depth layer — that the user can rotate, zoom, and
 * focus.
 *
 * <p>Interaction:
 * <ul>
 *   <li><b>Primary-button drag</b> rotates the cube (horizontal = yaw,
 *       vertical = pitch). Pitch is clamped to ±89° so the cube never
 *       flips upside down.</li>
 *   <li><b>Scroll wheel</b> zooms by translating the camera along Z.</li>
 *   <li><b>Right-click</b> resets the rotation to the default
 *       isometric pose.</li>
 *   <li>{@link #focusedLayerProperty()} isolates one layer — all other
 *       layers and the solution path are hidden. Value {@code -1} shows
 *       every layer plus the solution.</li>
 * </ul>
 *
 * <p>Visited cells fade in during Watch Search animations over
 * {@value #FADE_DURATION_MS} ms; the start marker has a soft pulse for
 * the first {@value #PULSE_FADE_OUT_MS} ms after a maze is generated.
 * An internal {@link AnimationTimer} drives both and self-stops when
 * nothing is animating.
 *
 * <p>API surface kept compatible with the prior Canvas-based displayer:
 * {@code maze3D / solution / visitedCells / zoom} keep their meaning;
 * {@code panX / panY} are repurposed as drag accumulators that feed
 * yaw / pitch (their pixel deltas convert to degrees via
 * {@value #DRAG_DEG_PER_PX} °/px), and {@link #resetPan()} returns the
 * cube to its default isometric view.
 */
public class Maze3DDisplayer extends Pane {
    private static final double CELL_SIZE = 22;
    private static final double LAYER_SPACING = 1.4;       // gap between layer floors, in cell-sizes
    private static final double DEFAULT_YAW_DEG = -28;
    private static final double DEFAULT_PITCH_DEG = -22;
    private static final double DRAG_DEG_PER_PX = 0.4;
    private static final long FADE_DURATION_MS = 280;
    private static final long PULSE_PERIOD_MS = 1800;
    private static final long PULSE_FADE_OUT_MS = 5000;

    private static final Color WALL_COLOR = Color.web("#34495e");
    private static final Color FLOOR_COLOR = Color.web("#dfe6ec");
    private static final Color VISITED_COLOR = Color.web("#f1c40f");
    private static final Color SOLUTION_COLOR = Color.web("#e67e22");
    private static final Color START_COLOR = Color.web("#2980b9");
    private static final Color START_GLOW_COLOR = Color.web("#5dade2");
    private static final Color GOAL_COLOR = Color.web("#27ae60");
    private static final Color BG_COLOR = Color.web("#eef2f7");

    private final PhongMaterial wallMaterial = phong(WALL_COLOR);
    private final PhongMaterial floorMaterial = phong(FLOOR_COLOR);
    private final PhongMaterial visitedMaterial = phong(VISITED_COLOR);
    private final PhongMaterial solutionMaterial = phong(SOLUTION_COLOR);
    private final PhongMaterial startMaterial = phong(START_COLOR);
    private final PhongMaterial startGlowMaterial = phong(START_GLOW_COLOR);
    private final PhongMaterial goalMaterial = phong(GOAL_COLOR);

    private final ObjectProperty<Maze3D> maze3D = new SimpleObjectProperty<>();
    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
    private final DoubleProperty panX = new SimpleDoubleProperty(0);
    private final DoubleProperty panY = new SimpleDoubleProperty(0);
    private final IntegerProperty focusedLayer = new SimpleIntegerProperty(-1);
    private final ObservableSet<AState> visitedCells = FXCollections.observableSet(new HashSet<>());

    private final SubScene subScene;
    private final Group sceneRoot = new Group();
    private final Group worldRoot = new Group();
    private final Group mazeRoot = new Group();
    private final Group solutionRoot = new Group();
    private final Rotate rotX = new Rotate(DEFAULT_PITCH_DEG, Rotate.X_AXIS);
    private final Rotate rotY = new Rotate(DEFAULT_YAW_DEG, Rotate.Y_AXIS);
    private final Translate cameraT = new Translate(0, 0, -600);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final List<Group> layerGroups = new ArrayList<>();
    private Sphere startGlowSphere;

    private final Map<AState, Box> visitedNodes = new HashMap<>();
    private final Map<AState, Long> visitedAt = new HashMap<>();
    private long mazeLoadedAt = 0;
    private double baseCameraDistance = 600;

    private final AnimationTimer animTimer = new AnimationTimer() {
        @Override public void handle(long now) {
            tickAnimations();
            if (!hasActiveAnimation()) stop();
        }
    };

    public Maze3DDisplayer() {
        subScene = new SubScene(sceneRoot, 100, 100, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(BG_COLOR);
        // Pane (this) catches mouse events for the controller's pan / scroll
        // handlers. SubScene events would otherwise be contained, so make it
        // mouse-transparent.
        subScene.setMouseTransparent(true);
        setPickOnBounds(true);
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.getTransforms().add(cameraT);
        subScene.setCamera(camera);

        // Lights live in the scene root (outside the rotating world group) so
        // shading stays consistent as the user rotates the maze. Two point
        // lights at different positions give walls a soft sense of volume.
        AmbientLight ambient = new AmbientLight(Color.gray(0.45));
        PointLight key = new PointLight(Color.WHITE);
        key.setTranslateX(-200);
        key.setTranslateY(-400);
        key.setTranslateZ(-300);
        PointLight fill = new PointLight(Color.gray(0.45));
        fill.setTranslateX(300);
        fill.setTranslateY(200);
        fill.setTranslateZ(-100);

        worldRoot.getTransforms().addAll(rotY, rotX);
        worldRoot.getChildren().addAll(mazeRoot, solutionRoot);
        sceneRoot.getChildren().addAll(worldRoot, ambient, key, fill);

        getChildren().add(subScene);

        maze3D.addListener((o, ov, nv) -> {
            visitedAt.clear();
            visitedNodes.clear();
            mazeLoadedAt = System.currentTimeMillis();
            rebuildScene();
            if (nv != null) animTimer.start();
        });
        solution.addListener((o, ov, nv) -> rebuildSolution());
        zoom.addListener((o, ov, nv) -> applyCamera());
        panX.addListener((o, ov, nv) -> applyRotation());
        panY.addListener((o, ov, nv) -> applyRotation());
        focusedLayer.addListener((o, ov, nv) -> applyLayerFocus());
        visitedCells.addListener((SetChangeListener<AState>) c -> {
            if (c.wasAdded())   addVisitedCell(c.getElementAdded());
            if (c.wasRemoved()) removeVisitedCell(c.getElementRemoved());
        });

        applyRotation();
        applyCamera();
    }

    private static PhongMaterial phong(Color c) {
        PhongMaterial m = new PhongMaterial(c);
        m.setSpecularColor(c.brighter());
        return m;
    }

    // ─── Scene rebuilding ────────────────────────────────────────────

    private void rebuildScene() {
        mazeRoot.getChildren().clear();
        solutionRoot.getChildren().clear();
        layerGroups.clear();
        startGlowSphere = null;

        Maze3D m = maze3D.get();
        if (m == null) return;

        // Pick a camera distance that keeps any maze in frame on first view.
        double maxDim = Math.max(Math.max(m.getColumns(), m.getRows()),
                                 m.getDepth() * LAYER_SPACING);
        baseCameraDistance = Math.max(400, maxDim * CELL_SIZE * 1.7);
        applyCamera();

        double halfW = (m.getColumns() - 1) * CELL_SIZE / 2.0;
        double halfH = (m.getRows() - 1) * CELL_SIZE / 2.0;
        double halfD = (m.getDepth() - 1) * CELL_SIZE * LAYER_SPACING / 2.0;

        for (int d = 0; d < m.getDepth(); d++) {
            Group layer = new Group();
            double layerZ = d * CELL_SIZE * LAYER_SPACING - halfD;

            // Floor tiles — one per cell. Wherever a vertical passage exists
            // between this layer and the next (both cells open), skip the
            // tile so the passage reads as a hole punched through the floor.
            // The last layer keeps a complete floor (no layer beyond it).
            boolean hasLayerBeyond = d < m.getDepth() - 1;
            for (int r = 0; r < m.getRows(); r++) {
                for (int c = 0; c < m.getColumns(); c++) {
                    if (hasLayerBeyond
                            && m.getCell(d, r, c) == 0
                            && m.getCell(d + 1, r, c) == 0) {
                        continue;  // vertical passage → punch a hole here
                    }
                    Box tile = new Box(CELL_SIZE * 0.98, CELL_SIZE * 0.98, 1.5);
                    tile.setMaterial(floorMaterial);
                    tile.setTranslateX(c * CELL_SIZE - halfW);
                    tile.setTranslateY(r * CELL_SIZE - halfH);
                    tile.setTranslateZ(layerZ + CELL_SIZE / 2.0);
                    tile.setOpacity(0.55);
                    layer.getChildren().add(tile);
                }
            }

            // Walls.
            for (int r = 0; r < m.getRows(); r++) {
                for (int c = 0; c < m.getColumns(); c++) {
                    if (m.getCell(d, r, c) == 1) {
                        Box wall = new Box(CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        wall.setMaterial(wallMaterial);
                        wall.setTranslateX(c * CELL_SIZE - halfW);
                        wall.setTranslateY(r * CELL_SIZE - halfH);
                        wall.setTranslateZ(layerZ);
                        layer.getChildren().add(wall);
                    }
                }
            }

            // Start marker — concentric blue sphere with a halo that pulses
            // briefly after generation.
            Position3D start = m.getStartPosition();
            if (start != null && start.getDepthIndex() == d) {
                double sx = start.getColumnIndex() * CELL_SIZE - halfW;
                double sy = start.getRowIndex() * CELL_SIZE - halfH;
                Sphere glow = new Sphere(CELL_SIZE * 0.55);
                glow.setMaterial(startGlowMaterial);
                glow.setTranslateX(sx);
                glow.setTranslateY(sy);
                glow.setTranslateZ(layerZ);
                glow.setOpacity(0.30);
                layer.getChildren().add(glow);
                startGlowSphere = glow;

                Sphere core = new Sphere(CELL_SIZE * 0.30);
                core.setMaterial(startMaterial);
                core.setTranslateX(sx);
                core.setTranslateY(sy);
                core.setTranslateZ(layerZ);
                layer.getChildren().add(core);
            }

            // Goal marker — a tilted cube that reads as a diamond.
            Position3D goal = m.getGoalPosition();
            if (goal != null && goal.getDepthIndex() == d) {
                Box gem = new Box(CELL_SIZE * 0.55, CELL_SIZE * 0.55, CELL_SIZE * 0.55);
                gem.setMaterial(goalMaterial);
                gem.setTranslateX(goal.getColumnIndex() * CELL_SIZE - halfW);
                gem.setTranslateY(goal.getRowIndex() * CELL_SIZE - halfH);
                gem.setTranslateZ(layerZ);
                gem.getTransforms().add(new Rotate(45, Rotate.Y_AXIS));
                gem.getTransforms().add(new Rotate(35, Rotate.X_AXIS));
                layer.getChildren().add(gem);
            }

            mazeRoot.getChildren().add(layer);
            layerGroups.add(layer);
        }

        applyLayerFocus();
        rebuildSolution();
    }

    private void rebuildSolution() {
        solutionRoot.getChildren().clear();
        Solution sol = solution.get();
        Maze3D m = maze3D.get();
        if (sol == null || m == null) return;

        double halfW = (m.getColumns() - 1) * CELL_SIZE / 2.0;
        double halfH = (m.getRows() - 1) * CELL_SIZE / 2.0;
        double halfD = (m.getDepth() - 1) * CELL_SIZE * LAYER_SPACING / 2.0;

        var path = sol.getSolutionPath();
        Maze3DState prev = null;
        for (AState s : path) {
            if (!(s instanceof Maze3DState ms)) continue;
            double cx = ms.getColumn() * CELL_SIZE - halfW;
            double cy = ms.getRow() * CELL_SIZE - halfH;
            double cz = ms.getDepth() * CELL_SIZE * LAYER_SPACING - halfD;

            Sphere dot = new Sphere(CELL_SIZE * 0.16);
            dot.setMaterial(solutionMaterial);
            dot.setTranslateX(cx);
            dot.setTranslateY(cy);
            dot.setTranslateZ(cz);
            solutionRoot.getChildren().add(dot);

            if (prev != null) {
                double px = prev.getColumn() * CELL_SIZE - halfW;
                double py = prev.getRow() * CELL_SIZE - halfH;
                double pz = prev.getDepth() * CELL_SIZE * LAYER_SPACING - halfD;
                solutionRoot.getChildren().add(connectingCylinder(px, py, pz, cx, cy, cz));
            }
            prev = ms;
        }
    }

    /**
     * Cylinder positioned at the midpoint of a→b and rotated so its long
     * axis aligns with the direction from a to b. The default cylinder
     * orientation in JavaFX is along the Y axis; we rotate around the
     * axis cross(Y, dir) by acos(dir·Y).
     */
    private Cylinder connectingCylinder(double ax, double ay, double az,
                                         double bx, double by, double bz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Cylinder cyl = new Cylinder(CELL_SIZE * 0.07, len);
        cyl.setMaterial(solutionMaterial);
        cyl.setTranslateX((ax + bx) / 2);
        cyl.setTranslateY((ay + by) / 2);
        cyl.setTranslateZ((az + bz) / 2);

        double nx = dx / len, ny = dy / len, nz = dz / len;
        // Axis = Y × direction = (nz, 0, -nx). When |axis| ≈ 0, direction is
        // colinear with Y so no rotation is needed.
        double axX = nz, axZ = -nx;
        double axLen = Math.sqrt(axX * axX + axZ * axZ);
        if (axLen > 1e-6) {
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, ny))));
            cyl.getTransforms().add(new Rotate(angle, new Point3D(axX, 0, axZ)));
        }
        return cyl;
    }

    // ─── Visited-cell animation ──────────────────────────────────────

    private void addVisitedCell(AState state) {
        if (!(state instanceof Maze3DState ms)) return;
        Maze3D m = maze3D.get();
        if (m == null) return;
        if (ms.getDepth() < 0 || ms.getDepth() >= layerGroups.size()) return;

        double halfW = (m.getColumns() - 1) * CELL_SIZE / 2.0;
        double halfH = (m.getRows() - 1) * CELL_SIZE / 2.0;
        double halfD = (m.getDepth() - 1) * CELL_SIZE * LAYER_SPACING / 2.0;
        double layerZ = ms.getDepth() * CELL_SIZE * LAYER_SPACING - halfD;

        Box cell = new Box(CELL_SIZE * 0.86, CELL_SIZE * 0.86, CELL_SIZE * 0.28);
        cell.setMaterial(visitedMaterial);
        cell.setTranslateX(ms.getColumn() * CELL_SIZE - halfW);
        cell.setTranslateY(ms.getRow() * CELL_SIZE - halfH);
        cell.setTranslateZ(layerZ);
        cell.setOpacity(0);
        layerGroups.get(ms.getDepth()).getChildren().add(cell);

        visitedNodes.put(state, cell);
        visitedAt.put(state, System.currentTimeMillis());
        animTimer.start();
    }

    private void removeVisitedCell(AState state) {
        Box cell = visitedNodes.remove(state);
        visitedAt.remove(state);
        if (cell != null && cell.getParent() instanceof Group g) {
            g.getChildren().remove(cell);
        }
    }

    private void tickAnimations() {
        long now = System.currentTimeMillis();

        // Fade visited cells in (ease-out).
        for (Map.Entry<AState, Long> e : visitedAt.entrySet()) {
            Box cell = visitedNodes.get(e.getKey());
            if (cell == null) continue;
            long age = now - e.getValue();
            if (age >= FADE_DURATION_MS) {
                cell.setOpacity(0.95);
            } else {
                double t = age / (double) FADE_DURATION_MS;
                cell.setOpacity(0.95 * (1.0 - (1.0 - t) * (1.0 - t)));
            }
        }

        // Pulse the start glow for the first few seconds after maze load,
        // then settle. Keeps the start findable without becoming a constant
        // distraction.
        if (startGlowSphere != null) {
            long age = now - mazeLoadedAt;
            if (age < PULSE_FADE_OUT_MS) {
                double envelope = 1.0 - age / (double) PULSE_FADE_OUT_MS;
                double phase = (age % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
                double pulse = 0.5 + 0.5 * Math.sin(phase * 2 * Math.PI);
                double scale = 1.0 + 0.18 * pulse * envelope;
                startGlowSphere.setScaleX(scale);
                startGlowSphere.setScaleY(scale);
                startGlowSphere.setScaleZ(scale);
                startGlowSphere.setOpacity(0.22 + 0.22 * pulse * envelope);
            } else {
                startGlowSphere.setScaleX(1);
                startGlowSphere.setScaleY(1);
                startGlowSphere.setScaleZ(1);
                startGlowSphere.setOpacity(0.28);
            }
        }
    }

    private boolean hasActiveAnimation() {
        long now = System.currentTimeMillis();
        for (long t : visitedAt.values()) {
            if (now - t < FADE_DURATION_MS) return true;
        }
        return startGlowSphere != null && (now - mazeLoadedAt) < PULSE_FADE_OUT_MS;
    }

    // ─── Camera, rotation, focus ─────────────────────────────────────

    private void applyCamera() {
        double distance = baseCameraDistance / Math.max(0.1, zoom.get());
        cameraT.setZ(-distance);
    }

    private void applyRotation() {
        double yaw = DEFAULT_YAW_DEG + panX.get() * DRAG_DEG_PER_PX;
        double pitch = DEFAULT_PITCH_DEG + panY.get() * DRAG_DEG_PER_PX;
        pitch = Math.max(-89, Math.min(89, pitch));
        rotY.setAngle(yaw);
        rotX.setAngle(pitch);
    }

    private void applyLayerFocus() {
        int focus = focusedLayer.get();
        for (int i = 0; i < layerGroups.size(); i++) {
            layerGroups.get(i).setVisible(focus < 0 || focus == i);
        }
        // Solution path threads through every layer — hide it while a
        // single layer is isolated so the focused layer reads cleanly.
        solutionRoot.setVisible(focus < 0);
    }

    // ─── Properties ──────────────────────────────────────────────────

    public ObjectProperty<Maze3D> maze3DProperty() { return maze3D; }
    public ObjectProperty<Solution> solutionProperty() { return solution; }
    public DoubleProperty zoomProperty() { return zoom; }
    public DoubleProperty panXProperty() { return panX; }
    public DoubleProperty panYProperty() { return panY; }
    public IntegerProperty focusedLayerProperty() { return focusedLayer; }
    public ObservableSet<AState> getVisitedCells() { return visitedCells; }

    /** Reset rotation to the default isometric pose. */
    public void resetPan() {
        panX.set(0);
        panY.set(0);
    }

    /** Convenience for the controller: the SubScene node so external code
     *  can attach extra event filters if needed. Currently unused. */
    Node getSubScene() {
        return subScene;
    }
}

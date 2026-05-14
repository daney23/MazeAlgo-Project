package mazealgo.viewmodel;

import mazealgo.model.MazeModel;
import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for movement rules, victory detection, and the diagonal
 * pinhole rule. Use a hand-built {@link Maze} (not the generator) so
 * walls are deterministic.
 */
class MazeViewModelTest {

    private MazeViewModel vm;

    @BeforeEach
    void setUp() {
        vm = new MazeViewModel(new MazeModel());
    }

    // Build a maze from rows of "." (passage) and "#" (wall).
    // First cell of first row is the start; bottom-right is the goal.
    private void load(String... rows) {
        Maze m = new Maze(rows.length, rows[0].length());
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                m.setCell(r, c, rows[r].charAt(c) == '#' ? 1 : 0);
            }
        }
        m.setStartPosition(new Position(0, 0));
        m.setGoalPosition(new Position(rows.length - 1, rows[0].length() - 1));
        vm.mazeProperty().set(m);
        vm.playerRowProperty().set(0);
        vm.playerColumnProperty().set(0);
    }

    @Test
    void straightMove_ontoPassage_advancesPlayer() {
        load("..", "..");
        vm.movePlayer(MovementDirection.RIGHT);
        assertEquals(0, vm.playerRowProperty().get());
        assertEquals(1, vm.playerColumnProperty().get());
    }

    @Test
    void move_intoWall_isBlocked() {
        load(".#", "..");
        vm.movePlayer(MovementDirection.RIGHT);
        assertEquals(0, vm.playerRowProperty().get());
        assertEquals(0, vm.playerColumnProperty().get());
    }

    @Test
    void move_offBoard_isBlocked() {
        load("..", "..");
        vm.movePlayer(MovementDirection.UP);
        vm.movePlayer(MovementDirection.LEFT);
        assertEquals(0, vm.playerRowProperty().get());
        assertEquals(0, vm.playerColumnProperty().get());
    }

    @Test
    void diagonal_throughPinhole_isBlocked() {
        // Player at (0,0), target (1,1). Both orthogonal corners (0,1) and (1,0) are walls.
        // The diagonal step would squeeze through the pinhole — must be rejected.
        load(".#",
                "#.");
        vm.movePlayer(MovementDirection.DOWN_RIGHT);
        assertEquals(0, vm.playerRowProperty().get());
        assertEquals(0, vm.playerColumnProperty().get());
    }

    @Test
    void diagonal_withOneOrthogonalOpen_isAllowed() {
        // Same target, but (1,0) is open — the path doesn't squeeze.
        load("..",
                "..");
        vm.movePlayer(MovementDirection.DOWN_RIGHT);
        assertEquals(1, vm.playerRowProperty().get());
        assertEquals(1, vm.playerColumnProperty().get());
    }

    @Test
    void victory_firesWhenPlayerLandsOnGoal() {
        load("..", "..");
        // walk to bottom-right
        vm.movePlayer(MovementDirection.DOWN_RIGHT);
        assertTrue(vm.victoryProperty().get());
    }

    @Test
    void victory_doesNotFireForIntermediateCells() {
        load("...", "...", "...");
        vm.movePlayer(MovementDirection.RIGHT);
        vm.movePlayer(MovementDirection.RIGHT);
        // at (0,2) — not the goal (2,2)
        assertFalse(vm.victoryProperty().get());
    }

    @Test
    void generate_resetsVictoryAndPlayerToStart() {
        // simulate an existing won state
        load("..", "..");
        vm.movePlayer(MovementDirection.DOWN_RIGHT);
        assertTrue(vm.victoryProperty().get());

        vm.generate(5, 5);
        assertFalse(vm.victoryProperty().get());
        // After generate, player is at the new maze's start (which MyMazeGenerator places at (0,0))
        assertEquals(0, vm.playerRowProperty().get());
        assertEquals(0, vm.playerColumnProperty().get());
    }

    @Test
    void afterVictory_furtherMovesAreIgnored() {
        load("..", "..");
        vm.movePlayer(MovementDirection.DOWN_RIGHT);
        assertTrue(vm.victoryProperty().get());
        // any further input is a no-op
        vm.movePlayer(MovementDirection.UP);
        assertEquals(1, vm.playerRowProperty().get());
        assertEquals(1, vm.playerColumnProperty().get());
    }

    @Test
    void numpadMapping_isCorrect() {
        assertEquals(MovementDirection.DOWN_LEFT, MovementDirection.forNumpadDigit(1));
        assertEquals(MovementDirection.DOWN, MovementDirection.forNumpadDigit(2));
        assertEquals(MovementDirection.DOWN_RIGHT, MovementDirection.forNumpadDigit(3));
        assertEquals(MovementDirection.LEFT, MovementDirection.forNumpadDigit(4));
        assertEquals(MovementDirection.RIGHT, MovementDirection.forNumpadDigit(6));
        assertEquals(MovementDirection.UP_LEFT, MovementDirection.forNumpadDigit(7));
        assertEquals(MovementDirection.UP, MovementDirection.forNumpadDigit(8));
        assertEquals(MovementDirection.UP_RIGHT, MovementDirection.forNumpadDigit(9));
    }
}

package io.github.daney23.mazealgo.model.algorithms.mazeGenerators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MazeByteArrayTest {

    @Test
    void roundtrip_preservesShapeAndStartAndGoal() {
        Maze original = new Maze(7, 11);
        original.setCell(0, 0, 0);
        original.setCell(6, 10, 0);
        original.setCell(3, 5, 1);
        original.setStartPosition(new Position(0, 0));
        original.setGoalPosition(new Position(6, 10));

        Maze restored = new Maze(original.toByteArray());

        assertEquals(original.getRows(), restored.getRows());
        assertEquals(original.getColumns(), restored.getColumns());
        assertEquals(original.getStartPosition().getRowIndex(), restored.getStartPosition().getRowIndex());
        assertEquals(original.getStartPosition().getColumnIndex(), restored.getStartPosition().getColumnIndex());
        assertEquals(original.getGoalPosition().getRowIndex(), restored.getGoalPosition().getRowIndex());
        assertEquals(original.getGoalPosition().getColumnIndex(), restored.getGoalPosition().getColumnIndex());
    }

    @Test
    void roundtrip_preservesEveryCell() {
        Maze original = new MyMazeGenerator().generate(20, 20);
        Maze restored = new Maze(original.toByteArray());

        for (int r = 0; r < original.getRows(); r++) {
            for (int c = 0; c < original.getColumns(); c++) {
                assertEquals(original.getCell(r, c), restored.getCell(r, c),
                        "cell (" + r + "," + c + ") differs after roundtrip");
            }
        }
    }

    @Test
    void sameMaze_producesIdenticalBytes() {
        // Used as a cache key — must be deterministic.
        Maze maze = new MyMazeGenerator().generate(10, 10);
        assertArrayEquals(maze.toByteArray(), maze.toByteArray());
    }

    @Test
    void invalidBytes_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Maze(new byte[]{1, 2, 3}));
    }
}

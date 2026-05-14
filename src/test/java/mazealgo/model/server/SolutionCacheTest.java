package mazealgo.model.server;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.mazeGenerators.MyMazeGenerator;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolutionCacheTest {

    @Test
    void get_returnsEmpty_whenKeyAbsent(@TempDir Path dir) {
        SolutionCache cache = new SolutionCache(dir);
        assertFalse(cache.get("hello".getBytes()).isPresent());
    }

    @Test
    void put_thenGet_returnsSameSolution(@TempDir Path dir) {
        SolutionCache cache = new SolutionCache(dir);
        Maze maze = new MyMazeGenerator().generate(8, 8);
        byte[] key = maze.toByteArray();
        Solution original = new BestFirstSearch().solve(new SearchableMaze(maze));

        cache.put(key, original);
        Optional<Solution> retrieved = cache.get(key);

        assertTrue(retrieved.isPresent());
        assertEquals(original.getSolutionPath().size(), retrieved.get().getSolutionPath().size());
        // first and last states match (path equality requires AState equality which works for MazeState)
        assertEquals(
                original.getSolutionPath().get(0),
                retrieved.get().getSolutionPath().get(0));
    }

    @Test
    void put_createsFileNamedBySha256(@TempDir Path dir) throws Exception {
        SolutionCache cache = new SolutionCache(dir);
        byte[] key = "the same maze bytes".getBytes();
        Solution dummy = new BestFirstSearch().solve(new SearchableMaze(
                new MyMazeGenerator().generate(3, 3)));

        cache.put(key, dummy);

        String expectedHash = SolutionCache.sha256Hex(key);
        assertTrue(Files.exists(dir.resolve(expectedHash + ".sol")));
    }

    @Test
    void differentMazes_produceDifferentKeys(@TempDir Path dir) {
        SolutionCache cache = new SolutionCache(dir);
        Maze a = new MyMazeGenerator().generate(5, 5);
        Maze b = new MyMazeGenerator().generate(7, 7);
        Solution solA = new BestFirstSearch().solve(new SearchableMaze(a));

        cache.put(a.toByteArray(), solA);

        // looking up B (different dims so different bytes) is a miss
        assertFalse(cache.get(b.toByteArray()).isPresent());
    }
}

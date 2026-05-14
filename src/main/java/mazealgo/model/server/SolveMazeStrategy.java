package mazealgo.model.server;

import mazealgo.model.algorithms.mazeGenerators.Maze;
import mazealgo.model.algorithms.search.BestFirstSearch;
import mazealgo.model.algorithms.search.ISearchingAlgorithm;
import mazealgo.model.algorithms.search.SearchableMaze;
import mazealgo.model.algorithms.search.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Server strategy: receives a {@link Maze}, returns a {@link Solution}.
 * Consults {@link SolutionCache} first — if this exact maze was solved
 * earlier (same bytes → same SHA-256), the stored Solution is returned
 * directly and the search algorithm is not invoked.
 *
 * <p>Wire protocol (symmetric Object streams on both sides):
 * <pre>
 *   client ─[ObjectOutputStream]─ Maze     ─→ server
 *   client ←─────── Solution ─── server
 * </pre>
 *
 * <p>A new searcher is created per request (cheap, and avoids the
 * stale-state pitfalls discussed in {@code ASearchingAlgorithm}).
 */
public class SolveMazeStrategy implements IServerStrategy {
    private static final Logger log = LogManager.getLogger(SolveMazeStrategy.class);

    private final SolutionCache cache;
    private final Supplier<ISearchingAlgorithm> searcherFactory;

    public SolveMazeStrategy() {
        this(new SolutionCache(), BestFirstSearch::new);
    }

    public SolveMazeStrategy(SolutionCache cache, Supplier<ISearchingAlgorithm> searcherFactory) {
        this.cache = cache;
        this.searcherFactory = searcherFactory;
    }

    @Override
    public void serverStrategy(InputStream inFromClient, OutputStream outToClient) throws Exception {
        ObjectInputStream in = new ObjectInputStream(inFromClient);
        Maze maze = (Maze) in.readObject();
        byte[] key = maze.toByteArray();

        Solution solution = cache.get(key).orElseGet(() -> {
            ISearchingAlgorithm searcher = searcherFactory.get();
            Solution fresh = searcher.solve(new SearchableMaze(maze));
            log.debug("Solved {}x{} maze in {} nodes, caching",
                    maze.getRows(), maze.getColumns(), searcher.getNumberOfNodesEvaluated());
            cache.put(key, fresh);
            return fresh;
        });

        ObjectOutputStream out = new ObjectOutputStream(outToClient);
        out.writeObject(solution);
        out.flush();
    }
}

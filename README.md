# MazeAlgo

**A 2D / 3D maze generation and search engine with a multi-threaded server backend and (upcoming) JavaFX GUI.**

This started as a university assignment and is being built out into a portfolio-grade project: clean MVVM separation, classic GoF design patterns, multi-threaded networking with smart caching, and full unit + integration test coverage.

---

## TL;DR

- **What it is.** A maze engine — generates 2D/3D mazes, solves them with BFS / DFS / Best-First (A* with admissible heuristics), and serves both operations over sockets.
- **Status.** Phases 1–2 of 4 done. `mvn clean test` → BUILD SUCCESS, 26 / 26 tests pass. JavaFX UI is Phase 3, in progress.
- **For the technically curious.** Adapter, Strategy, Decorator, and Template Method patterns all appear here in real load-bearing roles — not as toy examples. The server caches solutions on disk keyed by SHA-256 of the maze bytes; an integration test confirms the search algorithm runs exactly once across two identical requests.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 17 (compiled with `--release 17`, runs on JDK 17+) |
| Build | Maven 3.9 |
| UI _(in progress)_ | JavaFX 17.0.11 — controls, fxml, media |
| Logging | Log4j2 |
| Tests | JUnit Jupiter 5.8.1 (26 tests, all passing) |

---

## What's interesting in here

- **Three search algorithms over a common interface.** BFS, DFS, and a Best-First Search that behaves like A* when the domain supplies an admissible heuristic. Octile distance for 2D (diagonal moves cost more than straight moves), Manhattan distance scaled by step-cost for 3D — both proven admissible in comments next to the code.
- **Adapter pattern** keeps the search algorithms decoupled from the maze representation: `SearchableMaze` and `SearchableMaze3D` expose `Maze` / `Maze3D` as a generic `ISearchable`, so the same `BestFirstSearch` solves both 2D and 3D problems with zero changes.
- **Iterative DFS** (explicit stack, no recursion) so a 1000×1000 maze doesn't blow the JVM stack.
- **Diagonal move legality.** A diagonal step in 2D is only allowed when at least one of the two orthogonal cells around the corner is also open — paths can't squeeze through a wall pinhole.
- **Multi-threaded socket server** with an `ExecutorService` thread pool and a `setSoTimeout`-driven accept loop, so `stop()` is responsive even when no clients are connecting.
- **Smart caching.** Identical solve requests don't re-run the algorithm — `SolutionCache` keys solutions by SHA-256 of the maze bytes and stores them on disk under `$TMPDIR/MazeAlgo/cache/`. An integration test verifies that two identical requests invoke the search algorithm exactly once.
- **Streaming run-length encoding** via the `MyCompressorOutputStream` Decorator — a 10,000-byte sparse maze grid lands in well under 1 KB on the wire (>10× compression, asserted in the test suite).
- **26 tests, all passing**, including a full end-to-end test that spins up `MyServer` on an OS-assigned port and verifies both strategies via real sockets.

---

## Architecture

```
                           ┌──────────────────────────────────┐
                           │   View  (JavaFX, in progress)    │
                           │   MazeView.fxml + Controller     │
                           └────────────────┬─────────────────┘
                                            │ binds to JavaFX properties
                           ┌────────────────▼─────────────────┐
                           │           ViewModel              │
                           │   maze / solution / playerRow    │
                           │   playerColumn   (Observable)    │
                           └────────────────┬─────────────────┘
                                            │
                           ┌────────────────▼─────────────────┐
                           │            Model                 │
                           │   ┌────────────┐  ┌───────────┐  │
                           │   │ Algorithms │  │  Server   │  │
                           │   │ (gen+sea-  │  │ + Cache + │  │
                           │   │  rch +     │  │  Compres- │  │
                           │   │  adapters) │  │  sion     │  │
                           │   └────────────┘  └───────────┘  │
                           └──────────────────────────────────┘
```

The Server layer reuses the same `MyMazeGenerator` and `BestFirstSearch` the in-process Model uses — there is one source of truth for maze logic.

---

## Design patterns at a glance

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `IServerStrategy` (`GenerateMazeStrategy`, `SolveMazeStrategy`) | One pluggable behaviour per server endpoint. Adding a new endpoint = one new class. |
| **Strategy** | `ISearchingAlgorithm` (`BreadthFirstSearch`, `DepthFirstSearch`, `BestFirstSearch`) | Swappable searchers, identical contract: `solve(ISearchable) → Solution`. |
| **Adapter** | `SearchableMaze`, `SearchableMaze3D` | Expose 2D / 3D mazes as a unified `ISearchable` so searchers don't know dimensionality. |
| **Decorator** | `MyCompressorOutputStream`, `MyDecompressorInputStream` | Wrap any `OutputStream` / `InputStream` to add RLE compression transparently. |
| **Template Method** | `ASearchingAlgorithm`, `AMazeGenerator`, `AMaze3DGenerator` | Shared concerns (counter reset, timing) live in the base; subclasses implement the variable bits. |
| **Factory (Supplier)** | `SolveMazeStrategy(cache, searcherFactory)` | A fresh searcher per request — avoids leaking state across pooled solves. |

---

## Algorithms

### Best-First Search (A\*-like)

`src/main/java/io/github/daney23/mazealgo/model/algorithms/search/BestFirstSearch.java`

Reuses the BFS skeleton but swaps the FIFO queue for a `PriorityQueue` ordered by `cost-so-far + domain.heuristic(state, goal)`. Because the heuristics in `SearchableMaze` and `SearchableMaze3D` are admissible, the returned path is optimal.

```java
openList = new PriorityQueue<>(
    Comparator.comparingDouble(s -> s.getCost() + domain.heuristic(s, goal))
);
return super.solve(domain);
```

### Octile distance heuristic (2D, with diagonals)

`src/main/java/io/github/daney23/mazealgo/model/algorithms/search/SearchableMaze.java`

```java
// 10 * max(|dr|,|dc|) + 5 * min(|dr|,|dc|)
// — the exact optimal cost on an unobstructed grid, hence admissible.
return 10 * Math.max(dr, dc) + 5 * Math.min(dr, dc);
```

### Iterative randomized DFS maze generator

`src/main/java/io/github/daney23/mazealgo/model/algorithms/mazeGenerators/MyMazeGenerator.java`

The classic "recursive backtracker" rewritten with an explicit `Deque` stack so it scales to mazes larger than the JVM thread stack would allow.

---

## Server protocol

Two ports, one strategy each. The client speaks `Object` streams; the server-to-client direction is RLE-compressed for the generate endpoint.

```
GENERATE (port 5400)
   client →  ObjectOutputStream  →  int[]{rows, columns}      →  server
   client  ←──── byte[] (RLE-compressed Maze.toByteArray) ────  server

SOLVE (port 5401)
   client →  ObjectOutputStream  →  Maze                       →  server
   client  ←─────────── Solution ──────────────                 server
       (server consults SolutionCache before running search)
```

`SolutionCache` keys on `SHA-256(Maze.toByteArray())`. A corrupted cache entry is dropped silently — the next request rebuilds it.

---

## Build & run

You'll need JDK 17+ and Maven 3.6+.

```bash
# Run all tests (Phase 1 + Phase 2: 26 tests)
mvn clean test

# Compile only
mvn compile

# Run the JavaFX entrypoint (placeholder window — Phase 3 will fill the UI)
mvn javafx:run
```

### Demo: start the server, hit it from a client

In two terminals:

```bash
# Terminal 1
mvn -q exec:java -Dexec.mainClass=io.github.daney23.mazealgo.examples.RunMazeServer

# Terminal 2
mvn -q exec:java -Dexec.mainClass=io.github.daney23.mazealgo.examples.RunMazeClient
```

(The integration test does the same thing in-process, so the demo isn't load-bearing — it's just for visual confirmation.)

---

## Project structure

```
src/main/java/io/github/daney23/mazealgo/
├── MazeApp.java                            JavaFX Application entry
├── Launcher.java                           workaround for non-modular JavaFX launch
├── model/
│   ├── MazeModel.java                      facade (in-process; will delegate to server in Phase 3)
│   ├── algorithms/
│   │   ├── mazeGenerators/                 Maze, Position, generators (Empty/Simple/My)
│   │   ├── maze3D/                         Maze3D, Position3D, generators, SearchableMaze3D
│   │   └── search/                         ISearchable, AState, BFS, DFS, BestFirstSearch, SearchableMaze, Solution
│   ├── io/                                 MyCompressorOutputStream, MyDecompressorInputStream
│   └── server/                             MyServer, IServerStrategy, Generate/SolveMazeStrategy, SolutionCache
├── view/                                   FXML controllers
├── viewmodel/                              JavaFX-property bridge
└── examples/                               manual runners + RunMazeServer/RunMazeClient

src/test/java/...                            26 tests across compression, byte serialization,
                                             cache, search correctness, and end-to-end server
```

---

## Roadmap

- [x] **Phase 1** — Maven migration, MVVM scaffolding, Serializable refactor for the upcoming wire protocol, base + reset hook on `ASearchingAlgorithm` (fixes a real bug: early goal-find used to leave stale states in the BFS open list and corrupt the next reuse).
- [x] **Phase 2** — Multi-threaded server, Decorator-pattern compression, content-addressed solution cache.
- [ ] **Phase 3** — JavaFX GUI: custom `MazeDisplayer` canvas with wall / player / goal sprites, NumPad-driven movement (including diagonals from `SearchableMaze`), `Ctrl + scroll` zoom, background music and a victory chime.
- [ ] **Phase 4** — Algorithm visualizer: animated drawing of the solution path, a live `numberOfNodesEvaluated` counter, and a "watch the search happen" mode that colours cells as they enter the visited set. Full Javadoc generation.

---

## Author

[**daney23**](https://github.com/daney23) — CS undergrad. Built on Windows with IntelliJ IDEA and JDK 17/25.

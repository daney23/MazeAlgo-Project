# Changelog

All notable changes to this project are documented here. This project follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format and
[Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-05-15

First production release. All four planned phases shipped plus the cross-cutting
server / UI integration.

### Phase 1 — Maven + MVVM architecture
- Migrated from an IntelliJ project layout to standard Maven
  (`src/main/java`, `src/test/java`, `src/main/resources`).
- `pom.xml` targets Java 17 via `--release 17` with JavaFX 17.0.11
  (controls / fxml / media), Log4j2 2.23.1, JUnit Jupiter 5.8.1.
- Repackaged all algorithm classes under `mazealgo.model.algorithms.*`;
  scaffolded `mazealgo.model` / `view` / `viewmodel`.
- `Maze`, `Maze3D`, `Position`, `Position3D`, `AState`, `Solution` now
  implement `Serializable` (with `serialVersionUID`) so they survive
  `ObjectOutputStream` over a socket.
- `ASearchingAlgorithm` gained a `reset()` hook overridden by BFS to
  clear `openList` between solves — fixes a real bug where an early
  goal-find left stale states in the queue and corrupted the next reuse.

### Phase 2 — Multi-threaded server, compression, smart cache
- `MyServer`: `ServerSocket` + `ExecutorService` thread pool (8 workers
  by default), accept loop with `setSoTimeout` so `stop()` is responsive
  even when no clients are connecting. Bound port exposed via
  `getPort()` so tests can use OS-assigned ephemeral ports.
- `IServerStrategy` + `GenerateMazeStrategy` + `SolveMazeStrategy`.
- `SolutionCache`: SHA-256 of `Maze.toByteArray()` as key,
  `$TMPDIR/MazeAlgo/cache/<hash>.sol` as store; corrupt entries are
  dropped silently and rebuilt on the next request.
- `MyCompressorOutputStream` / `MyDecompressorInputStream` — Decorator
  pattern, streaming RLE encoder emitting `(count, value)` pairs,
  splits runs longer than 255 across consecutive pairs.
- `Maze.toByteArray()` and `Maze(byte[])` for the compact wire format.
- 18 new tests: compressor roundtrip on empty / single / long-run /
  random inputs and a >10× compression-ratio assertion on sparse grids;
  Maze byte roundtrip; SolutionCache put / get with `@TempDir`; full
  end-to-end MyServer integration verifying the second identical solve
  request hits the cache (search algorithm invoked exactly once).

### Phase 3 — JavaFX GUI
- Custom `MazeDisplayer` `Canvas` — sprite-capable (drops in
  `wall.png` / `player.png` / `goal.png` automatically), falls back to
  drawn shapes so the UI works without third-party assets.
- WASD and arrow-key movement; `Ctrl + scroll` zoom clamped 40–400%.
- `MazeViewModel` exposes JavaFX properties bound by the displayer:
  `maze`, `solution`, `playerRow`, `playerColumn`, `zoom`, `victory`,
  `nodesEvaluated`, `visualizing`.
- `SoundPlayer`: JavaFX `MediaPlayer` for an optional `background.mp3`,
  `AudioClip` for an optional `victory.wav` / `.mp3`; falls back to a
  synthesized C–E–G–C arpeggio via `javax.sound.sampled` (with linear
  fade envelopes to avoid clicks).
- Diagonal pinhole rule preserved in the ViewModel for parity with
  `SearchableMaze`, even though keyboard input is cardinal-only.

### Phase 4 — Algorithm visualizer + Javadoc
- `ASearchingAlgorithm.setNodeEvaluatedListener(...)` — Consumer<AState>
  observer hook fired once per node committed to the visited set.
  BFS / DFS / Best-First all participate; sync / server paths register
  no listener and pay nothing.
- **Solution Hint** button — runs Best-First synchronously and draws
  the path on the canvas as an orange dotted line through cell centers.
- **Watch Search** button — runs Best-First on a daemon thread with
  Platform.runLater feeding each visited state into an `ObservableSet`,
  painting cells yellow as they're explored. Adaptive per-cell delay
  keeps the animation under a 3-second budget regardless of maze size.
  A cancellation flag bails the search cleanly when a new
  Generate / Watch fires mid-flight.
- Live **Nodes: N** counter in the toolbar updates as the search runs.
- `maven-javadoc-plugin` 3.6.3 wired — `mvn javadoc:javadoc` generates
  `target/site/apidocs/`.
- 6 new tests pin the listener contract (count matches
  `getNumberOfNodesEvaluated`, first notification is the start state,
  null clears, missing listener is a no-op).

### Server / UI integration (post-Phase-4)
- `MazeApp.start()` spawns two embedded `MyServer` instances on
  OS-assigned ports and injects them into `MazeModel` via
  `FXMLLoader.setControllerFactory`. `Application.stop()` shuts both
  down cleanly.
- `Solution` now carries the `nodesEvaluated` count so the
  "Nodes: N" label stays accurate across the wire.
- Every **Generate** and **Solution Hint** click flows through the
  Phase 2 server pipeline (Strategy → RLE Decorator → SHA-256 cache).
  **Watch Search** stays in-process — it needs the per-node observer
  callback the server protocol doesn't stream.

### Tooling, docs, and polish
- Recruiter-facing README with TL;DR, screenshots, design-patterns
  table, algorithm snippets, wire-protocol diagram, build instructions.
- GitHub Actions CI (`mvn -B -ntp clean test` on push + PR) — green
  badge linked from the README.
- Github Pages deployment of Javadoc on every push to `main`.
- `.gitignore` set up for Maven; package coordinates flattened to
  `mazealgo.*` (groupId stays `io.github.daney23` in `pom.xml`).

### Stats at release
- 42 tests, all passing under `mvn clean test`.
- 50+ Java source files; 6 commits per phase on average.

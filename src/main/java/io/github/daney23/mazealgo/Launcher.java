package io.github.daney23.mazealgo;

/**
 * Plain-class entry point. JavaFX needs the application class to be loaded
 * by its own launcher; running the {@link MazeApp#main} directly from a
 * non-modular jar can fail with "Error: JavaFX runtime components are
 * missing". Bouncing through this class avoids that.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        MazeApp.main(args);
    }
}

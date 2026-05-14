package mazealgo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import mazealgo.model.MazeModel;
import mazealgo.model.server.GenerateMazeStrategy;
import mazealgo.model.server.MyServer;
import mazealgo.model.server.SolveMazeStrategy;
import mazealgo.view.MazeViewController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

/**
 * JavaFX application entry point.
 *
 * <p>On startup, {@link #start(Stage)} spawns two embedded
 * {@link MyServer} instances — one for {@code GenerateMazeStrategy},
 * one for {@code SolveMazeStrategy} — on OS-assigned ephemeral ports,
 * then injects those ports into a {@link MazeModel} that goes to the
 * {@link MazeViewController} via the FXMLLoader's controller factory.
 * Every Generate / Solution Hint click in the UI is now a real socket
 * roundtrip through the Phase 2 server pipeline (Strategy + Decorator
 * compression + SHA-256 solution cache).
 *
 * <p>{@link Application#stop()} shuts the servers down cleanly so the
 * JVM can exit when the window closes.
 */
public class MazeApp extends Application {
    private static final Logger log = LogManager.getLogger(MazeApp.class);

    private MyServer generateServer;
    private MyServer solveServer;

    @Override
    public void start(Stage stage) throws Exception {
        log.info("Starting MazeAlgo");

        generateServer = new MyServer(0, 200, new GenerateMazeStrategy());
        solveServer = new MyServer(0, 200, new SolveMazeStrategy());
        generateServer.start();
        solveServer.start();
        log.info("Embedded servers up: generate=:{}  solve=:{}",
                generateServer.getPort(), solveServer.getPort());

        MazeModel model = new MazeModel(generateServer.getPort(), solveServer.getPort());

        FXMLLoader loader = new FXMLLoader(
                MazeApp.class.getResource("/mazealgo/view/MazeView.fxml"));
        loader.setControllerFactory(clazz -> {
            if (clazz == MazeViewController.class) {
                return new MazeViewController(model);
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("FXMLLoader could not construct " + clazz, e);
            }
        });
        BorderPane root = loader.load();

        Scene scene = new Scene(root);
        URL stylesheet = MazeApp.class.getResource("/mazealgo/view/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("MazeAlgo");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        log.info("Stopping MazeAlgo");
        if (generateServer != null) generateServer.stop();
        if (solveServer != null) solveServer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

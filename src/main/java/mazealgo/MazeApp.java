package mazealgo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JavaFX application entry point. Loads the root FXML and shows the stage.
 * The real UI is built out in Phase 3 - this just verifies the MVVM wiring
 * compiles and launches.
 */
public class MazeApp extends Application {
    private static final Logger log = LogManager.getLogger(MazeApp.class);

    @Override
    public void start(Stage stage) throws Exception {
        log.info("Starting MazeAlgo");
        FXMLLoader loader = new FXMLLoader(
                MazeApp.class.getResource("/mazealgo/view/MazeView.fxml"));
        BorderPane root = loader.load();
        stage.setTitle("MazeAlgo");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

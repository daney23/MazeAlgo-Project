package mazealgo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

/**
 * JavaFX application entry point. Loads {@code MazeView.fxml}, attaches
 * the stylesheet, and shows the stage.
 */
public class MazeApp extends Application {
    private static final Logger log = LogManager.getLogger(MazeApp.class);

    @Override
    public void start(Stage stage) throws Exception {
        log.info("Starting MazeAlgo");

        FXMLLoader loader = new FXMLLoader(
                MazeApp.class.getResource("/mazealgo/view/MazeView.fxml"));
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

    public static void main(String[] args) {
        launch(args);
    }
}

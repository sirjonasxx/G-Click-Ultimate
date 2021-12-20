package extension;

import gearth.Main;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import gearth.ui.GEarthController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class GClickUltimateLauncher extends ExtensionFormCreator {

    public ExtensionForm createForm(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("gclickultimate.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("G-Click Ultimate");
        primaryStage.setScene(new Scene(root));
        primaryStage.getScene().getStylesheets().add(GEarthController.class.getResource("/gearth/themes/G-Earth/styling.css").toExternalForm());
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/gearth/themes/G-Earth/logoSmall.png")));

        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        return loader.getController();

    }

    public static void main(String[] args) {
        runExtensionForm(args, GClickUltimateLauncher.class);
    }

}

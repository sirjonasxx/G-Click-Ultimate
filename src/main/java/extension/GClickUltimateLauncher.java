package extension;

import gearth.extensions.ThemedExtensionFormCreator;
import javafx.stage.Stage;

import java.net.URL;

public class GClickUltimateLauncher extends ThemedExtensionFormCreator {

    @Override
    protected String getTitle() {
        return "G-Click Ultimate";
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("gclickultimate.fxml");
    }

    @Override
    protected void initialize(Stage primaryStage) {
        primaryStage.getScene().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    }

    public static void main(String[] args) {
        runExtensionForm(args, GClickUltimateLauncher.class);
    }

}

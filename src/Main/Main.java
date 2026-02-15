
package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application{

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Landpage.fxml"));
        Scene sc = new Scene(root, 1000, 600);
        primaryStage.setScene(sc);
        primaryStage.show();
    }
    
}

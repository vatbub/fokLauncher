package view;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.jdom2.JDOMException;

import applist.App;
import common.*;
import javafx.application.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.*;
import view.updateAvailableDialog.UpdateAvailableDialog;

public class MainWindow extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	private ResourceBundle bundle = ResourceBundle.getBundle("view.MainWindow");

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="appList"
	private ListView<String> appList; // Value injected by FXMLLoader

	@FXML // fx:id="enableSnapshotsCheckbox"
	private CheckBox enableSnapshotsCheckbox; // Value injected by FXMLLoader

	@FXML // fx:id="workOfflineCheckbox"
	private CheckBox workOfflineCheckbox; // Value injected by FXMLLoader

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			common.Common.setAppName("foklauncher");

			Thread updateThread = new Thread() {
				@Override
				public void run() {
					UpdateInfo update = UpdateChecker.isUpdateAvailable(Config.getUpdateRepoBaseURL(), Config.groupID,
							Config.artifactID, Config.updateFileClassifier);
					if (update.showAlert) {
						Platform.runLater(new Runnable() {

							@Override
							public void run() {
								new UpdateAvailableDialog(update);
							}

						});
					}
				}
			};
			updateThread.setName("updateThread");
			updateThread.start();

			Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"), bundle);

			Scene scene = new Scene(root);
			// scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			primaryStage.setTitle(bundle.getString("windowTitle"));

			primaryStage.setMinWidth(scene.getRoot().minWidth(0) + 70);
			primaryStage.setMinHeight(scene.getRoot().minHeight(0) + 70);

			primaryStage.setScene(scene);

			// Set Icon
			primaryStage.getIcons().add(new Image(MainWindow.class.getResourceAsStream("icon.png")));

			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML // This method is called by the FXMLLoader when initialization is
			// complete
	void initialize() {
		assert appList != null : "fx:id=\"appList\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert enableSnapshotsCheckbox != null : "fx:id=\"enableSnapshotsCheckbox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert workOfflineCheckbox != null : "fx:id=\"workOfflineCheckbox\" was not injected: check your FXML file 'MainWindow.fxml'.";

		// Initialize your logic here: all @FXML variables will have been
		// injected

		Thread getAppListThread = new Thread() {
			@Override
			public void run() {
				try {

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							ObservableList<String> items = FXCollections
									.observableArrayList(bundle.getString("WaitForAppList"));
							appList.setItems(items);
							appList.setDisable(true);
						}

					});

					List<App> apps = App.getAppList();

					ObservableList<String> items = FXCollections.observableArrayList();

					for (App app : apps) {
						items.add(app.getName());
					}

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							appList.setItems(items);
							appList.setDisable(false);
						}

					});

				} catch (JDOMException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		getAppListThread.setName("getAppListThread");
		getAppListThread.start();

	}

}

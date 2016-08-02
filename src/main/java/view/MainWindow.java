package view;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.jdom2.JDOMException;

import applist.App;
import common.*;
import javafx.application.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.*;
import view.updateAvailableDialog.UpdateAvailableDialog;

public class MainWindow extends Application implements HidableUpdateProgressDialog {

	public static void main(String[] args) {
		launch(args);
	}

	private ResourceBundle bundle = ResourceBundle.getBundle("view.MainWindow");
	private static Prefs prefs;
	private static String enableSnapshotsPrefKey = "enableSnapshots";
	private static List<App> apps;
	private static Stage stage;
	private static Thread downloadAndLaunchThread = new Thread();

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="appList"
	private ListView<String> appList; // Value injected by FXMLLoader

	@FXML // fx:id="enableSnapshotsCheckbox"
	private CheckBox enableSnapshotsCheckbox; // Value injected by FXMLLoader

	@FXML // fx:id="launchButton"
	private Button launchButton; // Value injected by FXMLLoader

	@FXML // fx:id="progressBar"
	private ProgressBar progressBar; // Value injected by FXMLLoader

	@FXML // fx:id="progressLabel"
	private Label progressLabel; // Value injected by FXMLLoader

	@FXML // fx:id="workOfflineCheckbox"
	private CheckBox workOfflineCheckbox; // Value injected by FXMLLoader

	@FXML
	/**
	 * fx:id="updateLink"
	 */
	private Hyperlink updateLink; // Value injected by FXMLLoader

	@FXML
	/**
	 * fx:id="versionLabel"
	 */
	private Label versionLabel; // Value injected by FXMLLoader

	// Handler for ListView[fx:id="appList"] onMouseClicked
	@FXML
	void appListOnMouseClicked(MouseEvent event) {
		if (appList.getSelectionModel().getSelectedIndex() != -1) {
			// Only enable the button if something was selected

		}
	}

	@FXML
	/**
	 * Handler for Hyperlink[fx:id="updateLink"] onAction
	 * 
	 * @param event
	 *            The event object that contains information about the event.
	 */
	void updateLinkOnAction(ActionEvent event) {
		// Check for new version ignoring ignored updates
		Thread updateThread = new Thread() {
			@Override
			public void run() {
				UpdateInfo update = UpdateChecker.isUpdateAvailableCompareAppVersion(Config.getUpdateRepoBaseURL(),
						Config.groupID, Config.artifactID, Config.updateFileClassifier);
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						new UpdateAvailableDialog(update);
					}
				});
			}
		};
		updateThread.setName("manualUpdateThread");
		updateThread.start();
	}

	// Handler for Button[fx:id="launchButton"] onAction
	@FXML
	void launchButtonOnAction(ActionEvent event) {
		MainWindow gui = this;
		App app = apps.get(appList.getSelectionModel().getSelectedIndex());

		if (!downloadAndLaunchThread.isAlive()) {
			// Launch the download
			downloadAndLaunchThread = new Thread() {
				@Override
				public void run() {
					try {
						app.downloadIfNecessaryAndLaunch(
								enableSnapshotsCheckbox.isSelected(), gui, workOfflineCheckbox.isSelected());
					} catch (IOException | JDOMException e) {
						gui.showErrorMessage("An error occurred: " + e.getMessage());
						e.printStackTrace();
					}
					;
				}
			};

			downloadAndLaunchThread.setName("downloadAndLaunchThread");
			downloadAndLaunchThread.start();
		} else {
			app.cancelDownloadAndLaunch(gui);
		}
	}

	// Handler for CheckBox[fx:id="workOfflineCheckbox"] onAction
	@FXML
	void workOfflineCheckboxOnAction(ActionEvent event) {
		updateLaunchButton();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		try {
			common.Common.setAppName("foklauncher");
			prefs = new Prefs(MainWindow.class.getName());

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
		assert launchButton != null : "fx:id=\"launchButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert progressLabel != null : "fx:id=\"progressLabel\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert updateLink != null : "fx:id=\"updateLink\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert versionLabel != null : "fx:id=\"versionLabel\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert workOfflineCheckbox != null : "fx:id=\"workOfflineCheckbox\" was not injected: check your FXML file 'MainWindow.fxml'.";

		// Initialize your logic here: all @FXML variables will have been
		// injected

		enableSnapshotsCheckbox.setSelected(Boolean.parseBoolean(prefs.getPreference(enableSnapshotsPrefKey, "false")));
		try {
			versionLabel.setText(new Version(Common.getAppVersion(), Common.getBuildNumber()).toString(false));
		} catch (IllegalArgumentException e) {
			versionLabel.setText(Common.UNKNOWN_APP_VERSION);
		}

		progressBar.setVisible(false);
		progressLabel.setVisible(false);

		// Disable multiselect
		appList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// Selection change listener
		appList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				updateLaunchButton();
			}
		});

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

					apps = App.getAppList();

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

	private void updateLaunchButton() {
		launchButton.setDisable(true);
		Thread getAppStatus = new Thread() {
			@Override
			public void run() {
				App app = apps.get(appList.getSelectionModel().getSelectedIndex());
				boolean progressVisibleBefore = progressLabel.isVisible();
				Platform.runLater(new Runnable(){
					@Override
					public void run(){
						progressLabel.setVisible(true);
						progressBar.setVisible(true);
						progressBar.setProgress(-1);
						progressLabel.setText(bundle.getString("progress.checkingVersionInfo"));
					}
				});
				
				try {
					if (!workOfflineCheckbox.isSelected()) {
						// downloads are enabled
						if (app.downloadRequired(enableSnapshotsCheckbox.isSelected())) {
							// download required
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									launchButton.setDisable(false);
									launchButton.setText(bundle.getString("okButton.downloadAndLaunch"));
								}
							});
						} else if (app.updateAvailable(enableSnapshotsCheckbox.isSelected())) {
							// Update available
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									launchButton.setDisable(false);
									launchButton.setText(bundle.getString("okButton.updateAndLaunch"));
								}
							});
						} else {
							// Can launch immediately
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									launchButton.setDisable(false);
									launchButton.setText(bundle.getString("okButton.launch"));
								}
							});
						}
					} else {
						// downloads disabled
						if (app.downloadRequired(enableSnapshotsCheckbox.isSelected())) {
							// download required but disabled
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									launchButton.setDisable(true);
									launchButton.setText(bundle.getString("okButton.downloadAndLaunch"));
								}
							});
						} else {
							// Can launch immediately
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									launchButton.setDisable(false);
									launchButton.setText(bundle.getString("okButton.launch"));
								}
							});
						}
					}
				} catch (JDOMException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Platform.runLater(new Runnable(){
					@Override
					public void run(){
						progressLabel.setVisible(progressVisibleBefore);
						progressBar.setVisible(progressVisibleBefore);
					}
				});
			}
		};

		getAppStatus.setName("getAppStatus");
		getAppStatus.start();
	}

	@Override
	public void hide() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				stage.hide();
			}
		});
	}

	// Handler for CheckBox[fx:id="enableSnapshotsCheckbox"] onAction
	@FXML
	void enableSnapshotsCheckboxOnAction(ActionEvent event) {
		updateLaunchButton();
		prefs.setPreference(enableSnapshotsPrefKey, Boolean.toString(enableSnapshotsCheckbox.isSelected()));
	}

	@Override
	public void preparePhaseStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				launchButton.setDisable(false);
				launchButton.setText(bundle.getString("okButton.cancelLaunch"));
				progressBar.setVisible(true);
				progressBar.setProgress(0 / 4.0);
				progressLabel.setVisible(true);
				progressLabel.setText(bundle.getString("progress.preparing"));
			}

		});
	}

	@Override
	public void downloadStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgress(-1);
				progressLabel.setText(bundle.getString("progress.downloading"));
			}

		});
	}

	@Override
	public void installStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgress(2 / 4.0);
				progressLabel.setText(bundle.getString("progress.installing"));
			}

		});
	}

	@Override
	public void launchStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgress(3 / 4.0);
				progressLabel.setText(bundle.getString("progress.launching"));
			}

		});
	}

	public void showErrorMessage(String message) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Alert alert = new Alert(Alert.AlertType.ERROR, message);
				alert.show();
			}
		});
	}

	@Override
	public void operationCanceled() {
		progressBar.setVisible(false);
		progressLabel.setVisible(false);
		updateLaunchButton();
	}

	@Override
	public void cancelRequested() {
		progressBar.setProgress(-1);
		progressLabel.setText(bundle.getString("cancelRequested"));
		launchButton.setDisable(true);
	}

}

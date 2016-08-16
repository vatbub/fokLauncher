package view;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.jdom2.JDOMException;

import applist.App;
import applist.AppList;
import common.*;
import extended.VersionMenuItem;
import javafx.application.*;
import javafx.beans.binding.Bindings;
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
import logging.FOKLogger;
import view.updateAvailableDialog.UpdateAvailableDialog;

public class MainWindow extends Application implements HidableUpdateProgressDialog {

	private static FOKLogger log;

	public static void main(String[] args) {
		launch(args);
	}

	private ResourceBundle bundle = ResourceBundle.getBundle("view.MainWindow");
	private static Prefs prefs;
	private static String enableSnapshotsPrefKey = "enableSnapshots";
	private static AppList apps;
	private static Stage stage;
	private static Thread downloadAndLaunchThread = new Thread();
	private static boolean launchSpecificVersionMenuCanceled = false;

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
		// Currently not used
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
						app.downloadIfNecessaryAndLaunch(enableSnapshotsCheckbox.isSelected(), gui,
								workOfflineCheckbox.isSelected());
					} catch (IOException | JDOMException e) {
						gui.showErrorMessage("An error occurred: " + e.getMessage());
						log.getLogger().log(Level.SEVERE, "An error occurred", e);
					}
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
			log = new FOKLogger(MainWindow.class.getName());
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
			log.getLogger().log(Level.SEVERE, "An error occurred", e);
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

		if (!Internet.isConnected()) {
			workOfflineCheckbox.setSelected(true);
			workOfflineCheckbox.setDisable(true);
		}

		MainWindow gui = this;

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

					// Build the context menu
					appList.setCellFactory(lv -> {

						ListCell<String> cell = new ListCell<>();

						ContextMenu contextMenu = new ContextMenu();

						Menu launchSpecificVersionItem = new Menu();
						launchSpecificVersionItem.textProperty()
								.bind(Bindings.format(bundle.getString("launchSpecificVersion"), cell.itemProperty()));

						MenuItem dummyVersion = new MenuItem();
						dummyVersion.setText(bundle.getString("waitForVersionList"));
						launchSpecificVersionItem.getItems().add(dummyVersion);
						launchSpecificVersionItem.setOnHiding(event2 -> {
							launchSpecificVersionMenuCanceled = true;
						});
						launchSpecificVersionItem.setOnShown(event -> {
							launchSpecificVersionMenuCanceled = false;
							Thread buildContextMenuThread = new Thread() {
								@Override
								public void run() {
									log.getLogger().info("Getting available online versions...");
									App app = apps.get(cell.getIndex());

									// Get available versions
									VersionList verList = new VersionList();
									if (!workOfflineCheckbox.isSelected()) {
										// Online mode enabled
										try {
											verList = app.getAllOnlineVersions();
											if (enableSnapshotsCheckbox.isSelected()) {
												verList.add(app.getLatestOnlineSnapshotVersion());
											}
										} catch (Exception e) {
											// Something happened, pretend
											// offline mode
											verList = app.getCurrentlyInstalledVersions();
										}

									} else {
										// Offline mode enabled
										verList = app.getCurrentlyInstalledVersions();
									}

									// Clear previous list
									Platform.runLater(new Runnable() {

										@Override
										public void run() {
											launchSpecificVersionItem.getItems().clear();
										}
									});

									for (Version ver : verList) {
										VersionMenuItem menuItem = new VersionMenuItem();
										menuItem.setVersion(ver);
										menuItem.setText(ver.toString(false));
										menuItem.setOnAction(event2 -> {
											// App app =
											// apps.get(appList.getSelectionModel().getSelectedIndex());

											// Launch the download
											downloadAndLaunchThread = new Thread() {
												@Override
												public void run() {
													try {
														apps.get(appList.getSelectionModel().getSelectedIndex())
																.downloadIfNecessaryAndLaunch(gui,
																		menuItem.getVersion(),
																		workOfflineCheckbox.isSelected());
													} catch (IOException | JDOMException e) {
														gui.showErrorMessage("An error occurred: " + e.getMessage());
														log.getLogger().log(Level.SEVERE, "An error occurred", e);
													}
												}
											};

											downloadAndLaunchThread.setName("downloadAndLaunchThread");
											downloadAndLaunchThread.start();
										});
										Platform.runLater(new Runnable() {

											@Override
											public void run() {
												launchSpecificVersionItem.getItems().add(menuItem);
											}
										});
									}
									Platform.runLater(new Runnable() {

										@Override
										public void run() {
											if (!launchSpecificVersionMenuCanceled) {
												launchSpecificVersionItem.hide();
												launchSpecificVersionItem.show();
											}
										}
									});
								}
							};

							if (!apps.get(cell.getIndex()).isSpecificVersionListLoaded()) {
								buildContextMenuThread.setName("buildContextMenuThread");
								buildContextMenuThread.start();
								apps.get(cell.getIndex()).setSpecificVersionListLoaded(true);
							}
						});

						Menu deleteItem = new Menu();
						deleteItem.textProperty()
								.bind(Bindings.format(bundle.getString("deleteVersion"), cell.itemProperty()));
						MenuItem dummyVersion2 = new MenuItem();
						dummyVersion2.setText(bundle.getString("waitForVersionList"));
						deleteItem.getItems().add(dummyVersion2);

						deleteItem.setOnShown(event -> {
							App app = apps.get(cell.getIndex());

							if (!app.isDeletableVersionListLoaded()) {
								// Get deletable versions
								app.setDeletableVersionListLoaded(true);
								log.getLogger().info("Getting deletable versions...");
								deleteItem.getItems().clear();

								VersionList verList = new VersionList();
								verList = app.getCurrentlyInstalledVersions();

								for (Version ver : verList) {
									VersionMenuItem menuItem = new VersionMenuItem();
									menuItem.setVersion(ver);
									menuItem.setText(ver.toString(false));
									menuItem.setOnAction(event2 -> {
										// App app =
										// apps.get(appList.getSelectionModel().getSelectedIndex());

										// Delete the file
										try {
											apps.get(appList.getSelectionModel().getSelectedIndex())
													.delete(menuItem.getVersion());
										} catch (IOException e) {
											log.getLogger().log(Level.SEVERE, "An error occurred", e);
										} finally {
											updateLaunchButton();
										}
										// Update the list the next time the
										// user opens it as it has changed
										app.setDeletableVersionListLoaded(false);

									});
									Platform.runLater(new Runnable() {

										@Override
										public void run() {
											deleteItem.getItems().add(menuItem);
										}
									});
								}
								Platform.runLater(new Runnable() {

									@Override
									public void run() {
										deleteItem.hide();
										deleteItem.show();
									}
								});
							}
						});

						contextMenu.getItems().addAll(launchSpecificVersionItem, deleteItem);

						cell.textProperty().bind(cell.itemProperty());

						cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
							if (isNowEmpty) {
								cell.setContextMenu(null);
							} else {
								cell.setContextMenu(contextMenu);
							}
						});
						return cell;
					});

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							appList.setItems(items);
							appList.setDisable(false);
						}

					});

				} catch (JDOMException | IOException e) {
					log.getLogger().log(Level.SEVERE, "An error occurred", e);
					gui.showErrorMessage(e.getMessage(), true);
				}
			}
		};

		getAppListThread.setName("getAppListThread");
		getAppListThread.start();
	}

	private void updateLaunchButton() {
		apps.reloadContextMenuEntriesOnShow();

		Thread getAppStatus = new Thread() {
			@Override
			public void run() {
				App app = apps.get(appList.getSelectionModel().getSelectedIndex());
				boolean progressVisibleBefore = progressLabel.isVisible();
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						launchButton.setDisable(true);
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
					log.getLogger().log(Level.SEVERE, "An error occurred", e);
				}

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						progressLabel.setVisible(progressVisibleBefore);
						progressBar.setVisible(progressVisibleBefore);
					}
				});
			}

		};

		// Only update the button caption if no download is running
		if (!downloadAndLaunchThread.isAlive()) {
			getAppStatus.setName("getAppStatus");
			getAppStatus.start();
		}
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
				appList.setDisable(true);
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
				progressBar.setProgress(1.0 / 2.0);
				progressLabel.setText(bundle.getString("progress.installing"));
			}

		});
	}

	@Override
	public void launchStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgress(2.0 / 2.0);
				progressLabel.setText(bundle.getString("progress.launching"));
			}

		});
	}

	public void showErrorMessage(String message) {
		showErrorMessage(message, false);
	}

	public void showErrorMessage(String message, boolean closeWhenDialogIsClosed) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Alert alert = new Alert(Alert.AlertType.ERROR, message);
				alert.show();

				Thread t = new Thread() {
					@Override
					public void run() {
						while (alert.isShowing()) {
							// wait for dialog to be closed
						}

						System.err.println("Closing app after exception, good bye...");
						Platform.exit();
					}
				};

				t.start();
			}
		});
	}

	@Override
	public void operationCanceled() {
		appList.setDisable(false);
		progressBar.setVisible(false);
		progressLabel.setVisible(false);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateLaunchButton();
			}
		});
	}

	@Override
	public void cancelRequested() {
		progressBar.setProgress(-1);
		progressLabel.setText(bundle.getString("cancelRequested"));
		launchButton.setDisable(true);
	}

	@Override
	public void downloadProgressChanged(double kilobytesDownloaded, double totalFileSizeInKB) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgress(kilobytesDownloaded / totalFileSizeInKB);
			}

		});
	}

}

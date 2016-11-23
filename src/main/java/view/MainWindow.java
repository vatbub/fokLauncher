package view;

import applist.App;
import applist.AppList;
import com.rometools.rome.io.FeedException;
import com.sun.glass.ui.Robot;
import common.*;
import extended.CustomListCell;
import extended.GuiLanguage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import logging.FOKLogger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jdom2.JDOMException;
import view.motd.MOTD;
import view.motd.MOTDDialog;
import view.updateAvailableDialog.UpdateAvailableDialog;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

public class MainWindow extends Application implements HidableUpdateProgressDialog {

	private static FOKLogger log;
	public static AppConfig appConfig;
	private static ImageView linkIconView = new ImageView(
			new Image(MainWindow.class.getResourceAsStream("link_gray.png")));
    private static ImageView optionIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("menu_gray.png")));

	public static void main(String[] args) {
		common.Common.setAppName("foklauncher");
		log = new FOKLogger(MainWindow.class.getName());
		prefs = new Prefs(MainWindow.class.getName());

		boolean autoLaunchApp = false;
		URL autoLaunchRepoURL = null;
		URL autoLaunchSnapshotRepoURL = null;
		String autoLaunchGroupId = null;
		String autoLaunchArtifactId = null;
		String autoLaunchClassifier = null;

		// Complete the update
		UpdateChecker.completeUpdate(args);

		for (String arg : args) {
			if (arg.toLowerCase().matches("mockappversion=.*")) {
				// Set the mock version
				String version = arg.substring(arg.toLowerCase().indexOf('=') + 1);
				Common.setMockAppVersion(version);
			} else if (arg.toLowerCase().matches("mockbuildnumber=.*")) {
				// Set the mock build number
				String buildnumber = arg.substring(arg.toLowerCase().indexOf('=') + 1);
				Common.setMockBuildNumber(buildnumber);
			} else if (arg.toLowerCase().matches("mockpackaging=.*")) {
				// Set the mock packaging
				String packaging = arg.substring(arg.toLowerCase().indexOf('=') + 1);
				Common.setMockPackaging(packaging);
			} else if (arg.toLowerCase().matches(".*launch")) {
				autoLaunchApp = true;
			} else if (arg.toLowerCase().matches("autolaunchrepourl=.*")) {
				if (autoLaunchApp) {
					try {
						autoLaunchRepoURL = new URL(arg.substring(arg.indexOf('=') + 1));
					} catch (MalformedURLException e) {
						log.getLogger().log(Level.SEVERE,
								"Ignoring argument autoLaunchRepoURL due to MalformedURLException", e);
					}
				} else {
					log.getLogger().severe(
							"autoLaunchRepoURL argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
				}
			} else if (arg.toLowerCase().matches("autolaunchsnapshotrepourl=.*")) {
				if (autoLaunchApp) {
					try {
						autoLaunchSnapshotRepoURL = new URL(arg.substring(arg.indexOf('=') + 1));
					} catch (MalformedURLException e) {
						log.getLogger().log(Level.SEVERE,
								"Ignoring argument autoLaunchSnapshotRepoURL due to MalformedURLException", e);
					}
				} else {
					log.getLogger().severe(
							"autoLaunchSnapshotRepoURL argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
				}
			} else if (arg.toLowerCase().matches("autolaunchgroupid=.*")) {
				if (autoLaunchApp) {
					autoLaunchGroupId = arg.substring(arg.indexOf('=') + 1);
				} else {
					log.getLogger().severe(
							"autoLaunchGroupId argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
				}
			} else if (arg.toLowerCase().matches("autolaunchartifactid=.*")) {
				if (autoLaunchApp) {
					autoLaunchArtifactId = arg.substring(arg.indexOf('=') + 1);
				} else {
					log.getLogger().severe(
							"autoLaunchArtifactId argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
				}
			} else if (arg.toLowerCase().matches("autolaunchclassifier=.*")) {
				if (autoLaunchApp) {
					autoLaunchClassifier = arg.substring(arg.indexOf('=') + 1);
				} else {
					log.getLogger().severe(
							"autoLaunchClassifier argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
				}
			}
		}

		if (autoLaunchApp) {
			if (autoLaunchRepoURL == null || autoLaunchSnapshotRepoURL == null || autoLaunchGroupId == null
					|| autoLaunchArtifactId == null) {
				// not sufficient info specified
				log.getLogger().severe("Cannot auto-launch app as unsufficient download info was specified.");
			} else {
				if (autoLaunchClassifier == null) {
					// No classifier specified
					appForAutoLaunch = new App("autoLaunchApp", autoLaunchRepoURL, autoLaunchSnapshotRepoURL,
							autoLaunchGroupId, autoLaunchArtifactId);
				} else {
					// Classifier specified
					appForAutoLaunch = new App("autoLaunchApp", autoLaunchRepoURL, autoLaunchSnapshotRepoURL,
							autoLaunchGroupId, autoLaunchArtifactId, autoLaunchClassifier);
				}
			}
		}

		launch(args);
	}

	public static ResourceBundle bundle;
	private static Prefs prefs;
	private static final String enableSnapshotsPrefKey = "enableSnapshots";
	private static final String showLauncherAgainPrefKey = "showLauncherAgain";
	private static final String guiLanguagePrefKey = "guiLanguage";
	private static AppList apps;
	public static Stage stage;
	public static Thread downloadAndLaunchThread = new Thread();
	public static boolean launchSpecificVersionMenuCanceled = false;
	private static Locale systemDefaultLocale;
	private Date latestProgressBarUpdate = Date.from(Instant.now());
	private static App appForAutoLaunch = null;

	private Runnable getAppListRunnable = new Runnable() {
		@Override
		public void run() {
			try {

				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						appList.setItems(FXCollections.observableArrayList());
						appList.setDisable(true);
						appList.setPlaceholder(new Label(bundle.getString("WaitForAppList")));
					}

				});

				apps = App.getAppList();

				ObservableList<App> items = FXCollections.observableArrayList();
				FilteredList<App> filteredData = new FilteredList<>(items, s -> true);

				for (App app : apps) {
					items.add(app);
				}

				// Add filter functionality
				searchField.textProperty().addListener(obs -> {
					String filter = searchField.getText();
					if (filter == null || filter.length() == 0) {
						filteredData.setPredicate(s -> true);
					} else {
						filteredData.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
					}
				});

				// Build the context menu
				appList.setCellFactory(lv -> {

					CustomListCell<App> cell = new CustomListCell<App>();

					cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
						if (isNowEmpty) {
							cell.setContextMenu(null);
						} else {
							// cell.setContextMenu(contextMenu);
                            cell.setContextMenu(cell.getItem().getContextMenu());
						}
					});
					return cell;
				});

				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						appList.setItems(filteredData);
						appList.setPlaceholder(new Label(bundle.getString("emptyAppList")));

						// Only enable if no download is running
						if (downloadAndLaunchThread == null) {
							appList.setDisable(false);
						} else if (!downloadAndLaunchThread.isAlive()) {
							appList.setDisable(false);
						}
					}

				});

			} catch (JDOMException | IOException e) {
				log.getLogger().log(Level.SEVERE, "An error occurred", e);
				currentMainWindowInstance
						.showErrorMessage("An error occurred: \n" + e.getClass().getName() + "\n" + e.getMessage());
			}
		}
	};

	/**
	 * The thread that gets the app list
	 */
	private Thread getAppListThread;

	/**
	 * This reference always refers to the currently used instance of the
	 * MainWidow. The purpose of this field that {@code this} can be accessed in
	 * a convenient way in static methods.
	 */
	public static MainWindow currentMainWindowInstance;

	private static App currentlySelectedApp = null;

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="appList"
	private ListView<App> appList; // Value injected by FXMLLoader

	@FXML // fx:id="searchField"
	private TextField searchField; // Value injected by FXMLLoader

	@FXML // fx:id="enableSnapshotsCheckbox"
	private CheckBox enableSnapshotsCheckbox; // Value injected by FXMLLoader

    /**
     * Returns {@code true} if snapshots are enabled
     * @return {@code true} if snapshots are enabled
     */
    public boolean snapshotsEnabled(){
        return enableSnapshotsCheckbox.isSelected();
    }

	@FXML // fx:id="launchButton"
	private ProgressButton launchButton; // Value injected by FXMLLoader

	@FXML
	private Button optionButton;

	@FXML // fx:id="linkButton"
	private Button linkButton; // Value injected by FXMLLoader

	@FXML // fx:id="launchLauncherAfterAppExitCheckbox"
	public CheckBox launchLauncherAfterAppExitCheckbox; // Value injected by
															// FXMLLoader

	@FXML // fx:id="languageSelector"
	private ComboBox<GuiLanguage> languageSelector; // Value injected by
													// FXMLLoader

	@FXML // fx:id="progressBar"
	private CustomProgressBar progressBar; // Value injected by FXMLLoader

	@FXML // fx:id="workOfflineCheckbox"
	private CheckBox workOfflineCheckbox; // Value injected by FXMLLoader

    /**
     * Returns {@code true} if the app needs to work offline
     * @return {@code true} if the app needs to work offline
     */
    public boolean workOffline(){
        return workOfflineCheckbox.isSelected();
    }

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

	@FXML // fx:id="settingsGridView"
	private GridPane settingsGridView; // Value injected by FXMLLoader

	@FXML // fx:id="appInfoButton"
	private Button appInfoButton; // Value injected by FXMLLoader

	// Handler for ListView[fx:id="appList"] onMouseClicked
	@FXML
	void appListOnMouseClicked(MouseEvent event) {
		// Currently not used
	}

	// Handler for AnchorPane[id="AnchorPane"] onDragDetected
	@FXML
	void appListOnDragDetected(MouseEvent event) {
		if (currentlySelectedApp != null) {
			File tempFile = new File(Common.getAndCreateAppDataPath() + currentlySelectedApp.getName() + ".lnk");
			try {
				currentlySelectedApp.createShortCut(tempFile, bundle.getString("shortcutQuickInfo"));
				Dragboard db = appList.startDragAndDrop(TransferMode.MOVE);
				ClipboardContent content = new ClipboardContent();
				content.putFiles(Arrays.asList(tempFile));
				db.setContent(content);
			} catch (IOException e) {
				log.getLogger().log(Level.SEVERE, "An error occurred", e);
			}
		}
	}

	// Handler for ProgressButton[id="launchButton"] onDragDetected
	@FXML
	void launchButtonOnDragDetected(MouseEvent event) {
		appListOnDragDetected(event);
	}

	// Handler for ProgressButton[id="linkButton"] onDragDetected
	@FXML
	void linkButtonOnDragDetected(MouseEvent event) {
		appListOnDragDetected(event);
		linkButton.setCursor(Cursor.OPEN_HAND);
	}

	// Handler for ProgressButton[id="linkButton"] onMousePressed
	@FXML
	void linkButtonOnMousePressed(MouseEvent event) {
		linkButton.setCursor(Cursor.CLOSED_HAND);
	}

	// Handler for ProgressButton[id="linkButton"] onMouseReleased
	@FXML
	void linkButtonOnMouseReleased(MouseEvent event) {
		linkButton.setCursor(Cursor.OPEN_HAND);
	}

	@FXML
	void optionButtonOnAction (ActionEvent event){
        Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();

        double x = robot.getMouseX();
        double y = robot.getMouseY();

        currentlySelectedApp.getContextMenu().show(optionButton, x, y);
	}

	// Handler for ListView[fx:id="appList"] onDragOver
	@FXML
	void mainFrameOnDragOver(DragEvent event) {
		Dragboard db = event.getDragboard();
		// Only allow drag'n'drop for files and if no app list is currently
		// loading
		if (db.hasFiles() && !getAppListThread.isAlive()) {
			// Don't accept the drag if any file contained in the drag does not
			// have the *.foklauncher extension
			for (File f : db.getFiles()) {
				if (!FilenameUtils.getExtension(f.getAbsolutePath()).equals("foklauncher")) {
					event.consume();
					return;
				}
			}

			event.acceptTransferModes(TransferMode.LINK);
		} else {
			event.consume();
		}
	}

	@FXML
	void mainFrameOnDragDropped(DragEvent event) {
		List<File> files = event.getDragboard().getFiles();

		for (File f : files) {
			log.getLogger().info("Importing app from " + f.getAbsolutePath() + "...");
			try {
				App.addImportedApp(f);
				currentMainWindowInstance.loadAppList();
			} catch (IOException e) {
				log.getLogger().log(Level.SEVERE, "An error occurred", e);
				currentMainWindowInstance.showErrorMessage(e.toString(), false);
			}
		}
	}

	public static Runnable showLauncherAgain = new Runnable() {
		@Override
		public void run() {

			// reset the ui
			try {
				currentMainWindowInstance.start(stage);
			} catch (Exception e) {
				log.getLogger().log(Level.INFO,
						"An error occurred while firing a handler for the LaunchedAppExited event, trying to run the handler using Platform.runLater...",
						e);
			}
			Platform.setImplicitExit(true);
		}
	};

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
				UpdateInfo update = UpdateChecker.isUpdateAvailableCompareAppVersion(AppConfig.getUpdateRepoBaseURL(),
						AppConfig.groupID, AppConfig.artifactID, AppConfig.getUpdateFileClassifier(),
						Common.getPackaging());
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

	@FXML
	void languageSelectorOnAction(ActionEvent event) {
		log.getLogger().info("Switching gui language to: "
				+ languageSelector.getItems().get(languageSelector.getSelectionModel().getSelectedIndex()));
		prefs.setPreference(guiLanguagePrefKey, languageSelector.getItems()
				.get(languageSelector.getSelectionModel().getSelectedIndex()).getLocale().getLanguage());

		// Restart gui
		boolean implicitExit = Platform.isImplicitExit();
		Platform.setImplicitExit(false);
		stage.hide();
		try {
			currentMainWindowInstance.start(stage);
		} catch (Exception e) {
			log.getLogger().log(Level.INFO, "An error occurred while setting a new gui language", e);
		}
		Platform.setImplicitExit(implicitExit);
	}

	// Handler for Button[fx:id="launchButton"] onAction
	@FXML
	void launchButtonOnAction(ActionEvent event) {
		MainWindow gui = this;

		if (!downloadAndLaunchThread.isAlive()) {
			// Launch the download
			downloadAndLaunchThread = new Thread() {
				@Override
				public void run() {
					try {
						// Attach the on app exit handler if required
						if (launchLauncherAfterAppExitCheckbox.isSelected()) {
							currentlySelectedApp.addEventHandlerWhenLaunchedAppExits(showLauncherAgain);
						} else {
							currentlySelectedApp.removeEventHandlerWhenLaunchedAppExits(showLauncherAgain);
						}

						currentlySelectedApp.downloadIfNecessaryAndLaunch(enableSnapshotsCheckbox.isSelected(), gui,
								workOfflineCheckbox.isSelected());
					} catch (IOException | JDOMException e) {
						gui.showErrorMessage("An error occurred: \n" + e.getClass().getName() + "\n" + e.getMessage());
						log.getLogger().log(Level.SEVERE, "An error occurred", e);
					}
				}
			};

			downloadAndLaunchThread.setName("downloadAndLaunchThread");
			downloadAndLaunchThread.start();
		} else {
			currentlySelectedApp.cancelDownloadAndLaunch(gui);
		}
	}

	@FXML
	void linkButtonOnAction(ActionEvent event) {
		log.getLogger().info("Creating shortcut using linkButton...");
		File file = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator
				+ currentlySelectedApp.getName() + ".lnk");
		try {
			log.getLogger().info("Creating shortcut for app " + currentlySelectedApp.getName()
					+ " at the following location: " + file.getAbsolutePath());
			currentlySelectedApp.createShortCut(file, bundle.getString("shortcutQuickInfo"));

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					Alert alert = new Alert(Alert.AlertType.INFORMATION,
							bundle.getString("shortcutCreatedMessage").replace("%s", currentlySelectedApp.getName()));
					alert.show();

					Thread t = new Thread() {
						@Override
						public void run() {
							while (alert.isShowing()) {
								// wait for dialog to be closed
							}
						}
					};

					t.setName("showErrorThread");
					t.start();
				}
			});
		} catch (Exception e) {
			// Add message about debugging environment
			String guiString = e.toString();
			if (e instanceof NullPointerException) {
				guiString = guiString
						+ "\n\nYou are probably in a development environment where linking does not work (where shall I link to? Package the source code into a jar file using the command \n\nmvn package\n\nand then retry.";
			}
			log.getLogger().log(Level.SEVERE, "An error occurred", e);
			currentMainWindowInstance.showErrorMessage(guiString);
		}
	}

	// Handler for CheckBox[fx:id="workOfflineCheckbox"] onAction
	@FXML
	void workOfflineCheckboxOnAction(ActionEvent event) {
		updateLaunchButton();
	}

	// Handler for CheckBox[fx:id="launchLauncherAfterAppExitCheckbox"] onAction
	@FXML
	void launchLauncherAfterAppExitCheckboxOnAction(ActionEvent event) {
		prefs.setPreference(showLauncherAgainPrefKey,
				Boolean.toString(launchLauncherAfterAppExitCheckbox.isSelected()));
	}

	@FXML
	void appInfoButtonOnAction(ActionEvent event) {
		try {
			Desktop.getDesktop().browse(new URI(currentlySelectedApp.getAdditionalInfoURL().toString()));
		} catch (IOException | URISyntaxException e) {
			log.getLogger().log(Level.SEVERE, "An error occurred", e);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// get the right resource bundle
		String guiLanguageCode = prefs.getPreference(guiLanguagePrefKey, "");

		if (guiLanguageCode.equals("")) {
			if (systemDefaultLocale != null) {
				Locale.setDefault(systemDefaultLocale);
			}
		} else {
			// Get the specified bundle
			if (systemDefaultLocale == null) {
				systemDefaultLocale = Locale.getDefault();
			}
			log.getLogger().info("Setting language: " + guiLanguageCode);
			Locale.setDefault(new Locale(guiLanguageCode));
		}

		bundle = ResourceBundle.getBundle("view.MainWindow");

		// appConfig = new Config();

		stage = primaryStage;
		try {
			Thread updateThread = new Thread() {
				@Override
				public void run() {
					UpdateInfo update = UpdateChecker.isUpdateAvailable(AppConfig.getUpdateRepoBaseURL(),
							AppConfig.groupID, AppConfig.artifactID, AppConfig.getUpdateFileClassifier(),
							Common.getPackaging());
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
			scene.getStylesheets().add(getClass().getResource("MainWindow.css").toExternalForm());

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

	@Override
	public void stop() {
		try {
			UpdateChecker.cancelUpdateCompletion();
			if (currentlySelectedApp != null) {
				currentlySelectedApp.cancelDownloadAndLaunch(this);
			}
		} catch (Exception e) {
			log.getLogger().log(Level.SEVERE,
					"An error occurred but is not relevant as we are currently in the shutdown process. Possible reasons for this exception are: You tried to modify a view but it is not shown any more on the screen; You tried to cancel the app download but no download was in progress.",
					e);
		}
	}

	@FXML // This method is called by the FXMLLoader when initialization is
			// complete
	void initialize() {
		assert launchLauncherAfterAppExitCheckbox != null : "fx:id=\"launchLauncherAfterAppExitCheckbox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert languageSelector != null : "fx:id=\"languageSelector\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert searchField != null : "fx:id=\"searchField\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert workOfflineCheckbox != null : "fx:id=\"workOfflineCheckbox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert launchButton != null : "fx:id=\"launchButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert versionLabel != null : "fx:id=\"versionLabel\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert appList != null : "fx:id=\"appList\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert appInfoButton != null : "fx:id=\"appInfoButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert enableSnapshotsCheckbox != null : "fx:id=\"enableSnapshotsCheckbox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert updateLink != null : "fx:id=\"updateLink\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert linkButton != null : "fx:id=\"linkButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert settingsGridView != null : "fx:id=\"settingsGridView\" was not injected: check your FXML file 'MainWindow.fxml'.";

		// Initialize your logic here: all @FXML variables will have been
		// injected

		// add icon to linkButton and optionButton
		linkButton.setGraphic(linkIconView);
        optionButton.setGraphic(optionIconView);

		// Bind the disabled property of the launchButton to the linkButton
		linkButton.disableProperty().bind(launchButton.disableProperty());

		// show gey icon when disabled
		linkButton.disableProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue.booleanValue() == true) {
					// disabled, select gray icon
					linkIconView.setImage(new Image(MainWindow.class.getResourceAsStream("link_gray.png")));
				} else {
					// enabled, select blue icon
					linkIconView.setImage(new Image(MainWindow.class.getResourceAsStream("link.png")));
				}
			}
		});

        optionButton.disableProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue.booleanValue() == true) {
                    // disabled, select gray icon
                    optionIconView.setImage(new Image(MainWindow.class.getResourceAsStream("menu_gray.png")));
                } else {
                    // enabled, select blue icon
                    optionIconView.setImage(new Image(MainWindow.class.getResourceAsStream("menu.png")));
                }
            }
        });

        optionButton.disableProperty().bind(launchButton.disableProperty());

		// TODO Add icon source
		// <div>Icons made by <a
		// href="http://www.flaticon.com/authors/simpleicon"
		// title="SimpleIcon">SimpleIcon</a> from <a
		// href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a>
		// is licensed by <a href="http://creativecommons.org/licenses/by/3.0/"
		// title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

		// Show messages of the day
		Thread motdThread = new Thread() {
			@Override
			public void run() {
				MOTD motd;
				try {
					motd = MOTD.getLatestMOTD(AppConfig.getMotdFeedUrl());
					if (!motd.isMarkedAsRead()) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								new MOTDDialog(motd, motd.getEntry().getTitle());
							}
						});
					}
				} catch (IllegalArgumentException | FeedException | IOException | ClassNotFoundException e) {
					log.getLogger().log(Level.SEVERE, "An error occurred", e);
				}
			}
		};
		motdThread.setName("motdThread");
		motdThread.start();

		currentMainWindowInstance = this;

		enableSnapshotsCheckbox.setSelected(Boolean.parseBoolean(prefs.getPreference(enableSnapshotsPrefKey, "false")));
		launchLauncherAfterAppExitCheckbox
				.setSelected(Boolean.parseBoolean(prefs.getPreference(showLauncherAgainPrefKey, "false")));

		try {
			versionLabel.setText(new Version(Common.getAppVersion(), Common.getBuildNumber()).toString(false));
		} catch (IllegalArgumentException e) {
			versionLabel.setText(Common.UNKNOWN_APP_VERSION);
		}

		progressBar.setVisible(false);

		// Disable multiselect
		appList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		loadAvailableGuiLanguages();

		// Selection change listener
		appList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<App>() {
			public void changed(ObservableValue<? extends App> observable, App oldValue, App newValue) {
				try {
					currentlySelectedApp = appList.getSelectionModel().getSelectedItem();
				} catch (ArrayIndexOutOfBoundsException e) {
					currentlySelectedApp = null;
				}
				updateLaunchButton();
			}
		});

		if (!Internet.isConnected()) {
			workOfflineCheckbox.setSelected(true);
			workOfflineCheckbox.setDisable(true);
		}

		loadAppList();

		// auto launch app if one was specified
		if (appForAutoLaunch != null) {
			MainWindow gui = this;
			currentlySelectedApp = appForAutoLaunch;

			// Launch the download
			downloadAndLaunchThread = new Thread() {
				@Override
				public void run() {
					try {
						appForAutoLaunch.downloadIfNecessaryAndLaunch(enableSnapshotsCheckbox.isSelected(), gui,
								workOfflineCheckbox.isSelected());
					} catch (Exception e) {
						gui.showErrorMessage("An error occurred: \n" + e.getClass().getName() + "\n" + e.getMessage());
						log.getLogger().log(Level.SEVERE, "An error occurred", e);
					} finally {
						// Clean up
						appForAutoLaunch = null;
					}
				}
			};

			downloadAndLaunchThread.setName("downloadAndLaunchThread");
			downloadAndLaunchThread.start();
		}
	}

	private void loadAvailableGuiLanguages() {
		List<Locale> supportedGuiLocales = Common.getLanguagesSupportedByResourceBundle(bundle);
		List<GuiLanguage> convertedList = new ArrayList<GuiLanguage>(supportedGuiLocales.size());

		for (Locale lang : supportedGuiLocales) {
			convertedList.add(new GuiLanguage(lang, bundle.getString("langaugeSelector.chooseAutomatically")));
		}
		ObservableList<GuiLanguage> items = FXCollections.observableArrayList(convertedList);
		languageSelector.setItems(items);

		if (Locale.getDefault() != systemDefaultLocale) {
			GuiLanguage langToSelect = null;

			for (GuiLanguage lang : convertedList) {
				if (Locale.getDefault().equals(lang.getLocale())) {
					langToSelect = lang;
				}
			}

			if (langToSelect != null) {
				languageSelector.getSelectionModel().select(langToSelect);
			}
		}
	}

	/**
	 * Loads the app list using the {@link App#getAppList()}-method
	 */
	public void loadAppList() {
		if (getAppListThread != null) {
			// If thread is not null and running, quit
			if (getAppListThread.isAlive()) {
				return;
			}
		}

		// Thread is either null or not running anymore
		getAppListThread = new Thread(getAppListRunnable);
		getAppListThread.setName("getAppListThread");
		getAppListThread.start();
	}

	public void updateLaunchButton() {
		apps.reloadContextMenuEntriesOnShow();

		Thread getAppStatus = new Thread() {
			@Override
			public void run() {
				App checkedApp = currentlySelectedApp;
				boolean progressVisibleBefore = progressBar.isVisible();
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						launchButton.setDisable(true);
						launchButton.setDefaultButton(false);
						launchButton.setStyle("-fx-background-color: transparent;");
						launchButton.setControlText("");
						progressBar.setPrefHeight(launchButton.getHeight());
						progressBar.setVisible(true);
						progressBar.setProgress(-1);
						launchButton.setProgressText(bundle.getString("progress.checkingVersionInfo"));
						appInfoButton.setDisable(true);
					}
				});

				try {
					if (!workOfflineCheckbox.isSelected()) {
						// downloads are enabled

						// enable the additional info button if applicable
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								appInfoButton.setDisable(checkedApp.getAdditionalInfoURL() == null);
							}
						});

						if (checkedApp.downloadRequired(enableSnapshotsCheckbox.isSelected())) {
							// download required
							setLaunchButtonText(checkedApp, false, bundle.getString("okButton.downloadAndLaunch"));
						} else if (checkedApp.updateAvailable(enableSnapshotsCheckbox.isSelected())) {
							// Update available
							setLaunchButtonText(checkedApp, false, bundle.getString("okButton.updateAndLaunch"));
						} else {
							// Can launch immediately
							setLaunchButtonText(checkedApp, false, bundle.getString("okButton.launch"));
						}
					} else {
						// downloads disabled
						if (checkedApp.downloadRequired(enableSnapshotsCheckbox.isSelected())) {
							// download required but disabled
							setLaunchButtonText(checkedApp, true, bundle.getString("okButton.downloadAndLaunch"));
						} else {
							// Can launch immediately
							setLaunchButtonText(checkedApp, false, bundle.getString("okButton.launch"));
						}
					}
				} catch (JDOMException | IOException e) {
					log.getLogger().log(Level.SEVERE, "An error occurred", e);

					// Switch to offline mode
					workOfflineCheckbox.setSelected(true);
					workOfflineCheckbox.setDisable(true);

					// update launch button accordingly
					updateLaunchButton();

					// Show error message
					currentMainWindowInstance.showErrorMessage(
							bundle.getString("updateLaunchButtonException") + "\n\n" + ExceptionUtils.getStackTrace(e),
							false);
				}

				// Do finishing touches to gui only if checkedApp still equals
				// currentlySelectedApp (make sure the user did not change the
				// selection in the meanwhile)
				if (checkedApp == currentlySelectedApp) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							launchButton.setProgressText("");
							progressBar.setVisible(progressVisibleBefore);
						}
					});
				}
			}

		};

		// Only update the button caption if no download is running and an app
		// is selected
		if (!downloadAndLaunchThread.isAlive() && currentlySelectedApp != null) {
			getAppStatus.setName("getAppStatus");
			getAppStatus.start();
		} else if (currentlySelectedApp == null) {
			// disable the button
			launchButton.setDisable(true);
		}
	}

	private void setLaunchButtonText(App checkedApp, boolean isDisabled, String text) {
		// Only update the button if the user did not change his selection
		if (checkedApp == currentlySelectedApp) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					launchButton.setDisable(isDisabled);
					launchButton.setDefaultButton(!isDisabled);
					launchButton.setStyle("");
					launchButton.setControlText(text);
				}
			});
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
				launchButton.setDefaultButton(false);
				progressBar.setPrefHeight(launchButton.getHeight());
				launchButton.setStyle("-fx-background-color: transparent;");
				launchButton.setControlText(bundle.getString("okButton.cancelLaunch"));
				progressBar.setVisible(true);
				progressBar.setProgressAnimated(0 / 4.0);
				launchButton.setProgressText(bundle.getString("progress.preparing"));

				settingsGridView.setDisable(true);
			}

		});
	}

	@Override
	public void downloadStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgress(-1);
				launchButton.setProgressText(bundle.getString("progress.downloading"));
			}

		});
	}

	@Override
	public void installStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgressAnimated(1.0 / 2.0);
				launchButton.setProgressText(bundle.getString("progress.installing"));
			}

		});
	}

	@Override
	public void launchStarted() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressBar.setProgressAnimated(2.0 / 2.0);
				launchButton.setProgressText(bundle.getString("progress.launching"));
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
				String finalMessage;
				if (closeWhenDialogIsClosed) {
					finalMessage = message + "\n\n" + "The app needs to close now.";
				} else {
					finalMessage = message;
				}

				Alert alert = new Alert(Alert.AlertType.ERROR, finalMessage);
				alert.show();

				Thread t = new Thread() {
					@Override
					public void run() {
						while (alert.isShowing()) {
							// wait for dialog to be closed
						}

						if (closeWhenDialogIsClosed) {
							System.err.println("Closing app after exception, good bye...");
							Platform.exit();
						}
					}
				};

				t.setName("showErrorThread");
				t.start();
			}
		});
	}

	@Override
	public void operationCanceled() {
		log.getLogger().info("Operation cancelled.");
		Platform.setImplicitExit(true);
		appList.setDisable(false);
		progressBar.setVisible(false);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				launchButton.setProgressText("");
				settingsGridView.setDisable(false);
				updateLaunchButton();
			}
		});
	}

	@Override
	public void cancelRequested() {
		if (progressBar != null) {
			progressBar.setProgress(-1);
			launchButton.setProgressText(bundle.getString("cancelRequested"));
			launchButton.setDisable(true);
			log.getLogger().info("Requested to cancel the current operation, Cancel in progress...");
		}
	}

	@Override
	public void downloadProgressChanged(double kilobytesDownloaded, double totalFileSizeInKB) {
		double timeThreshold = 500 + Math.random() * 3000;
		if (Math.abs(Date.from(Instant.now()).getTime() - latestProgressBarUpdate.getTime()) >= (timeThreshold)
				|| kilobytesDownloaded == totalFileSizeInKB) {
			latestProgressBarUpdate = Date.from(Instant.now());
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					progressBar.setProgressAnimated(kilobytesDownloaded / totalFileSizeInKB);

					String downloadedString;

					if (kilobytesDownloaded < 1024) {
						downloadedString = Double.toString(Math.round(kilobytesDownloaded * 100.0) / 100.0) + " "
								+ bundle.getString("kilobyte");
					} else if ((kilobytesDownloaded / 1024) < 1024) {
						downloadedString = Double.toString(Math.round((kilobytesDownloaded * 100.0) / 1024) / 100.0)
								+ " " + bundle.getString("megabyte");
					} else if (((kilobytesDownloaded / 1024) / 1024) < 1024) {
						downloadedString = Double
								.toString(Math.round(((kilobytesDownloaded * 100.0) / 1024) / 1024) / 100.0) + " "
								+ bundle.getString("gigabyte");
					} else {
						downloadedString = Double
								.toString(Math.round((((kilobytesDownloaded * 100.0) / 1024) / 1024) / 1024) / 100.0)
								+ " " + bundle.getString("terabyte");
					}

					String totalString;
					if (totalFileSizeInKB < 1024) {
						totalString = Double.toString(Math.round(totalFileSizeInKB * 100.0) / 100.0) + " "
								+ bundle.getString("kilobyte");
					} else if ((totalFileSizeInKB / 1024) < 1024) {
						totalString = Double.toString(Math.round((totalFileSizeInKB * 100.0) / 1024) / 100.0) + " "
								+ bundle.getString("megabyte");
					} else if (((totalFileSizeInKB / 1024) / 1024) < 1024) {
						totalString = Double.toString(Math.round(((totalFileSizeInKB * 100.0) / 1024) / 1024) / 100.0)
								+ " " + bundle.getString("gigabyte");
					} else {
						totalString = Double
								.toString(Math.round((((totalFileSizeInKB * 100.0) / 1024) / 1024) / 1024) / 100.0)
								+ " " + bundle.getString("terabyte");
					}

					launchButton.setProgressText(bundle.getString("progress.downloading") + "(" + downloadedString + "/"
							+ totalString + ")");
				}

			});
		}
	}

}

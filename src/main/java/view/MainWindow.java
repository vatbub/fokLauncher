package view;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import applist.App;
import applist.AppList;
import com.rometools.rome.io.FeedException;
import com.sun.glass.ui.Robot;
import common.*;
import common.internet.Internet;
import extended.CustomListCell;
import extended.GuiLanguage;
import javafx.application.Application;
import javafx.application.Platform;
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
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import org.apache.commons.io.FileUtils;
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
    private static final ImageView linkIconView = new ImageView(
            new Image(MainWindow.class.getResourceAsStream("link_gray.png")));
    private static final ImageView optionIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("menu_gray.png")));
    private static final ImageView infoIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("info_gray.png")));
    private static boolean autoLaunchUseSnapshots;

    /**
     * This reference always refers to the currently used instance of the
     * MainWidow. The purpose of this field that {@code this} can be accessed in
     * a convenient way in static methods.
     */
    public static MainWindow currentMainWindowInstance;

    private static App currentlySelectedApp = null;
    public static ResourceBundle bundle;
    /**
     * {@code true }if this is the first launch after an update
     */
    private static boolean isFirstLaunchAfterUpdate = false;
    private static String firstUpdateMessageTextKey;

    private static final UpdateChecker.CompleteUpdateRunnable firstStartAfterUpdateRunnable = (oldVersion, oldFile) -> {
        isFirstLaunchAfterUpdate = true;

        if (oldVersion == null) {
            // Version was so old that we cannot determine its actual version number so we need to make sure that we can use the current storage model

            // use the old alert message
            firstUpdateMessageTextKey = "firstLaunchAfterUpdateDeletedApps";
            try {
                // delete apps folder
                FOKLogger.info(MainWindow.class.getName(), "Deleting the apps folder after update...");
                FileUtils.deleteDirectory(new File(Common.getAndCreateAppDataPath() + "apps"));
            } catch (Exception e) {
                // Try to log, if it does not work just print the error
                try {
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }
        }else{
            firstUpdateMessageTextKey = "firstLaunchAfterUpdate";
        }
    };

    public static void main(String[] args) {
        common.Common.setAppName("foklauncher");
        FOKLogger.enableLoggingOfUncaughtExceptions();
        prefs = new Prefs(MainWindow.class.getName());

        boolean autoLaunchApp = false;
        URL autoLaunchRepoURL = null;
        URL autoLaunchSnapshotRepoURL = null;
        String autoLaunchGroupId = null;
        String autoLaunchArtifactId = null;
        String autoLaunchClassifier = null;

        // Complete the update
        UpdateChecker.completeUpdate(args, firstStartAfterUpdateRunnable);

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
                        FOKLogger.log(MainWindow.class.getName(), Level.SEVERE,
                                "Ignoring argument autoLaunchRepoURL due to MalformedURLException", e);
                    }
                } else {
                    FOKLogger.severe(MainWindow.class.getName(),
                            "autoLaunchRepoURL argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
                }
            } else if (arg.toLowerCase().matches("autolaunchsnapshotrepourl=.*")) {
                if (autoLaunchApp) {
                    try {
                        autoLaunchSnapshotRepoURL = new URL(arg.substring(arg.indexOf('=') + 1));
                    } catch (MalformedURLException e) {
                        FOKLogger.log(MainWindow.class.getName(), Level.SEVERE,
                                "Ignoring argument autoLaunchSnapshotRepoURL due to MalformedURLException", e);
                    }
                } else {
                    FOKLogger.severe(MainWindow.class.getName(),
                            "autoLaunchSnapshotRepoURL argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
                }
            } else if (arg.toLowerCase().matches("autolaunchgroupid=.*")) {
                if (autoLaunchApp) {
                    autoLaunchGroupId = arg.substring(arg.indexOf('=') + 1);
                } else {
                    FOKLogger.severe(MainWindow.class.getName(),
                            "autoLaunchGroupId argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
                }
            } else if (arg.toLowerCase().matches("autolaunchartifactid=.*")) {
                if (autoLaunchApp) {
                    autoLaunchArtifactId = arg.substring(arg.indexOf('=') + 1);
                } else {
                    FOKLogger.severe(MainWindow.class.getName(),
                            "autoLaunchArtifactId argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
                }
            } else if (arg.toLowerCase().matches("autolaunchclassifier=.*")) {
                if (autoLaunchApp) {
                    autoLaunchClassifier = arg.substring(arg.indexOf('=') + 1);
                } else {
                    FOKLogger.severe(MainWindow.class.getName(),
                            "autoLaunchClassifier argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
                }
            } else if (arg.toLowerCase().matches("autolaunchenablesnapshots")) {
                if (autoLaunchApp) {
                    autoLaunchUseSnapshots=true;
                } else {
                    FOKLogger.severe(MainWindow.class.getName(),
                            "autolaunchenablesnapshots argument will be ignored as no preceding launch command was found in the arguments. Please specify the argument 'launch' BEFORE specifying any autoLaunch arguments.");
                }
            }
        }

        if (autoLaunchApp) {
            if (autoLaunchRepoURL == null || autoLaunchSnapshotRepoURL == null || autoLaunchGroupId == null
                    || autoLaunchArtifactId == null) {
                // not sufficient info specified
                FOKLogger.severe(MainWindow.class.getName(), "Cannot auto-launch app as insufficient download info was specified.");
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

    private final Runnable getAppListRunnable = new Runnable() {
        @Override
        public void run() {
            try {

                Platform.runLater(() -> {
                    appList.setItems(FXCollections.observableArrayList());
                    appList.setDisable(true);
                    appList.setPlaceholder(new Label(bundle.getString("WaitForAppList")));
                });

                apps = App.getAppList();

                ObservableList<App> items = FXCollections.observableArrayList();
                FilteredList<App> filteredData = new FilteredList<>(items, s -> true);

                items.addAll(apps);

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

                    CustomListCell<App> cell = new CustomListCell<>();

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

                Platform.runLater(() -> {
                    appList.setItems(filteredData);
                    appList.setPlaceholder(new Label(bundle.getString("emptyAppList")));

                    // Only enable if no download is running
                    if (downloadAndLaunchThread == null) {
                        appList.setDisable(false);
                    } else if (!downloadAndLaunchThread.isAlive()) {
                        appList.setDisable(false);
                    }
                });

            } catch (JDOMException | IOException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                currentMainWindowInstance
                        .showErrorMessage("An error occurred: \n" + e.getClass().getName() + "\n" + e.getMessage());
            }
        }
    };

    /**
     * The thread that gets the app list
     */
    private Thread getAppListThread;


    @FXML // fx:id="appList"
    private ListView<App> appList; // Value injected by FXMLLoader

    @FXML // fx:id="searchField"
    private TextField searchField; // Value injected by FXMLLoader

    @FXML // fx:id="enableSnapshotsCheckbox"
    private CheckBox enableSnapshotsCheckbox; // Value injected by FXMLLoader

    /**
     * Returns {@code true} if snapshots are enabled
     *
     * @return {@code true} if snapshots are enabled
     */
    public boolean snapshotsEnabled() {
        return enableSnapshotsCheckbox.isSelected();
    }

    @FXML // fx:id="launchButton"
    private ProgressButton launchButton; // Value injected by FXMLLoader

    @FXML
    private Button optionButton;

    @FXML // fx:id="linkButton"
    private Button linkButton; // Value injected by FXMLLoader

    @SuppressWarnings("CanBeFinal")
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
     *
     * @return {@code true} if the app needs to work offline
     */
    public boolean workOffline() {
        return workOfflineCheckbox.isSelected();
    }

    @FXML
    private Hyperlink updateLink; // Value injected by FXMLLoader

    @FXML
    private Label versionLabel; // Value injected by FXMLLoader

    @FXML // fx:id="settingsGridView"
    private GridPane settingsGridView; // Value injected by FXMLLoader

    @FXML // fx:id="appInfoButton"
    private Button appInfoButton; // Value injected by FXMLLoader

    // Handler for AnchorPane[id="AnchorPane"] onDragDetected
    @SuppressWarnings("unused")
    @FXML
    void appListOnDragDetected(MouseEvent event) {
        if (currentlySelectedApp != null) {
            File tempFile = new File(Common.getAndCreateAppDataPath() + currentlySelectedApp.getName() + ".lnk");
            try {
                currentlySelectedApp.createShortCut(tempFile, bundle.getString("shortcutQuickInfo"));
                Dragboard db = appList.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putFiles(Collections.singletonList(tempFile));
                db.setContent(content);
            } catch (IOException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
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
    @SuppressWarnings("unused")
    void linkButtonOnMousePressed(MouseEvent event) {
        linkButton.setCursor(Cursor.CLOSED_HAND);
    }

    // Handler for ProgressButton[id="linkButton"] onMouseReleased
    @FXML
    @SuppressWarnings("unused")
    void linkButtonOnMouseReleased(MouseEvent event) {
        linkButton.setCursor(Cursor.OPEN_HAND);
    }

    @FXML
    @SuppressWarnings("unused")
    void optionButtonOnAction(ActionEvent event) {
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
                try {
                    if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("foklauncher")) {
                        event.acceptTransferModes(TransferMode.LINK);
                    } else if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("lnk") && (new ShellLink(f)).resolveTarget().startsWith(new File(Common.getPathAndNameOfCurrentJar()).toPath().toString())) {
                        event.acceptTransferModes(TransferMode.COPY);
                    } else {
                        event.consume();
                        return;
                    }
                } catch (IOException | ShellLinkException e) {
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                }
            }
        } else {
            event.consume();
        }
    }

    @FXML
    void mainFrameOnDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();

        for (File f : files) {
            try {
                if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("foklauncher")) {
                    FOKLogger.info(MainWindow.class.getName(), "Importing app from " + f.getAbsolutePath() + "...");
                    App.addImportedApp(f);
                    currentMainWindowInstance.loadAppList();
                } else if ((FilenameUtils.getExtension(f.getAbsolutePath()).equals("lnk") && (new ShellLink(f)).resolveTarget().startsWith(new File(Common.getPathAndNameOfCurrentJar()).toPath().toString()))) {
                    FOKLogger.info(MainWindow.class.getName(), "Running link from lnk file " + f.getAbsolutePath());
                    ShellLink link = new ShellLink(f);
                    File target = new File(link.resolveTarget());
                    String launchCommand = "";

                    if (FilenameUtils.getExtension(target.getAbsolutePath()).equals("jar")) {
                        launchCommand = "java -jar ";
                    }

                    launchCommand = launchCommand + link.resolveTarget() + " " + link.getCMDArgs();

                    Runtime.getRuntime().exec(launchCommand);

                    preparePhaseStarted();

                    Platform.runLater(() -> {
                        launchButton.setDisable(true);
                        launchButton.setControlText("");
                        progressBar.setProgressAnimated(-1);
                    });


                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            Platform.exit();
                        } catch (InterruptedException e) {
                            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                        }
                    });

                    t.start();
                }
            } catch (IOException | ShellLinkException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                currentMainWindowInstance.showErrorMessage(e.toString(), false);
            }
        }
    }

    public static final Runnable showLauncherAgain = new Runnable() {
        @Override
        public void run() {

            // reset the ui
            try {
                currentMainWindowInstance.start(stage);
            } catch (Exception e) {
                FOKLogger.log(MainWindow.class.getName(), Level.INFO,
                        "An error occurred while firing a handler for the LaunchedAppExited event, trying to run the handler using Platform.runLater...",
                        e);
            }
            Platform.setImplicitExit(true);
        }
    };

    @FXML
    @SuppressWarnings("unused")
    void updateLinkOnAction(ActionEvent event) {
        // Check for new version ignoring ignored updates
        Thread updateThread = new Thread(() -> {
            UpdateInfo update = UpdateChecker.isUpdateAvailableCompareAppVersion(AppConfig.getUpdateRepoBaseURL(),
                    AppConfig.groupID, AppConfig.artifactID, AppConfig.getUpdateFileClassifier(),
                    Common.getPackaging());
            Platform.runLater(() -> new UpdateAvailableDialog(update));
        });
        updateThread.setName("manualUpdateThread");
        updateThread.start();
    }

    @FXML
    @SuppressWarnings("unused")
    void languageSelectorOnAction(ActionEvent event) {
        FOKLogger.info(MainWindow.class.getName(), "Switching gui language to: "
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
            FOKLogger.log(MainWindow.class.getName(), Level.INFO, "An error occurred while setting a new gui language", e);
        }
        Platform.setImplicitExit(implicitExit);
    }

    // Handler for Button[fx:id="launchButton"] onAction
    @FXML
    @SuppressWarnings("unused")
    void launchButtonOnAction(ActionEvent event) {
        MainWindow gui = this;

        if (!downloadAndLaunchThread.isAlive()) {
            // Launch the download
            downloadAndLaunchThread = new Thread(() -> {
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
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                }
            });

            downloadAndLaunchThread.setName("downloadAndLaunchThread");
            downloadAndLaunchThread.start();
        } else {
            currentlySelectedApp.cancelDownloadAndLaunch(gui);
        }
    }

    @FXML
    @SuppressWarnings("unused")
    void linkButtonOnAction(ActionEvent event) {
        FOKLogger.info(MainWindow.class.getName(), "Creating shortcut using linkButton...");
        File file = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator
                + currentlySelectedApp.getName() + ".lnk");
        try {
            FOKLogger.info(MainWindow.class.getName(), "Creating shortcut for app " + currentlySelectedApp.getName()
                    + " at the following location: " + file.getAbsolutePath());
            currentlySelectedApp.createShortCut(file, bundle.getString("shortcutQuickInfo"));

            currentMainWindowInstance.showMessage(Alert.AlertType.INFORMATION, bundle.getString("shortcutCreatedMessage").replace("%s", currentlySelectedApp.getName()), false);
        } catch (Exception e) {
            // Add message about debugging environment
            String guiString = e.toString();
            if (e instanceof NullPointerException) {
                guiString = guiString
                        + "\n\nYou are probably in a development environment where linking does not work (where shall I link to? Package the source code into a jar file using the command \n\nmvn package\n\nand then retry.";
            }
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
            currentMainWindowInstance.showErrorMessage(guiString);
        }
    }

    // Handler for CheckBox[fx:id="workOfflineCheckbox"] onAction
    @FXML
    @SuppressWarnings("unused")
    void workOfflineCheckboxOnAction(ActionEvent event) {
        updateLaunchButton();
    }

    // Handler for CheckBox[fx:id="launchLauncherAfterAppExitCheckbox"] onAction
    @FXML
    @SuppressWarnings("unused")
    void launchLauncherAfterAppExitCheckboxOnAction(ActionEvent event) {
        prefs.setPreference(showLauncherAgainPrefKey,
                Boolean.toString(launchLauncherAfterAppExitCheckbox.isSelected()));
    }

    @FXML
    @SuppressWarnings("unused")
    void appInfoButtonOnAction(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI(currentlySelectedApp.getAdditionalInfoURL().toString()));
        } catch (IOException | URISyntaxException e) {
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
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
            FOKLogger.info(MainWindow.class.getName(), "Setting language: " + guiLanguageCode);
            Locale.setDefault(new Locale(guiLanguageCode));
        }

        bundle = ResourceBundle.getBundle("view.MainWindow");

        // appConfig = new Config();

        stage = primaryStage;
        try {
            Thread updateThread = new Thread(() -> {
                UpdateInfo update = UpdateChecker.isUpdateAvailable(AppConfig.getUpdateRepoBaseURL(),
                        AppConfig.groupID, AppConfig.artifactID, AppConfig.getUpdateFileClassifier(),
                        Common.getPackaging());
                if (update.showAlert) {
                    Platform.runLater(() -> new UpdateAvailableDialog(update));
                }
            });
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
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
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
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE,
                    "An error occurred but is not relevant as we are currently in the shutdown process. Possible reasons for this exception are: You tried to modify a view but it is not shown any more on the screen; You tried to cancel the app download but no download was in progress.",
                    e);
        }
    }

    @FXML
    // This method is called by the FXMLLoader when initialization is
    // complete
    @SuppressWarnings("unused")
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
        appInfoButton.setGraphic(infoIconView);

        // Bind the disabled property of the launchButton to the linkButton
        linkButton.disableProperty().bind(launchButton.disableProperty());

        // show gey icon when disabled
        linkButton.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // disabled, select gray icon
                linkIconView.setImage(new Image(MainWindow.class.getResourceAsStream("link_gray.png")));
            } else {
                // enabled, select blue icon
                linkIconView.setImage(new Image(MainWindow.class.getResourceAsStream("link.png")));
            }
        });

        optionButton.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // disabled, select gray icon
                optionIconView.setImage(new Image(MainWindow.class.getResourceAsStream("menu_gray.png")));
            } else {
                // enabled, select blue icon
                optionIconView.setImage(new Image(MainWindow.class.getResourceAsStream("menu.png")));
            }
        });

        appInfoButton.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // disabled, select gray icon
                infoIconView.setImage(new Image(MainWindow.class.getResourceAsStream("info_gray.png")));
            } else {
                // enabled, select blue icon
                infoIconView.setImage(new Image(MainWindow.class.getResourceAsStream("info.png")));
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
        Thread motdThread = new Thread(() -> {
            MOTD motd;
            try {
                motd = MOTD.getLatestMOTD(AppConfig.getMotdFeedUrl());
                if (!motd.isMarkedAsRead()) {
                    Platform.runLater(() -> new MOTDDialog(motd, motd.getEntry().getTitle()));
                }
            } catch (IllegalArgumentException | FeedException | IOException | ClassNotFoundException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
            }
        });
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
        appList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            try {
                currentlySelectedApp = appList.getSelectionModel().getSelectedItem();
            } catch (ArrayIndexOutOfBoundsException e) {
                currentlySelectedApp = null;
            }
            updateLaunchButton();
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
            downloadAndLaunchThread = new Thread(() -> {
                try {
                    appForAutoLaunch.downloadIfNecessaryAndLaunch(autoLaunchUseSnapshots || enableSnapshotsCheckbox.isSelected(), gui,
                            workOfflineCheckbox.isSelected());
                } catch (Exception e) {
                    gui.showErrorMessage("An error occurred: \n" + e.getClass().getName() + "\n" + e.getMessage());
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                } finally {
                    // Clean up
                    appForAutoLaunch = null;
                }
            });

            downloadAndLaunchThread.setName("downloadAndLaunchThread");
            downloadAndLaunchThread.start();
        }

        // Show alert if this is the first launch after an update
        if (isFirstLaunchAfterUpdate) {
            // first start consumed
            isFirstLaunchAfterUpdate = false;
            try {
                FOKLogger.fine(MainWindow.class.getName(), "Showing message after update...");
                this.showMessage(Alert.AlertType.INFORMATION, bundle.getString(firstUpdateMessageTextKey).replace("%v",Common.getAppVersion()), false);
            } catch (Exception e) {
                // Try to log, if it does not work just print the error
                try {
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadAvailableGuiLanguages() {
        List<Locale> supportedGuiLocales = Common.getLanguagesSupportedByResourceBundle(bundle);
        List<GuiLanguage> convertedList = new ArrayList<>(supportedGuiLocales.size());

        for (Locale lang : supportedGuiLocales) {
            convertedList.add(new GuiLanguage(lang, bundle.getString("languageSelector.chooseAutomatically")));
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

        Thread getAppStatus = new Thread(() -> {
            App checkedApp = currentlySelectedApp;
            boolean progressVisibleBefore = progressBar.isVisible();
            Platform.runLater(() -> {
                launchButton.setDisable(true);
                launchButton.setDefaultButton(false);
                launchButton.setStyle("-fx-background-color: transparent;");
                launchButton.setControlText("");
                progressBar.setPrefHeight(launchButton.getHeight());
                progressBar.setVisible(true);
                progressBar.setProgress(-1);
                launchButton.setProgressText(bundle.getString("progress.checkingVersionInfo"));
                appInfoButton.setDisable(true);
            });

            try {
                if (!workOfflineCheckbox.isSelected()) {
                    // downloads are enabled

                    // enable the additional info button if applicable
                    Platform.runLater(() -> appInfoButton.setDisable(checkedApp.getAdditionalInfoURL() == null));

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
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred", e);

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
                Platform.runLater(() -> {
                    launchButton.setProgressText("");
                    progressBar.setVisible(progressVisibleBefore);
                });
            }
        });

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
            Platform.runLater(() -> {
                launchButton.setDisable(isDisabled);
                launchButton.setDefaultButton(!isDisabled);
                launchButton.setStyle("");
                launchButton.setControlText(text);
            });
        }
    }

    @Override
    public void hide() {
        Platform.runLater(() -> stage.hide());
    }

    // Handler for CheckBox[fx:id="enableSnapshotsCheckbox"] onAction
    @FXML
    @SuppressWarnings("unused")
    void enableSnapshotsCheckboxOnAction(ActionEvent event) {
        updateLaunchButton();
        prefs.setPreference(enableSnapshotsPrefKey, Boolean.toString(enableSnapshotsCheckbox.isSelected()));
    }

    @Override
    public void preparePhaseStarted() {
        Platform.runLater(() -> {
            appList.setDisable(true);
            launchButton.setDisable(false);
            launchButton.setDefaultButton(false);
            optionButton.disableProperty().unbind();
            optionButton.setDisable(true);
            progressBar.setPrefHeight(launchButton.getHeight());
            launchButton.setStyle("-fx-background-color: transparent;");
            launchButton.setControlText(bundle.getString("okButton.cancelLaunch"));
            progressBar.setVisible(true);
            progressBar.setProgressAnimated(0 / 4.0);
            launchButton.setProgressText(bundle.getString("progress.preparing"));

            settingsGridView.setDisable(true);
        });
    }

    @Override
    public void downloadStarted() {
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            launchButton.setProgressText(bundle.getString("progress.downloading"));
        });
    }

    @Override
    public void installStarted() {
        Platform.runLater(() -> {
            progressBar.setProgressAnimated(1.0 / 2.0);
            launchButton.setProgressText(bundle.getString("progress.installing"));
        });
    }

    @Override
    public void launchStarted() {
        Platform.runLater(() -> {
            progressBar.setProgressAnimated(2.0 / 2.0);
            launchButton.setProgressText(bundle.getString("progress.launching"));
        });
    }

    public void showErrorMessage(String message) {
        showErrorMessage(message, false);
    }

    public void showErrorMessage(String message, @SuppressWarnings("SameParameterValue") boolean closeWhenDialogIsClosed) {
        showMessage(Alert.AlertType.ERROR, message, closeWhenDialogIsClosed);
    }

    public void showMessage(Alert.AlertType alertType, String message, boolean closeWhenDialogIsClosed) {
        Platform.runLater(() -> {
            String finalMessage;
            if (closeWhenDialogIsClosed) {
                finalMessage = message + "\n\n" + "The app needs to close now.";
            } else {
                finalMessage = message;
            }

            Alert alert = new Alert(alertType, finalMessage);
            alert.show();

            Thread t = new Thread(() -> {
                //noinspection StatementWithEmptyBody
                while (alert.isShowing()) {
                    // wait for dialog to be closed
                }

                if (closeWhenDialogIsClosed) {
                    System.err.println("Closing app after exception, good bye...");
                    Platform.exit();
                }
            });

            t.setName("showErrorThread");
            t.start();
        });
    }

    @Override
    public void operationCanceled() {
        FOKLogger.info(MainWindow.class.getName(), "Operation cancelled.");
        Platform.setImplicitExit(true);
        appList.setDisable(false);
        progressBar.setVisible(false);
        optionButton.disableProperty().bind(launchButton.disableProperty());
        Platform.runLater(() -> {
            launchButton.setProgressText("");
            settingsGridView.setDisable(false);
            updateLaunchButton();
        });
    }

    @Override
    public void cancelRequested() {
        if (progressBar != null) {
            progressBar.setProgressAnimated(0);
            launchButton.setProgressText(bundle.getString("cancelRequested"));
            launchButton.setDisable(true);
            FOKLogger.info(MainWindow.class.getName(), "Requested to cancel the current operation, Cancel in progress...");
        }
    }

    @Override
    public void downloadProgressChanged(double kilobytesDownloaded, double totalFileSizeInKB) {
        double timeThreshold = 500 + Math.random() * 3000;
        if (Math.abs(Date.from(Instant.now()).getTime() - latestProgressBarUpdate.getTime()) >= (timeThreshold)
                || kilobytesDownloaded == totalFileSizeInKB) {
            latestProgressBarUpdate = Date.from(Instant.now());
            Platform.runLater(() -> {
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
            });
        }
    }

}

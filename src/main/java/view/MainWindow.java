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
import applist.DownloadQueue;
import applist.DownloadQueueEntry;
import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.StringCommon;
import com.github.vatbub.common.core.logging.FOKLogger;
import com.github.vatbub.common.internet.Internet;
import com.github.vatbub.common.updater.UpdateChecker;
import com.github.vatbub.common.updater.UpdateInfo;
import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.view.UpdateAvailableDialog;
import com.github.vatbub.common.view.core.CustomProgressBar;
import com.github.vatbub.common.view.core.ProgressButton;
import com.github.vatbub.common.view.motd.MOTD;
import com.github.vatbub.common.view.motd.MOTDDialog;
import com.rometools.rome.io.FeedException;
import config.AppConfig;
import extended.CustomListCell;
import extended.GuiLanguage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.Nullable;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class MainWindow implements HidableProgressDialogWithEnqueuedNotification {
    private static final ImageView linkIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("link_gray.png")));
    private static final ImageView addToDownloadQueueIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("down-arrow-hollow.png")));
    private static final ImageView optionIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("menu_gray.png")));
    private static final ImageView infoIconView = new ImageView(new Image(MainWindow.class.getResourceAsStream("info_gray.png")));
    // private static final EnumSet<DatadogReporter.Expansion> expansions = EnumSet.of(COUNT, RATE_1_MINUTE, RATE_15_MINUTE, MEDIAN, P95, P99);
    // private static final MetricRegistry metricsRegistry = new MetricRegistry();
    private static ResourceBundle bundle;
    public static final Runnable showLauncherAgain = () -> {
        // reset the ui
        try {
            // EntryClass.restart();
            EntryClass.getControllerInstance().updateLaunchButton();
            EntryClass.getStage().setIconified(false);
        } catch (Exception e) {
            FOKLogger.log(MainWindow.class.getName(), Level.INFO,
                    "An error occurred while firing a handler for the LaunchedAppExited event, trying to run the handler using Platform.runLater...",
                    e);
        }
        Platform.setImplicitExit(true);
    };
    @FXML
    public CheckBox launchLauncherAfterAppExitCheckbox;
    @FXML
    private Button addToDownloadQueueButton;
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private AppList apps;
    private App currentlySelectedApp = null;
    private int currentlySelectedIndex = -1;
    private Date latestProgressBarUpdate = Date.from(Instant.now());
    private Thread getAppListThread;
    private App appForAutoLaunch;
    @FXML
    private ListView<App> appList;
    @FXML
    private TextField searchField;
    @FXML
    private Button showDownloadQueueButton;
    @FXML
    private Label downloadQueueCountLabel;
    @FXML
    private CheckBox enableSnapshotsCheckbox;
    @FXML
    private ProgressButton launchButton;
    @FXML
    private Button optionButton;
    @FXML
    private Button linkButton;
    @FXML
    private ComboBox<GuiLanguage> languageSelector;
    @FXML
    private CustomProgressBar progressBar;
    @FXML
    private CheckBox workOfflineCheckbox;
    @FXML
    private Hyperlink updateLink;
    @FXML
    private Label versionLabel;
    @FXML
    private GridPane settingsGridView;
    @FXML
    private Button appInfoButton;
    private DownloadQueue downloadQueue = new DownloadQueue();
    private final Runnable getAppListRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                int previouslySelectedIndex = getCurrentlySelectedIndex();

                // skip the loading screen if it already contains a list
                if (apps == null) {
                    Platform.runLater(() -> {
                        appList.setItems(FXCollections.observableArrayList());
                        appList.setDisable(true);
                        appList.setPlaceholder(new Label(bundle.getString("WaitForAppList")));
                    });
                }

                apps = App.getAppList(workOffline());

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
                            cell.setContextMenu(cell.getItem().getContextMenu());
                        }
                    });
                    return cell;
                });

                Platform.runLater(() -> {
                    appList.setItems(filteredData);
                    appList.setPlaceholder(new Label(bundle.getString("emptyAppList")));

                    if (previouslySelectedIndex >= 0) {
                        appList.getSelectionModel().select(previouslySelectedIndex);
                    }

                    // Only enable if no download is running
                    if (!isMainDownloadRunning()) {
                        appList.setDisable(false);
                    }
                });

            } catch (FileNotFoundException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "Unable to load the cached app list", e);
            } catch (JDOMException | IOException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                EntryClass.getControllerInstance()
                        .showErrorMessage(FOKLogger.DEFAULT_ERROR_TEXT + ": \n" + e.getClass().getName() + "\n" + e.getMessage());
            }
        }
    };
    @FXML
    private TitledPane downloadQueueTitledPane;

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static void setBundle(ResourceBundle bundleToSet) {
        bundle = bundleToSet;
    }

    private static void initDataDogReporting(String apiKey) throws IOException {
        HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(apiKey).build();
        /*DatadogReporter reporter = DatadogReporter.forRegistry(metricsRegistry)
                .withTransport(httpTransport)
                .withExpansions(expansions)
                .build();

        reporter.start(10, TimeUnit.SECONDS);*/
    }

    /**
     * Performs cleanup tasks. This method is expected to be called when the controlled stage is hidden.
     */
    public void cleanup() {
        downloadQueue.shutdownAndCancelDownloads();
        scheduledExecutorService.shutdownNow();
    }

    /*public static MetricRegistry getMetricsRegistry()
    {
        return metricsRegistry;
    }*/

    /**
     * Initiates the download and launch of the latest version of the specified app.
     *
     * @param appToLaunch      The app to launch.
     * @param snapshotsEnabled Specifies if snapshots are enabled.
     * @param startupArgs      Additional startup args to be passed to the app's main method
     */
    public void launchAppFromGUI(App appToLaunch, boolean snapshotsEnabled, String... startupArgs) {
        launchAppFromGUI(appToLaunch, snapshotsEnabled, false, startupArgs);
    }

    /**
     * Initiates the download and launch of the specified version of the specified app.
     *
     * @param appToLaunch     The app to launch.
     * @param versionToLaunch The version to launch.
     * @param startupArgs     Additional startup args to be passed to the app's main method
     */
    public void launchAppFromGUI(App appToLaunch, @Nullable Version versionToLaunch, String... startupArgs) {
        launchAppFromGUI(appToLaunch, false, false, versionToLaunch, startupArgs);
    }

    /**
     * Initiates the download and launch of the latest version of the specified app.
     *
     * @param appToLaunch                           The app to launch.
     * @param snapshotsEnabled                      Specifies if snapshots are enabled.
     * @param ignoreShowLauncherWhenAppExitsSetting If set to {@code true}, the value of the 'Show launcher after app exits'-checkbox is ignored (will act as if not checked)
     * @param startupArgs                           Additional startup args to be passed to the app's main method
     */
    public void launchAppFromGUI(App appToLaunch, boolean snapshotsEnabled, boolean ignoreShowLauncherWhenAppExitsSetting, String... startupArgs) {
        launchAppFromGUI(appToLaunch, snapshotsEnabled, ignoreShowLauncherWhenAppExitsSetting, null, startupArgs);
    }

    /**
     * Initiates the download and launch of the latest version of the specified app.
     *
     * @param appToLaunch                           The app to launch.
     * @param snapshotsEnabled                      Specifies if snapshots are enabled.
     * @param ignoreShowLauncherWhenAppExitsSetting If set to {@code true}, the value of the 'Show launcher after app exits'-checkbox is ignored (will act as if not checked)
     * @param versionToDownload                     The version to download and launch.
     * @param startupArgs                           Additional startup args to be passed to the app's main method
     */
    public void launchAppFromGUI(App appToLaunch, boolean snapshotsEnabled, boolean ignoreShowLauncherWhenAppExitsSetting, @Nullable Version versionToDownload, String... startupArgs) {
        DownloadQueueEntry entry = downloadQueue.getEntryForApp(appToLaunch);
        if (entry != null && entry.isEnableSnapshots() == snapshotsEnabled && entry.getVersionToDownload() == versionToDownload) {
            entry.setLaunchAfterDownload(true);
            if (launchLauncherAfterAppExitCheckbox.isSelected() && !ignoreShowLauncherWhenAppExitsSetting) {
                entry.getApp().addEventHandlerWhenLaunchedAppExits(showLauncherAgain);
            } else {
                entry.getApp().removeEventHandlerWhenLaunchedAppExits(showLauncherAgain);
            }
            if (entry.getGui() != null && entry.getGui() instanceof DownloadQueueEntryView) {
                ((DownloadQueueEntryView) entry.getGui()).addAttachedGui(this);
            }
            return;
        }

        // Attach the on app exit handler if required
        if (launchLauncherAfterAppExitCheckbox.isSelected() && !ignoreShowLauncherWhenAppExitsSetting) {
            appToLaunch.addEventHandlerWhenLaunchedAppExits(showLauncherAgain);
        } else {
            appToLaunch.removeEventHandlerWhenLaunchedAppExits(showLauncherAgain);
        }

        DownloadQueueEntry entryToLaunch;
        if (versionToDownload == null) {
            entryToLaunch = new DownloadQueueEntry(appToLaunch, new DownloadQueueEntryView(this, (ListView<DownloadQueueEntryView>) downloadQueueTitledPane.getContent(), appToLaunch), snapshotsEnabled(), startupArgs);
        } else {
            entryToLaunch = new DownloadQueueEntry(appToLaunch, new DownloadQueueEntryView(this, (ListView<DownloadQueueEntryView>) downloadQueueTitledPane.getContent(), appToLaunch), versionToDownload, snapshotsEnabled(), startupArgs);
        }
        entryToLaunch.setLaunchAfterDownload(true);
        ((DownloadQueueEntryView) entryToLaunch.getGui()).addAttachedGui(this);
        downloadQueue.addFirst(entryToLaunch);
    }

    /**
     * Returns the currently selected app.
     *
     * @return The currently selected app.
     * @see #updateLaunchButton()
     */
    public App getCurrentlySelectedApp() {
        return currentlySelectedApp;
    }

    public int getCurrentlySelectedIndex() {
        return currentlySelectedIndex;
    }

    /**
     * Returns {@code true} if snapshots are enabled
     *
     * @return {@code true} if snapshots are enabled
     */
    public boolean snapshotsEnabled() {
        return enableSnapshotsCheckbox.isSelected();
    }

    /**
     * Returns {@code true} if the app needs to work offline
     *
     * @return {@code true} if the app needs to work offline
     */
    public boolean workOffline() {
        return workOfflineCheckbox.isSelected();
    }

    // Handler for AnchorPane[id="AnchorPane"] onDragDetected
    @FXML
    void appListOnDragDetected(@SuppressWarnings("unused") MouseEvent event) {
        if (currentlySelectedApp != null) {
            File tempFile = new File(Common.getInstance().getAndCreateAppDataPath() + currentlySelectedApp.getName() + ".lnk");
            try {
                currentlySelectedApp.createShortCut(tempFile, bundle.getString("shortcutQuickInfo"));
                Dragboard db = appList.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putFiles(Collections.singletonList(tempFile));
                db.setContent(content);
            } catch (IOException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
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
    void linkButtonOnMousePressed(@SuppressWarnings("unused") MouseEvent event) {
        linkButton.setCursor(Cursor.CLOSED_HAND);
    }

    // Handler for ProgressButton[id="linkButton"] onMouseReleased
    @FXML
    void linkButtonOnMouseReleased(@SuppressWarnings("unused") MouseEvent event) {
        linkButton.setCursor(Cursor.OPEN_HAND);
    }

    @FXML
    void optionButtonOnMouseClicked(MouseEvent event) {
        currentlySelectedApp.getContextMenu().show(optionButton, event.getScreenX(), event.getScreenY());
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
                    } else if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("lnk") && (new ShellLink(f)).resolveTarget().startsWith(new File(Common.getInstance().getPathAndNameOfCurrentJar()).toPath().toString())) {
                        event.acceptTransferModes(TransferMode.COPY);
                    } else {
                        event.consume();
                        return;
                    }
                } catch (IOException | ShellLinkException e) {
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
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
                    loadAppList();
                } else if ((FilenameUtils.getExtension(f.getAbsolutePath()).equals("lnk") && (new ShellLink(f)).resolveTarget().startsWith(new File(Common.getInstance().getPathAndNameOfCurrentJar()).toPath().toString()))) {
                    FOKLogger.info(MainWindow.class.getName(), "Running link from lnk file " + f.getAbsolutePath());
                    ShellLink link = new ShellLink(f);
                    File target = new File(link.resolveTarget());
                    StringBuilder launchCommand = new StringBuilder();

                    if (FilenameUtils.getExtension(target.getAbsolutePath()).equals("jar")) {
                        launchCommand.append("java -jar ");
                    }

                    launchCommand.append(link.resolveTarget()).append(" ").append(link.getCMDArgs());

                    Runtime.getRuntime().exec(launchCommand.toString());

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
                            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                        }
                    });

                    t.start();
                }
            } catch (IOException | ShellLinkException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                EntryClass.getControllerInstance().showErrorMessage(e.toString(), false);
            }
        }
    }

    @FXML
    void updateLinkOnAction(@SuppressWarnings("unused") ActionEvent event) {
        // Check for new version ignoring ignored updates
        Thread updateThread = new Thread(() -> {
            try {
                UpdateInfo update = UpdateChecker.isUpdateAvailableCompareAppVersion(new URL(AppConfig.getRemoteConfig().getValue("updateRepoBaseURL")),
                        AppConfig.getRemoteConfig().getValue("groupID"), AppConfig.getRemoteConfig().getValue("artifactID"), AppConfig.getUpdateFileClassifier(),
                        Common.getInstance().getPackaging());
                Platform.runLater(() -> new UpdateAvailableDialog(update));
            } catch (MalformedURLException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
            }
        });
        updateThread.setName("manualUpdateThread");
        updateThread.start();
    }

    @FXML
    void languageSelectorOnAction(@SuppressWarnings("unused") ActionEvent event) {
        FOKLogger.info(MainWindow.class.getName(), "Switching gui language to: "
                + languageSelector.getItems().get(languageSelector.getSelectionModel().getSelectedIndex()));
        EntryClass.getPrefs().setPreference(EntryClass.PrefKeys.GUI_LANGUAGE.toString(), languageSelector.getItems()
                .get(languageSelector.getSelectionModel().getSelectedIndex()).getLocale().getLanguage());

        // Restart gui
        boolean implicitExit = Platform.isImplicitExit();
        Platform.setImplicitExit(false);
        EntryClass.getStage().hide();
        try {
            EntryClass.restart();
        } catch (Exception e) {
            FOKLogger.log(MainWindow.class.getName(), Level.INFO, "An error occurred while setting a new gui language", e);
        }
        Platform.setImplicitExit(implicitExit);
    }

    // Handler for Button[fx:id="launchButton"] onAction
    @FXML
    void launchButtonOnAction(@SuppressWarnings("unused") ActionEvent event) {
        DownloadQueueEntry entry = downloadQueue.getEntryForApp(getCurrentlySelectedApp());

        if (!isMainDownloadRunning(entry)) {
            launchAppFromGUI(currentlySelectedApp, enableSnapshotsCheckbox.isSelected());
        } else {
            if (entry != null) {
                entry.getApp().cancelDownloadAndLaunch(entry.getGui());
            } else {
                currentlySelectedApp.cancelDownloadAndLaunch(this);
            }
        }
    }

    public boolean isMainDownloadRunning() {
        DownloadQueueEntry entry;
        if (getCurrentlySelectedApp() != null)
            entry = downloadQueue.getEntryForApp(getCurrentlySelectedApp());
        else if (appForAutoLaunch != null)
            entry = downloadQueue.getEntryForApp(appForAutoLaunch);
        else
            return false;

        return isMainDownloadRunning(entry);
    }

    public boolean isMainDownloadRunning(DownloadQueueEntry entry) {
        return entry != null && entry.getGui() instanceof DownloadQueueEntryView && ((DownloadQueueEntryView) entry.getGui()).getAttachedGuis().contains(this);
    }

    @FXML
    void linkButtonOnAction(@SuppressWarnings("unused") ActionEvent event) {
        FOKLogger.info(MainWindow.class.getName(), "Creating shortcut using linkButton...");
        File file = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator
                + currentlySelectedApp.getName() + ".lnk");
        try {
            FOKLogger.info(MainWindow.class.getName(), "Creating shortcut for app " + currentlySelectedApp.getName()
                    + " at the following location: " + file.getAbsolutePath());
            currentlySelectedApp.createShortCut(file, bundle.getString("shortcutQuickInfo"));

            EntryClass.getControllerInstance().showMessage(Alert.AlertType.INFORMATION, bundle.getString("shortcutCreatedMessage").replace("%s", currentlySelectedApp.getName()), false);
        } catch (NullPointerException e) {
            String errorText = "You are probably in a development environment where linking does not work (where shall I link to? Package the source code into a jar file using the command \n\nmvn package\n\nand then retry.";
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, errorText, e);
            EntryClass.getControllerInstance().showErrorMessage(e.toString() + "\n\n" + errorText);
        } catch (Exception e) {
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
            EntryClass.getControllerInstance().showErrorMessage(e.toString());
        }
    }

    @FXML
    void workOfflineCheckboxOnAction(@SuppressWarnings("unused") ActionEvent event) {
        updateLaunchButton();
    }

    @FXML
    void launchLauncherAfterAppExitCheckboxOnAction(@SuppressWarnings("unused") ActionEvent event) {
        EntryClass.getPrefs().setPreference(EntryClass.PrefKeys.SHOW_LAUNCHER_AGAIN.toString(),
                Boolean.toString(launchLauncherAfterAppExitCheckbox.isSelected()));
    }

    @FXML
    void appInfoButtonOnAction(@SuppressWarnings("unused") ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI(currentlySelectedApp.getAdditionalInfoURL().toString()));
        } catch (IOException | URISyntaxException e) {
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
        }
    }

    @FXML
    void addToDownloadQueueButtonOnAction(@SuppressWarnings("unused") ActionEvent event) {
        downloadQueue.add(new DownloadQueueEntry(getCurrentlySelectedApp(), new DownloadQueueEntryView(this, (ListView<DownloadQueueEntryView>) downloadQueueTitledPane.getContent(), getCurrentlySelectedApp()), snapshotsEnabled()));
        updateLaunchButton();
    }

    private void updateDownloadCounter(int numberOfDownloads) {
        if (numberOfDownloads == 1) {
            // Platform.runLater(() -> downloadQueueCountLabel.setText(""));
            Platform.runLater(() -> downloadQueueTitledPane.setText(getBundle().getString("downloadQueueTitle.oneDownload").replace("%n", Integer.toString(numberOfDownloads))));
        } else {
            // Platform.runLater(() -> downloadQueueCountLabel.setText(newValue.toString()));
            Platform.runLater(() -> downloadQueueTitledPane.setText(getBundle().getString("downloadQueueTitle.moreDownloads").replace("%n", Integer.toString(numberOfDownloads))));
        }
    }

    @FXML
        // This method is called by the FXMLLoader when initialization is
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

        // bind the download queue count to the label text
        downloadQueue.currentTotalDownloadCountProperty().addListener((observable, oldValue, newValue) -> {
            updateDownloadCounter(newValue.intValue());
        });

        // update the counter for the first time
        updateDownloadCounter(downloadQueue.getCurrentTotalDownloadCount());
        ((ListView<DownloadQueueEntryView>) downloadQueueTitledPane.getContent()).setSelectionModel(new NoSelectionModel<>());

        downloadQueueTitledPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // expanded
                ListView<DownloadQueueEntryView> listView = (ListView<DownloadQueueEntryView>) downloadQueueTitledPane.getContent();
                double maxWidth = 0;
                for (DownloadQueueEntryView view : listView.getItems()) {
                    view.autosize();
                    if (maxWidth < view.getWidth())
                        maxWidth = view.getWidth();
                }
                downloadQueueTitledPane.setPrefWidth(maxWidth + 40);
            } else {
                downloadQueueTitledPane.setPrefWidth(0);
            }
        });

        // add icons to buttons
        linkButton.setGraphic(linkIconView);
        optionButton.setGraphic(optionIconView);
        appInfoButton.setGraphic(infoIconView);
        addToDownloadQueueButton.setGraphic(addToDownloadQueueIconView);

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

        addToDownloadQueueButton.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                addToDownloadQueueIconView.setImage((new Image(MainWindow.class.getResourceAsStream("down-arrow-hollow_gray.png"))));
            } else {
                addToDownloadQueueIconView.setImage((new Image(MainWindow.class.getResourceAsStream("down-arrow-hollow.png"))));
            }
        });

        optionButton.disableProperty().bind(launchButton.disableProperty());
        linkButton.disableProperty().bind(launchButton.disableProperty());
        addToDownloadQueueButton.disableProperty().bind(launchButton.disableProperty());

        addToDownloadQueueButton.setTooltip(new Tooltip(getBundle().getString("addToDownloadQueueButton.hintText")));

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
                motd = MOTD.getLatestMOTD(new URL(AppConfig.getRemoteConfig().getValue("motdFeedUrl")));
                if (!motd.isMarkedAsRead()) {
                    Platform.runLater(() -> new MOTDDialog(motd, motd.getEntry().getTitle()));
                }
            } catch (IllegalArgumentException | FeedException | IOException | ClassNotFoundException e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
            }
        });
        motdThread.setName("motdThread");
        motdThread.start();

        enableSnapshotsCheckbox.setSelected(Boolean.parseBoolean(EntryClass.getPrefs().getPreference(EntryClass.PrefKeys.ENABLE_SNAPSHOTS.toString(), "false")));
        launchLauncherAfterAppExitCheckbox
                .setSelected(Boolean.parseBoolean(EntryClass.getPrefs().getPreference(EntryClass.PrefKeys.SHOW_LAUNCHER_AGAIN.toString(), "false")));

        try {
            versionLabel.setText(new Version(Common.getInstance().getAppVersion(), Common.getInstance().getBuildNumber()).toString(false));
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
                currentlySelectedIndex = appList.getSelectionModel().getSelectedIndex();
                currentlySelectedApp = appList.getSelectionModel().getSelectedItem();
            } catch (ArrayIndexOutOfBoundsException e) {
                currentlySelectedApp = null;
                currentlySelectedIndex = -1;
            }
            updateLaunchButton();
        });

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            boolean previousSelectionState = workOfflineCheckbox.isSelected();
            if (Internet.isConnected()) {
                // we don't want to deselect the checkbox if the offline mode was activated manually
                if (workOfflineCheckbox.isDisable()) {
                    // computer was offline before and now went online (offline mode was automatic)
                    workOfflineCheckbox.setSelected(false);
                }
                workOfflineCheckbox.setDisable(false);
            } else {
                workOfflineCheckbox.setSelected(true);
                workOfflineCheckbox.setDisable(true);
            }

            // update the list only if the offline mode was DISabled
            if (!workOfflineCheckbox.isSelected() && previousSelectionState) {
                loadAppList();
            }
            // update the launch button if offlinemode as enabled or disabled
            if (previousSelectionState != workOfflineCheckbox.isSelected()) {
                updateLaunchButton();
            }
        }, 0, 30, TimeUnit.SECONDS);

        loadAppList();

        // auto launch app if one was specified
        if (EntryClass.getAutoLaunchMVNCoordinates() != null) {
            appForAutoLaunch = new App(EntryClass.getAutoLaunchMVNCoordinates().getArtifactId(), EntryClass.getAutoLaunchMVNCoordinates());
            launchAppFromGUI(appForAutoLaunch, EntryClass.isAutoLaunchSnapshotsEnabled() || enableSnapshotsCheckbox.isSelected(), true, EntryClass.getAdditionalAutoLaunchStartupArgs());
        }

        // Show alert if this is the first launch after an update
        if (EntryClass.isFirstLaunchAfterUpdate()) {
            // first start consumed
            EntryClass.setFirstLaunchAfterUpdate(false);
            try {
                FOKLogger.fine(MainWindow.class.getName(), "Showing message after update...");
                this.showMessage(Alert.AlertType.INFORMATION, bundle.getString(EntryClass.getFirstUpdateMessageTextKey()).replace("%v", Common.getInstance().getAppVersion()), false);
            } catch (Exception e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
            }
        }
    }

    /**
     * Loads the available GUI languages to the appropriate selector
     */
    private void loadAvailableGuiLanguages() {
        List<Locale> supportedGuiLocales = Common.getInstance().getLanguagesSupportedByResourceBundle(bundle);
        List<GuiLanguage> convertedList = new ArrayList<>(supportedGuiLocales.size());

        for (Locale lang : supportedGuiLocales) {
            convertedList.add(new GuiLanguage(lang, bundle.getString("languageSelector.chooseAutomatically")));
        }
        ObservableList<GuiLanguage> items = FXCollections.observableArrayList(convertedList);
        languageSelector.setItems(items);

        if (Locale.getDefault() != EntryClass.getSystemDefaultLocale()) {
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
     * Loads the app list using the {@link App#getAppList(boolean)}-method
     */
    public void loadAppList() {
        if (getAppListThread != null && getAppListThread.isAlive()) {
            return;
        }

        // Thread is either null or not running anymore
        getAppListThread = new Thread(getAppListRunnable);
        getAppListThread.setName("getAppListThread");
        getAppListThread.start();
    }

    /**
     * Updates the text on the launch button according to the status of the currently selected app
     *
     * @see #getCurrentlySelectedApp()
     */
    public void updateLaunchButton() {
        apps.clearVersionCache();

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
                DownloadQueueEntry entry = downloadQueue.getEntryForApp(checkedApp);
                if (entry != null) {
                    setLaunchButtonText(checkedApp, false, bundle.getString("okButton.launchAfterDownload"));
                } else if (!workOfflineCheckbox.isSelected()) {
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
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);

                // Switch to offline mode
                workOfflineCheckbox.setSelected(true);
                workOfflineCheckbox.setDisable(true);

                // update launch button accordingly
                updateLaunchButton();

                // Show error message
                EntryClass.getControllerInstance().showErrorMessage(
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
        if (!isMainDownloadRunning() && currentlySelectedApp != null) {
            getAppStatus.setName("getAppStatus");
            getAppStatus.start();
        } else if (currentlySelectedApp == null && appForAutoLaunch == null) {
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
        Platform.runLater(() -> EntryClass.getStage().setIconified(true));

        if (launchLauncherAfterAppExitCheckbox.isSelected()) {
            Platform.runLater(() -> {
                appList.setDisable(false);
                launchButton.setDefaultButton(true);
                optionButton.disableProperty().bind(launchButton.disableProperty());
                progressBar.setPrefHeight(launchButton.getHeight());
                launchButton.setStyle("");
                progressBar.setVisible(false);
                progressBar.setProgressAnimated(-1);
                launchButton.setProgressText("");

                settingsGridView.setDisable(false);
            });

        } else {
            Platform.runLater(() ->{
                progressBar.setProgressAnimated(-1);
                launchButton.setProgressText(getBundle().getString("progress.waitForDownloadsToFinish"));
                launchButton.setControlText("");
            });

            // exit the launcher once all downloads are complete
            downloadQueue.shutdown();
            downloadQueue.setOnShutdownCompleted(() -> Platform.runLater(() -> EntryClass.getStage().hide()));
        }
    }

    // Handler for CheckBox[fx:id="enableSnapshotsCheckbox"] onAction
    @FXML
    @SuppressWarnings("unused")
    void enableSnapshotsCheckboxOnAction(ActionEvent event) {
        updateLaunchButton();
        EntryClass.getPrefs().setPreference(EntryClass.PrefKeys.ENABLE_SNAPSHOTS.toString(), Boolean.toString(enableSnapshotsCheckbox.isSelected()));
    }

    @Override
    public void enqueued() {
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
            progressBar.setProgressAnimated(-1);
            launchButton.setProgressText(bundle.getString("progress.enqueued"));

            settingsGridView.setDisable(true);
        });
    }

    @Override
    public void preparePhaseStarted() {
        Platform.runLater(() -> {
            launchButton.setProgressText(bundle.getString("progress.preparing"));
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
            progressBar.setProgressAnimated(1);
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
                    FOKLogger.severe(getClass().getName(), "Closing app after exception, good bye...");
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

                String downloadedString = StringCommon.convertFileSizeToReadableString(kilobytesDownloaded);

                String totalString = StringCommon.convertFileSizeToReadableString(totalFileSizeInKB);

                launchButton.setProgressText(bundle.getString("progress.downloading") + "(" + downloadedString + "/"
                        + totalString + ")");
            });
        }
    }
}

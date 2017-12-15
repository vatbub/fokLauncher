package view;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 - 2017 Frederik Kammel
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
import applist.MVNCoordinates;
import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.Prefs;
import com.github.vatbub.common.core.logging.FOKLogger;
import com.github.vatbub.common.internet.Internet;
import com.github.vatbub.common.updater.UpdateChecker;
import com.github.vatbub.common.updater.UpdateInfo;
import com.github.vatbub.common.updater.view.UpdateAvailableDialog;
import config.AppConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * Class that contains the main method, handles the command line args and launches the JavaFX toolkit if required.
 * The decision to divide {@link MainWindow} into two parts ({@link MainWindow} and this class) was taken as
 * command line processing and framework resource management became too big tasks overtime and having them all together in one class just created a big mess.
 */
public class EntryClass extends Application {
    private static EntryClass entryClassInstance;
    private static Options options;
    private static Option help;
    private static Option autoLaunchCLIMode;
    private static Option autoLaunchWindowMode;
    private static Option autoLaunchRepoUrl;
    private static Option autoLaunchSnapshotRepoUrl;
    private static Option groupId;
    private static Option artifactId;
    private static Option classifier;
    private static Option enableSnapshots;
    private static Option additionalArgs;
    private static Option mockAppVersion;
    private static Option mockBuildNumber;
    private static Option mockPackaging;
    private static Prefs prefs;
    private static Locale systemDefaultLocale;
    private static MainWindow controllerInstance;
    private static boolean isFirstLaunchAfterUpdate;
    private static String firstUpdateMessageTextKey;
    private static final UpdateChecker.CompleteUpdateRunnable firstStartAfterUpdateRunnable = (oldVersion, oldFile) -> {
        setFirstLaunchAfterUpdate(true);

        if (oldVersion == null) {
            // Version was so old that we cannot determine its actual version number so we need to make sure that we can use the current storage model

            // use the old alert message
            setFirstUpdateMessageTextKey("firstLaunchAfterUpdateDeletedApps");
            try {
                // delete apps folder
                FOKLogger.info(MainWindow.class.getName(), "Deleting the apps folder after update...");
                FileUtils.deleteDirectory(new File(Common.getInstance().getAndCreateAppDataPath() + "apps"));
            } catch (Exception e) {
                FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
            }
        } else {
            setFirstUpdateMessageTextKey("firstLaunchAfterUpdate");
        }
    };
    private static LaunchMode launchMode;
    private static MVNCoordinates autoLaunchMVNCoordinates;
    private static String[] additionalAutoLaunchStartupArgs;
    private static boolean autoLaunchSnapshotsEnabled;
    private Stage stage;

    public static void main(String[] args) {
        Common.getInstance().setAppName("foklauncher");
        FOKLogger.enableLoggingOfUncaughtExceptions();
        systemDefaultLocale = Locale.getDefault();

        UpdateChecker.completeUpdate(args, firstStartAfterUpdateRunnable);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(getCliOptions(), args);

            if (commandLine.hasOption(getHelpOption().getOpt())) {
                printHelpMessage();
                System.exit(0);
            }

            if (commandLine.hasOption(getMockAppVersionOption().getOpt())) {
                Common.getInstance().setMockAppVersion(commandLine.getOptionValue(getMockAppVersionOption().getOpt()));
            }
            if (commandLine.hasOption(getMockBuildNumberOption().getOpt())) {
                Common.getInstance().setMockBuildNumber(commandLine.getOptionValue(getMockBuildNumberOption().getOpt()));
            }
            if (commandLine.hasOption(getMockPackagingOption().getOpt())) {
                Common.getInstance().setMockPackaging(commandLine.getOptionValue(getMockPackagingOption().getOpt()));
            }

            if (commandLine.hasOption(getAutoLaunchCLIModeOption().getOpt())) {
                launchMode = LaunchMode.CLI;
            } else if (commandLine.hasOption(getAutoLaunchWindowModeOption().getOpt())) {
                launchMode = LaunchMode.AUTO_WINDOW;
            } else {
                launchMode = LaunchMode.MANUAL_WINDOW;
            }

            if (getLaunchMode() != LaunchMode.MANUAL_WINDOW && !hasRequiredAutoLaunchArgs(commandLine)) {
                throw new IllegalArgumentException("Launcher is set to auto-launch mode but at least one required auto-launch parameter was not specified.");
            } else if (getLaunchMode() == LaunchMode.MANUAL_WINDOW && hasAtLeastAutoLaunchArgs(commandLine)) {
                FOKLogger.info(EntryClass.class.getName(), "Ignoring auto-launch args as the launcher was not put in auto-launch mode. Use either -cli or -w to put the launcher in auto-launch mode.");
            }

            if (getLaunchMode() != LaunchMode.MANUAL_WINDOW) {
                autoLaunchMVNCoordinates = new MVNCoordinates(new URL(commandLine.getOptionValue(getAutoLaunchRepoUrlOption().getOpt())), new URL(commandLine.getOptionValue(getAutoLaunchSnapshotRepoUrlOption().getOpt())), commandLine.getOptionValue(getGroupIdOption().getOpt()), commandLine.getOptionValue(getArtifactIdOption().getOpt()));
                if (commandLine.hasOption(getClassifierOption().getOpt())) {
                    autoLaunchMVNCoordinates.setClassifier(commandLine.getOptionValue(getClassifierOption().getOpt()));
                }
                if (commandLine.hasOption(getAdditionalArgsOption().getOpt())) {
                    additionalAutoLaunchStartupArgs = commandLine.getOptionValues(getAdditionalArgsOption().getOpt());
                }
                if (commandLine.hasOption(getEnableSnapshotsOption().getOpt())) {
                    autoLaunchSnapshotsEnabled = true;
                }
            }

            switch (getLaunchMode()) {
                case MANUAL_WINDOW:
                case AUTO_WINDOW:
                    try {
                        launch(args);
                        break;
                    } catch (Exception e) {
                        if (getLaunchMode() == LaunchMode.AUTO_WINDOW) {
                            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Unable to launch the gui, falling back to CLI mode...", e);
                        } else {
                            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Unable to launch the gui", e);
                            System.exit(1);
                        }
                    }
                case CLI:
                    App autoLaunchApp = new App(getAutoLaunchMVNCoordinates().getArtifactId(), getAutoLaunchMVNCoordinates());
                    // app won't exit if we don't do this
                    autoLaunchApp.addEventHandlerWhenLaunchedAppExits(() -> System.exit(0));
                    autoLaunchApp.downloadIfNecessaryAndLaunch(isAutoLaunchSnapshotsEnabled() || Boolean.parseBoolean(EntryClass.getPrefs().getPreference(PrefKeys.ENABLE_SNAPSHOTS.toString(), "false")), new CLIProgressUpdateDialog(),
                            !Internet.isConnected(), getAdditionalAutoLaunchStartupArgs());
                    break;
            }
        } catch (ParseException e) {
            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Unable to parse the command line arguments", e);
            printHelpMessage();
            System.exit(1);
        } catch (Exception e) {
            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Something went wrong while starting the launcher", e);
            printHelpMessage();
            System.exit(1);
        }
    }

    /**
     * Checks if all commandline arguments were supplied that are required for asn auto-launch
     *
     * @param commandLine The {@code CommandLine}-object to check.
     * @return {@code true} if all required command line args are supplied.
     * @see #hasAtLeastAutoLaunchArgs(CommandLine)
     */
    private static boolean hasRequiredAutoLaunchArgs(CommandLine commandLine) {
        return commandLine.hasOption(getAutoLaunchRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getAutoLaunchSnapshotRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getGroupIdOption().getOpt()) &&
                commandLine.hasOption(getArtifactIdOption().getOpt());
    }

    /**
     * Checks if at least one auto-launch command line argument was passed.
     *
     * @param commandLine The {@code CommandLine}-object to check.
     * @return {@code true} if at least one auto-launch command line argument was passed.
     * @see #hasRequiredAutoLaunchArgs(CommandLine)
     */
    private static boolean hasAtLeastAutoLaunchArgs(CommandLine commandLine) {
        return commandLine.hasOption(getAutoLaunchRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getAutoLaunchSnapshotRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getGroupIdOption().getOpt()) &&
                commandLine.hasOption(getArtifactIdOption().getOpt()) &&
                commandLine.hasOption(getEnableSnapshotsOption().getOpt()) &&
                commandLine.hasOption(getClassifierOption().getOpt()) &&
                commandLine.hasOption(getAdditionalArgsOption().getOpt());
    }

    /**
     * Prints the help message to standard out that explains the command line args
     */
    private static void printHelpMessage() {
        new HelpFormatter().printHelp(Common.getInstance().getPathAndNameOfCurrentJar(), getCliOptions());
    }

    /**
     * Creates the object that describes available command line parameters.
     * The returned instance is cached and multiple calls to this method will return the same instance (much like a singleton).
     *
     * @return The object that describes available command line parameters.
     */
    public static Options getCliOptions() {
        if (options == null) {
            options = new Options();

            options.addOption(getHelpOption());

            options.addOption(getAutoLaunchCLIModeOption());
            options.addOption(getAutoLaunchWindowModeOption());
            options.addOption(getAutoLaunchRepoUrlOption());
            options.addOption(getAutoLaunchSnapshotRepoUrlOption());
            options.addOption(getGroupIdOption());
            options.addOption(getArtifactIdOption());
            options.addOption(getClassifierOption());
            options.addOption(getEnableSnapshotsOption());
            options.addOption(getAdditionalArgsOption());

            options.addOption(getMockAppVersionOption());
            options.addOption(getMockBuildNumberOption());
            options.addOption(getMockPackagingOption());
        }
        return options;
    }

    public static Option getHelpOption() {
        if (help == null) {
            help = new Option("h", "help", false, "Displays this text");
        }
        return help;
    }

    public static Option getAutoLaunchCLIModeOption() {
        if (autoLaunchCLIMode == null) {
            autoLaunchCLIMode = new Option("cli", "autoLaunchCLIMode", false, "Puts the launcher in the auto-launch mode. No window will be shown, all output will be printed to standard out. If specified, -repoURL, -sRepoURL, -gId and -aId must be specified too. Usage of -c is optional.");
        }
        return autoLaunchCLIMode;
    }

    public static Option getAutoLaunchWindowModeOption() {
        if (autoLaunchWindowMode == null) {
            autoLaunchWindowMode = new Option("w", "autoLaunchWindowMode", false, "Puts the launcher in auto-launch mode. The launcher window will be opened to show the user the launch progress. If specified, -repoURL, -sRepoURL, -gId and -aId must be specified too. Usage of -c is optional.");
        }
        return autoLaunchWindowMode;
    }

    public static Option getAutoLaunchRepoUrlOption() {
        if (autoLaunchRepoUrl == null) {
            autoLaunchRepoUrl = new Option("repoURL", "autoLaunchRepoURL", true, "The base URL of the maven repository that contains the releases");
        }
        return autoLaunchRepoUrl;
    }

    public static Option getAutoLaunchSnapshotRepoUrlOption() {
        if (autoLaunchSnapshotRepoUrl == null) {
            autoLaunchSnapshotRepoUrl = new Option("sRepoURL", "autoLaunchSnapshotRepoURL", true, "The base URL of the maven repository that contains the snapshots");
        }
        return autoLaunchSnapshotRepoUrl;
    }

    public static Option getGroupIdOption() {
        if (groupId == null) {
            groupId = new Option("gId", "groupId", true, "The groupId of the maven artifact to launch");
        }
        return groupId;
    }

    public static Option getArtifactIdOption() {
        if (artifactId == null) {
            artifactId = new Option("aId", "artifactId", true, "The artifactId of the maven artifact to launch");
        }
        return artifactId;
    }

    public static Option getClassifierOption() {
        if (classifier == null) {
            classifier = new Option("c", "autoLaunchClassifier", true, "The classifier of the maven artifact to launch");
        }
        return classifier;
    }

    public static Option getEnableSnapshotsOption() {
        if (enableSnapshots == null) {
            enableSnapshots = new Option("s", "enableSnapshots", false, "If specified, snapshots will be enabled for this execution of the launcher. If not specified, the value found in the preferences will be used.");
        }
        return enableSnapshots;
    }

    public static Option getMockAppVersionOption() {
        if (mockAppVersion == null) {
            mockAppVersion = new Option("mav", "mockAppVersion", true, "The mock app version to use. If specified, this will override the version read from the jar's manifest. Useful for debugging and testing.");
        }
        return mockAppVersion;
    }

    public static Option getMockBuildNumberOption() {
        if (mockBuildNumber == null) {
            mockBuildNumber = new Option("mbn", "mockBuildNumber", true, "The mock build number to use. If specified, this will override the build number read from the jar's manifest. Useful for debugging and testing.");
        }
        return mockBuildNumber;
    }

    public static Option getMockPackagingOption() {
        if (mockPackaging == null) {
            mockPackaging = new Option("mpg", "mockPackaging", true, "The mock packaging to use. If specified, this will override the packaging determined from the executables file extension. Useful for debugging and testing.");
        }
        return mockPackaging;
    }

    public static Option getAdditionalArgsOption() {
        if (additionalArgs == null) {
            additionalArgs = Option.builder("args").longOpt("additionalArgs").hasArgs().desc("Additional command line args that will be passed to the main method of the maven artifact.").build();
        }
        return additionalArgs;
    }

    /**
     * Gets the user preferences
     *
     * @return The user preferences
     */
    public static Prefs getPrefs() {
        if (prefs == null) {
            prefs = new Prefs(MainWindow.class.getName());
        }
        return prefs;
    }

    /**
     * The system default locale detected at startup
     *
     * @return The system default locale detected at startup
     */
    public static Locale getSystemDefaultLocale() {
        return systemDefaultLocale;
    }

    /**
     * Returns the current instance of the view controller.
     *
     * @return The current instance of the view controller.
     */
    public static MainWindow getControllerInstance() {
        return controllerInstance;
    }

    /**
     * Returns the mode the launcher was launched in.
     *
     * @return The mode the launcher was launched in.
     */
    public static LaunchMode getLaunchMode() {
        return launchMode;
    }

    /**
     * Returns the maven coordinates of the app that was specified for auto-launch.
     *
     * @return The maven coordinates of the app that was specified for auto-launch or {@code null} if {@link #launchMode} {@code == MANUAL_WINDOW}.
     */
    public static MVNCoordinates getAutoLaunchMVNCoordinates() {
        return autoLaunchMVNCoordinates;
    }

    /**
     * The command line arguments to be passed to the auto-launch app.
     *
     * @return The command line arguments to be passed to the auto-launch app or {@code null} if {@link #launchMode} {@code == MANUAL_WINDOW}.
     */
    public static String[] getAdditionalAutoLaunchStartupArgs() {
        return additionalAutoLaunchStartupArgs;
    }

    /**
     * Checks whether the --enableSnapshots option was supplied
     *
     * @return whether the --enableSnapshots option was supplied
     */
    public static boolean isAutoLaunchSnapshotsEnabled() {
        return autoLaunchSnapshotsEnabled;
    }

    /**
     * Restarts the launcher gui. This means:
     * <ul>
     * <li>The FXML is reloaded</li>
     * <li>The controller is reinstantiated</li>
     * <li>user preferences are re-read</li>
     * </ul>
     *
     * @throws Exception If something happens during the relaunch
     */
    public static void restart() throws Exception {
        if (entryClassInstance == null) {
            throw new IllegalStateException("No GUI started");
        }
        // reload preferences
        prefs = null;
        if (getControllerInstance() != null) {
            getControllerInstance().cleanup();
        }
        if (entryClassInstance.stage.isShowing()){
            entryClassInstance.stage.hide();
        }
        entryClassInstance.start(entryClassInstance.stage);
    }

    /**
     * The stage of the current main window
     *
     * @return The stage of the current main window
     */
    public static Stage getStage() {
        if (entryClassInstance == null) {
            throw new IllegalStateException("No GUI started");
        }

        return entryClassInstance.stage;
    }

    /**
     * Returns {@code true} if this is the first launch after a launcher update.
     *
     * @return {@code true} if this is the first launch after a launcher update.
     */
    public static boolean isFirstLaunchAfterUpdate() {
        return isFirstLaunchAfterUpdate;
    }

    public static void setFirstLaunchAfterUpdate(boolean firstLaunchAfterUpdate) {
        isFirstLaunchAfterUpdate = firstLaunchAfterUpdate;
    }

    /**
     * Returns the key of the first launch message to be used .
     *
     * @return The key of the first launch message to be used .
     */
    public static String getFirstUpdateMessageTextKey() {
        return firstUpdateMessageTextKey;
    }

    public static void setFirstUpdateMessageTextKey(String firstUpdateMessageTextKey) {
        EntryClass.firstUpdateMessageTextKey = firstUpdateMessageTextKey;
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     * <p>
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param primaryStage the primary stage for this application, onto which
     *                     the application scene can be set. The primary stage will be embedded in
     *                     the browser if the application was launched as an applet.
     *                     Applications may create other stages, if needed, but they will not be
     *                     primary stages and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        entryClassInstance = this;
        stage = primaryStage;
        // get the right resource bundle
        String guiLanguageCode = EntryClass.getPrefs().getPreference(PrefKeys.GUI_LANGUAGE.toString(), "");

        if (!guiLanguageCode.equals("")) {
            // Get the specified bundle
            FOKLogger.info(MainWindow.class.getName(), "Setting language: " + guiLanguageCode);
            Locale.setDefault(new Locale(guiLanguageCode));
        } else {
            // reset to default language
            Locale.setDefault(getSystemDefaultLocale());
        }

        MainWindow.setBundle(ResourceBundle.getBundle("view.MainWindow"));

        try {
            Thread updateThread = new Thread(() -> {
                try {
                    UpdateInfo update = UpdateChecker.isUpdateAvailable(new URL(AppConfig.getInstance().getRemoteConfig().getValue("updateRepoBaseURL")),
                            AppConfig.getInstance().getRemoteConfig().getValue("groupID"), AppConfig.getInstance().getRemoteConfig().getValue("artifactID"), AppConfig.getInstance().getUpdateFileClassifier(),
                            Common.getInstance().getPackaging());
                    if (update.showAlert) {
                        Platform.runLater(() -> new UpdateAvailableDialog(update));
                    }
                } catch (MalformedURLException e) {
                    FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                }
            });
            updateThread.setName("updateThread");
            updateThread.start();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainWindow.fxml"), MainWindow.getBundle());
            Parent root = fxmlLoader.load();
            controllerInstance = fxmlLoader.getController();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("MainWindow.css").toExternalForm());

            primaryStage.setTitle(MainWindow.getBundle().getString("windowTitle"));

            primaryStage.setMinWidth(scene.getRoot().minWidth(0) + 70);
            primaryStage.setMinHeight(scene.getRoot().minHeight(0) + 70);

            primaryStage.setScene(scene);

            // Set Icon
            primaryStage.getIcons().add(new Image(MainWindow.class.getResourceAsStream("icon.png")));

            primaryStage.show();
        } catch (Exception e) {
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
        }
    }

    @Override
    public void stop() {
        try {
            UpdateChecker.cancelUpdateCompletion();
            getControllerInstance().cleanup();
        } catch (Exception e) {
            FOKLogger.log(MainWindow.class.getName(), Level.SEVERE, "An error occurred but is not relevant as we are currently in the shutdown process. Possible reasons for this exception are: You tried to modify a view but it is not shown any more on the screen; You tried to cancel the app download but no download was in progress.", e);
        }
    }

    public enum LaunchMode {
        MANUAL_WINDOW, AUTO_WINDOW, CLI
    }

    public enum PrefKeys {
        GUI_LANGUAGE("guiLanguage"),
        ENABLE_SNAPSHOTS("enableSnapshots"),
        SHOW_LAUNCHER_AGAIN("showLauncherAgain");

        private final String text;

        PrefKeys(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}

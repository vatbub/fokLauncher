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
        prefs = new Prefs(MainWindow.class.getName());
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

            if (launchMode != LaunchMode.MANUAL_WINDOW && !hasRequiredAutoLaunchArgs(commandLine)) {
                throw new IllegalArgumentException("Launcher is set to auto-launch mode but at least one required auto-launch parameter was not specified.");
            } else if (launchMode == LaunchMode.MANUAL_WINDOW && hasAtLeastAutoLaunchArgs(commandLine)) {
                FOKLogger.info(EntryClass.class.getName(), "Ignoring auto-launch args as the launcher was not put in auto-launch mode. Use either -cli or -w to put the launcher in auto-launch mode.");
            }

            if (launchMode != LaunchMode.MANUAL_WINDOW) {
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

            switch (launchMode) {
                case MANUAL_WINDOW:
                case AUTO_WINDOW:
                    try {
                        launch(args);
                        break;
                    } catch (Exception e) {
                        if (launchMode == LaunchMode.AUTO_WINDOW) {
                            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Unable to launch the gui, falling back to CLI mode...", e);
                        } else {
                            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Unable to launch the gui", e);
                            System.exit(1);
                        }
                    }
                case CLI:
                    App autoLaunchApp = new App("autoLaunchApp", getAutoLaunchMVNCoordinates());
                    // app won't exit if we don't do this
                    autoLaunchApp.addEventHandlerWhenLaunchedAppExits(() -> System.exit(0));
                    autoLaunchApp.downloadIfNecessaryAndLaunch(isAutoLaunchSnapshotsEnabled() || Boolean.parseBoolean(EntryClass.getPrefs().getPreference(PrefKeys.ENABLE_SNAPSHOTS.toString(), "false")), new CLIProgressUpdateDialog(),
                            !Internet.isConnected(), getAdditionalAutoLaunchStartupArgs());
                    break;
            }
        } catch (ParseException e) {
            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Unable to parse the command line arguments", e);
            printHelpMessage();
        } catch (Exception e) {
            FOKLogger.log(EntryClass.class.getName(), Level.SEVERE, "Something went wrong while starting the launcher", e);
            printHelpMessage();
        }
    }

    private static boolean hasRequiredAutoLaunchArgs(CommandLine commandLine) {
        return commandLine.hasOption(getAutoLaunchRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getAutoLaunchSnapshotRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getGroupIdOption().getOpt()) &&
                commandLine.hasOption(getArtifactIdOption().getOpt());
    }

    private static boolean hasAtLeastAutoLaunchArgs(CommandLine commandLine) {
        return commandLine.hasOption(getAutoLaunchRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getAutoLaunchSnapshotRepoUrlOption().getOpt()) &&
                commandLine.hasOption(getGroupIdOption().getOpt()) &&
                commandLine.hasOption(getArtifactIdOption().getOpt()) &&
                commandLine.hasOption(getEnableSnapshotsOption().getOpt()) &&
                commandLine.hasOption(getClassifierOption().getOpt()) &&
                commandLine.hasOption(getAdditionalArgsOption().getOpt());
    }

    private static void printHelpMessage() {
        new HelpFormatter().printHelp(Common.getInstance().getPathAndNameOfCurrentJar(), getCliOptions());
    }

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

    public static Prefs getPrefs() {
        return prefs;
    }

    public static Locale getSystemDefaultLocale() {
        return systemDefaultLocale;
    }

    public static MainWindow getControllerInstance() {
        return controllerInstance;
    }

    public static LaunchMode getLaunchMode() {
        return launchMode;
    }

    public static MVNCoordinates getAutoLaunchMVNCoordinates() {
        return autoLaunchMVNCoordinates;
    }

    public static String[] getAdditionalAutoLaunchStartupArgs() {
        return additionalAutoLaunchStartupArgs;
    }

    public static boolean isAutoLaunchSnapshotsEnabled() {
        return autoLaunchSnapshotsEnabled;
    }

    public static void restart() throws Exception {
        if (entryClassInstance == null) {
            throw new IllegalStateException("No GUI started");
        }

        entryClassInstance.start(entryClassInstance.stage);
    }

    public static Stage getStage() {
        if (entryClassInstance == null) {
            throw new IllegalStateException("No GUI started");
        }

        return entryClassInstance.stage;
    }

    public static boolean isFirstLaunchAfterUpdate() {
        return isFirstLaunchAfterUpdate;
    }

    public static void setFirstLaunchAfterUpdate(boolean firstLaunchAfterUpdate) {
        isFirstLaunchAfterUpdate = firstLaunchAfterUpdate;
    }

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
        }

        MainWindow.setBundle(ResourceBundle.getBundle("view.MainWindow"));

        try {
            Thread updateThread = new Thread(() -> {
                try {
                    UpdateInfo update = UpdateChecker.isUpdateAvailable(new URL(AppConfig.getRemoteConfig().getValue("updateRepoBaseURL")),
                            AppConfig.getRemoteConfig().getValue("groupID"), AppConfig.getRemoteConfig().getValue("artifactID"), AppConfig.getUpdateFileClassifier(),
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
            if (getControllerInstance().getCurrentlySelectedApp() != null) {
                getControllerInstance().getCurrentlySelectedApp().cancelDownloadAndLaunch(getControllerInstance());
            }
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

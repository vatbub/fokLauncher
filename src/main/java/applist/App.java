package applist;

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


import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import com.github.vatbub.common.updater.HidableUpdateProgressDialog;
import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.VersionList;
import config.AppConfig;
import extended.VersionMenuItem;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.SystemUtils;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.Nullable;
import view.EntryClass;
import view.MainWindow;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Represents an app that is deployed to a maven repository.
 */
public class App {
    private final List<Runnable> eventHandlersWhenLaunchedAppExits = new ArrayList<>();
    private MVNMetadataFile releaseRepoMetadataFile;
    private MVNMetadataFile snapshotRepoMetadataFile;
    private LocalMetadataFile localMetadataFile;
    private File importFile;
    private String name;
    private boolean cancelDownloadAndLaunch;
    private MVNCoordinates mvnCoordinates;
    private URL additionalInfoURL;
    private URL changelogURL;
    private boolean specificVersionListLoaded = false;
    private boolean deletableVersionListLoaded = false;
    private ContextMenu contextMenuCache;

    /**
     * Creates a new App with the specified name.
     *
     * @param name The name of the app.
     */
    public App(String name) {
        this(name, new MVNCoordinates());
    }

    /**
     * Creates a new App with the specified maven coordinates.
     *
     * @param name           The name of the app
     * @param mvnCoordinates The maven coordinates of the app
     */
    public App(String name, MVNCoordinates mvnCoordinates) {
        this(name, mvnCoordinates, null);
    }

    /**
     * Creates a new App with the specified maven coordinates.
     *
     * @param name              The name of the app
     * @param mvnCoordinates    The maven coordinates of the app
     * @param additionalInfoURL The url to a webpage where the user finds additional info
     *                          about this app.
     */
    public App(String name, MVNCoordinates mvnCoordinates, URL additionalInfoURL) {
        this(name, mvnCoordinates, additionalInfoURL, null);
    }

    /**
     * Creates a new App with the specified maven coordinates.
     *
     * @param name              The name of the app
     * @param mvnCoordinates    The maven coordinates of the app
     * @param additionalInfoURL The url to a webpage where the user finds additional info
     *                          about this app.
     * @param changelogURL      The url to the webpage where the user finds a changelog for this app.
     */
    public App(String name, MVNCoordinates mvnCoordinates, URL additionalInfoURL, URL changelogURL) {
        setName(name);
        setMvnCoordinates(mvnCoordinates);
        setAdditionalInfoURL(additionalInfoURL);
        setChangelogURL(changelogURL);
    }

    /**
     * Creates a new app and imports its info from the specified file. If the
     * specified file cannot be imported for some reason, this app will
     * automatically be deleted from the imported apps list.
     *
     * @param fileToImportFrom The file to import the info from. Must be a *.foklauncher file
     * @throws IOException If the specified file is not a file (but a directory) or if
     *                     the launcher has no permission to read the file and this app
     *                     cannot be deleted from the imported app list for some reason.
     * @see App#removeFromImportedAppList()
     */
    public App(File fileToImportFrom) throws IOException {
        try {
            this.importInfo(fileToImportFrom);
        } catch (IOException e) {
            FOKLogger.log(App.class.getName(), Level.SEVERE,
                    "The app that should be imported from " + fileToImportFrom.getAbsolutePath()
                            + " is automatically deleted from the imported apps list because something went wrong during the import. We probably didn't find the file to import.",
                    e);
            this.removeFromImportedAppList();
            // Propagate the Exception
            throw e;
        }
    }

    /**
     * Returns a combined list of imported apps and apps from the server.
     *
     * @return A combined list of imported apps and apps from the server.
     * @throws JDOMException If the xml-app list on the server or the xml file containing
     *                       info about the imported apps is malformed
     * @throws IOException   If the app list or metadata of some apps cannot be downloaded
     *                       or if the xml file containing the info about the imported
     *                       apps cannot be read.
     * @see App#getOnlineAppList()
     * @see App#getImportedAppList()
     */
    public static AppList getAppList() throws JDOMException, IOException {
        AppList res = getOnlineAppList();
        res.addAll(getImportedAppList());
        return res;
    }

    /**
     * Get a {@link List} of available apps from the server.
     *
     * @return A {@link List} of available apps from the server.
     * @throws JDOMException If the xml-app list on the server is malformed
     * @throws IOException   If the app list or metadata of some apps cannot be
     *                       downloaded.
     */
    public static AppList getOnlineAppList() throws JDOMException, IOException {
        return new AppListFile().getAppList();
    }

    /**
     * Returns the list of apps that were imported by the user. If any app
     * cannot be imported, it will be removed from the list permanently
     *
     * @return The list of apps that were imported by the user.
     */
    @SuppressWarnings("unused")
    public static AppList getImportedAppList() {
        AppList res = new ImportedAppListFile().getAppList();
        return res == null ? new AppList() : res;
    }

    /**
     * Adds the app specified in the metadata file to the list of imported apps.
     *
     * @param infoFile The metadata file that contains the info about the app. The file should be in the {@code *.foklauncher} format.
     * @throws IOException If the file cannot be read for any reason
     */
    public static void addImportedApp(File infoFile) throws IOException {
        ImportedAppListFile importedAppListFile = new ImportedAppListFile();
        importedAppListFile.getAppList().addAndCheckForDuplicateImports(new App(infoFile));
        importedAppListFile.saveFile();
    }

    /**
     * Clears all cached data about the app and forces it to be reloaded.
     * Subsequent calls to the data will take longer as the data has to be reloaded.
     * The cache is created on a lazy basis, meaning that data is only cached once a call is made to it.
     * Cached data includes:
     * <ul>
     * <li>Release metadata from the release maven repo</li>
     * <li>Snapshot metadata from the snapshot maven repo</li>
     * <li>Metadata about the locally installed versions of the app</li>
     * <li>The JavaFX context menu of this application</li>
     * </ul>
     */
    public void clearCache() {
        releaseRepoMetadataFile = null;
        snapshotRepoMetadataFile = null;
        localMetadataFile = null;
        contextMenuCache = null;
        deletableVersionListLoaded = false;
        specificVersionListLoaded = false;
    }

    /**
     * Returns the changelog URL of this app
     *
     * @return The changelog URL of this app
     */
    public URL getChangelogURL() {
        return changelogURL;
    }

    /**
     * Sets the changelog URL of this app
     *
     * @param changelogURL The changelog URL to set
     */
    public void setChangelogURL(URL changelogURL) {
        this.changelogURL = changelogURL;
    }

    /**
     * returns the name of this app
     *
     * @return The name of this app
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this app
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a list that contains all versions of the artifact available
     * online
     *
     * @return A list that contains all versions of the artifact available
     * online
     * @throws IOException   If the maven metadata file can't be read or downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public VersionList getAllOnlineVersions() throws JDOMException, IOException {
        if (releaseRepoMetadataFile == null) {
            releaseRepoMetadataFile = new MVNMetadataFile(getMvnCoordinates(), false);
        }
        return releaseRepoMetadataFile.getVersionList().clone();
    }

    /**
     * @return the latestOnlineVersion
     * @throws IOException   If the maven metadata file can't be read or downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public Version getLatestOnlineVersion() throws JDOMException, IOException {
        if (releaseRepoMetadataFile == null) {
            releaseRepoMetadataFile = new MVNMetadataFile(getMvnCoordinates(), false);
        }
        Version res = releaseRepoMetadataFile.getLatest().clone();

        if (res.isSnapshot()) {
            throw new IllegalStateException(
                    "Latest version in this repository is a snapshot and not a release. This might happen if you host snapshots and releases in the same repository (which is not recommended). If you still need this case to be covered, please submit an issue at https://github.com/vatbub/fokLauncher/issues");
        }

        return res;
    }

    /**
     * @return the latestOnlineSnapshotVersion
     * @throws IOException   If the maven metadata file can't be read or downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public Version getLatestOnlineSnapshotVersion() throws JDOMException, IOException {
        if (snapshotRepoMetadataFile == null) {
            snapshotRepoMetadataFile = new MVNMetadataFile(getMvnCoordinates(), true);
        }
        return snapshotRepoMetadataFile.getLatest().clone();
    }

    /**
     * Returns the currently installed version of the app or {@code null} if the
     * app is not yet installed locally. Takes snapshots into account.
     *
     * @return the currentlyInstalledVersion
     * @see #isPresentOnHarddrive()
     */
    public Version getCurrentlyInstalledVersion() {
        return getCurrentlyInstalledVersion(true);
    }

    /**
     * Returns the currently installed version of the app or {@code null} if the
     * app is not yet installed locally.
     *
     * @param snapshotsEnabled If {@code false}, snapshots will be removed from this list.
     * @return the currentlyInstalledVersion
     * @see #isPresentOnHarddrive()
     */
    public Version getCurrentlyInstalledVersion(boolean snapshotsEnabled) {
        try {
            VersionList version = getCurrentlyInstalledVersions();
            if (!snapshotsEnabled) {
                version.removeSnapshots();
            }
            return Collections.max(version);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a list of currently installed versions
     *
     * @return A list of currently installed versions
     */
    public VersionList getCurrentlyInstalledVersions() {
        if (localMetadataFile == null && !loadLocalMetadataFile()) {
            // something went wrong, exception already logged
            return null;
        }
        return localMetadataFile.getVersionList().clone();
    }

    /**
     * Returns the absolute path object for the folder that contains this app's jar files and metadata
     *
     * @return The absolute path object for the folder that contains this app's jar files and metadata
     */
    private Path getAbsolutePathToSubfolderToSaveApps() {
        return Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve(getSubfolderToSaveApps());
    }

    /**
     * Returns the location of this app's metadata file on disk
     *
     * @return The location of this app's metadata file on disk
     */
    private File getLocationOfLocalMetadataFile() {
        return getAbsolutePathToSubfolderToSaveApps().resolve(AppConfig.getRemoteConfig().getValue("appMetadataFileName")).toFile();
    }

    /**
     * Loads this app's local metadata file from disk
     *
     * @return {@code true} if the file was loaded successfully, {@code false} otherwise. Exceptions are logged to the log
     */
    private boolean loadLocalMetadataFile() {
        try {
            localMetadataFile = new LocalMetadataFile(getLocationOfLocalMetadataFile());
            return true;
        } catch (JDOMException | IOException e) {
            FOKLogger.log(App.class.getName(), Level.SEVERE, "Cannot retrieve currently installed version of app " + this.getName()
                    + ", probably because it is not installed.", e);
            return false;
        }
    }

    /**
     * Returns the maven coordinates of this app
     *
     * @return The maven coordinates of this app
     */
    public MVNCoordinates getMvnCoordinates() {
        return mvnCoordinates;
    }

    /**
     * Sets the maven coordinates of this app
     *
     * @param mvnCoordinates The maven coordinates to set
     */
    public void setMvnCoordinates(MVNCoordinates mvnCoordinates) {
        this.mvnCoordinates = mvnCoordinates;
    }

    /**
     * Returns {@code true} if this app was imported from a {@code *.foklauncher} file, {@code false} if not
     *
     * @return {@code true} if this app was imported from a {@code *.foklauncher} file, {@code false} if not
     */
    public boolean isImported() {
        return getImportFile() != null;
    }

    /**
     * Returns the file this app was imported from or {@code null} if {@link #isImported()} {@code  == false}.
     *
     * @return The file this app was imported from or {@code null} if {@link #isImported()} {@code  == false}.
     */
    public File getImportFile() {
        return importFile;
    }

    /**
     * During context menu creation, a list of all available release and snapshot versions (if enabled) is created and cached.
     * Returns {@code true} if this list was already created and cached and {@code false} if not.
     *
     * @return {@code true} if the specific list was already created and cached and {@code false} if not.
     */
    public boolean isSpecificVersionListLoaded() {
        return specificVersionListLoaded;
    }

    /**
     * During context menu creation, a list of all available release and snapshot versions (if enabled) is created and cached.
     * This method sets the value if the list was loaded and cached or not.
     *
     * @param specificVersionListLoaded {@code true} if the specific list was already created and cached and {@code false} if not.
     */
    public void setSpecificVersionListLoaded(boolean specificVersionListLoaded) {
        this.specificVersionListLoaded = specificVersionListLoaded;
    }

    /**
     * During context menu creation, a list of all locally installed versions is created and cached.
     * Returns {@code true} if this list was already created and cached and {@code false} if not.
     *
     * @return Returns {@code true} if the deletable version list was already created and cached and {@code false} if not.
     */
    public boolean isDeletableVersionListLoaded() {
        return deletableVersionListLoaded;
    }

    /**
     * During context menu creation, a list of all locally installed versions is created and cached.
     * This method sets the value if the list was loaded and cached or not.
     *
     * @param deletableVersionListLoaded {@code true} if the deletable version list was already created and cached and {@code false} if not.
     */
    public void setDeletableVersionListLoaded(boolean deletableVersionListLoaded) {
        this.deletableVersionListLoaded = deletableVersionListLoaded;
    }

    /**
     * Returns the additional info URL
     *
     * @return The additional info URL
     */
    public URL getAdditionalInfoURL() {
        return additionalInfoURL;
    }

    /**
     * Sets the additional info URL
     *
     * @param additionalInfoURL The additionalInfoURL to set
     */
    public void setAdditionalInfoURL(URL additionalInfoURL) {
        this.additionalInfoURL = additionalInfoURL;
    }

    /**
     * Checks if any version of this app is already downloaded
     *
     * @return {@code true} if any version of this app is already downloaded,
     * {@code false} otherwise.
     */
    public boolean isPresentOnHarddrive() {
        // Check if metadata file is present
        return this.getCurrentlyInstalledVersion() != null;
    }

    /**
     * Checks if the specified version of this app is already downloaded
     *
     * @param ver The version to check
     * @return {@code true} if the specified version of this app is already
     * downloaded, {@code false} otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPresentOnHarddrive(Version ver) {
        // something went wrong, exception already logged
        return (localMetadataFile != null || loadLocalMetadataFile()) && localMetadataFile.getVersionList().contains(ver);
    }

    /**
     * Downloads the version info and saves it as a xml file in the app folder.
     *
     * @param versionToGet The version to get the info for
     * @throws IOException If the maven metadata file cannot be downloaded
     */
    private void downloadVersionInfo(Version versionToGet)
            throws IOException {
        if (localMetadataFile == null && !loadLocalMetadataFile()) {
            // something went wrong, exception already logged
            localMetadataFile = new LocalMetadataFile();
            localMetadataFile.setVersionList(new VersionList());
        }
        localMetadataFile.setMvnCoordinates(getMvnCoordinates());

        boolean versionFound = false;

        for (Version ver : localMetadataFile.getVersionList()) {
            if (ver.equals(versionToGet)) {
                versionFound = true;
                break;
            }
        }

        // Check if the specified version is already present
        if (!versionFound) {
            localMetadataFile.getVersionList().add(versionToGet);
        }
        localMetadataFile.saveFile(getLocationOfLocalMetadataFile());
    }

    /**
     * Checks if this artifact needs to be downloaded prior to launching it.
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @return {@code true} if this artifact needs to be downloaded prior to
     * launching it, {@code false} otherwise
     */
    public boolean downloadRequired(boolean snapshotsEnabled) {
        return (this.isPresentOnHarddrive() && (!snapshotsEnabled) && !this.getCurrentlyInstalledVersions().containsRelease()) || (!(this.isPresentOnHarddrive() && (!snapshotsEnabled)) && !(this.isPresentOnHarddrive() && snapshotsEnabled));
    }

    /**
     * Checks if an update is available for this artifact.
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @return {@code true} if an update is available, {@code false} otherwise
     * @throws JDOMException If the maven metadata file is malformed
     * @throws IOException   If the maven metadata file cannot be downloaded
     */
    public boolean updateAvailable(boolean snapshotsEnabled) throws JDOMException, IOException {
        Version onlineVersion;

        if (snapshotsEnabled) {
            onlineVersion = this.getLatestOnlineSnapshotVersion();
        } else {
            onlineVersion = this.getLatestOnlineVersion();
        }

        return onlineVersion.compareTo(this.getCurrentlyInstalledVersion(snapshotsEnabled)) > 0;
    }

    /**
     * Checks if an update is available for the specified artifact version.
     *
     * @param versionToCheck The version to be checked.
     * @return {@code true} if an update is available, {@code false} otherwise
     */
    @SuppressWarnings("unused")
    public boolean updateAvailable(Version versionToCheck) {
        return versionToCheck.compareTo(this.getCurrentlyInstalledVersion()) > 0;
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards. Does not
     * take snapshots into account
     *
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    @SuppressWarnings("unused")
    public void downloadIfNecessaryAndLaunch(String... startupArgs) throws IOException, JDOMException {
        downloadIfNecessaryAndLaunch(null, startupArgs);
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards. Does not
     * take snapshots into account
     *
     * @param gui A reference to a gui that shows the update and launch
     *            progress.
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public void downloadIfNecessaryAndLaunch(HidableUpdateProgressDialog gui, String... startupArgs) throws IOException, JDOMException {
        downloadIfNecessaryAndLaunch(false, gui, startupArgs);
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards. Only
     * takes snapshots into account
     *
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    @SuppressWarnings("unused")
    public void downloadSnapshotIfNecessaryAndLaunch(String... startupArgs) throws IOException, JDOMException {
        downloadSnapshotIfNecessaryAndLaunch(null, startupArgs);
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards. Only
     * takes snapshots into account
     *
     * @param gui A reference to a gui that shows the update and launch
     *            progress.
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public void downloadSnapshotIfNecessaryAndLaunch(HidableUpdateProgressDialog gui, String... startupArgs)
            throws IOException, JDOMException {
        downloadIfNecessaryAndLaunch(true, gui, startupArgs);
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @param gui              A reference to a gui that shows the update and launch
     *                         progress.
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public void downloadIfNecessaryAndLaunch(boolean snapshotsEnabled, HidableUpdateProgressDialog gui, String... startupArgs)
            throws IOException, JDOMException {
        downloadIfNecessaryAndLaunch(snapshotsEnabled, gui, false, startupArgs);
    }

    /**
     * Launches the artifact and forces offline mode. Does not take snapshots
     * into account.
     *
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    @SuppressWarnings("unused")
    public void launchWithoutDownload(String... startupArgs) throws IOException, JDOMException {
        launchWithoutDownload(false, startupArgs);
    }

    /**
     * Launches the artifact and forces offline mode. Only launches snapshots.
     *
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    @SuppressWarnings("unused")
    public void launchSnapshotWithoutDownload(String... startupArgs) throws IOException, JDOMException {
        launchWithoutDownload(true, startupArgs);
    }

    /**
     * Launches the artifact and forces offline mode
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public void launchWithoutDownload(boolean snapshotsEnabled, String... startupArgs)
            throws IOException, JDOMException {
        launchWithoutDownload(snapshotsEnabled, null, startupArgs);
    }

    /**
     * Launches the artifact and forces offline mode
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @param gui              A reference to a gui that shows the update and launch
     *                         progress.
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public void launchWithoutDownload(boolean snapshotsEnabled, HidableUpdateProgressDialog gui, String... startupArgs)
            throws IOException, JDOMException {
        downloadIfNecessaryAndLaunch(snapshotsEnabled, gui, true, startupArgs);
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @param gui              A reference to a gui that shows the update and launch
     *                         progress.
     * @param disableDownload  If {@code true}, the method will be forced to work offline.
     * @throws IOException   If the maven metadata cannot be downloaded
     * @throws JDOMException If the maven metadata file is malformed
     */
    public void downloadIfNecessaryAndLaunch(boolean snapshotsEnabled, HidableUpdateProgressDialog gui,
                                             boolean disableDownload, String... startupArgs) throws IOException, JDOMException {
        Version versionToLaunch;

        if (!disableDownload) {
            if (snapshotsEnabled) {
                versionToLaunch = this.getLatestOnlineSnapshotVersion();
            } else {
                versionToLaunch = this.getLatestOnlineVersion();
            }
        } else {
            versionToLaunch = this.getCurrentlyInstalledVersion();
        }

        downloadIfNecessaryAndLaunch(gui, versionToLaunch, disableDownload, startupArgs);
    }

    /**
     * Downloads the artifact if necessary and launches it afterwards
     *
     * @param gui             A reference to a gui that shows the update and launch
     *                        progress.
     * @param versionToLaunch The version of the app to be downloaded and launched.
     * @param disableDownload If {@code true}, the method will be forced to work offline.
     * @throws IOException If the maven metadata cannot be downloaded
     */
    public void downloadIfNecessaryAndLaunch(HidableUpdateProgressDialog gui, Version versionToLaunch,
                                             boolean disableDownload, String... startupArgs) throws IOException {
        cancelDownloadAndLaunch = false;

        // Continue by default, only cancel, when user cancelled
        boolean downloadPerformed = true;
        if (!disableDownload && !this.isPresentOnHarddrive(versionToLaunch)) {
            // app not downloaded at all or needs to be updated
            downloadPerformed = this.download(versionToLaunch, gui);
        } else if (!this.isPresentOnHarddrive(versionToLaunch)) {
            // Download is disabled and app needs to be downloaded
            throw new IllegalStateException(
                    "The artifact needs to be downloaded prior to launch but download is disabled.");
        }
        if (!downloadPerformed) {
            // Download was cancelled by the user so return
            return;
        }

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return;
        }

        if (gui != null) {
            gui.launchStarted();
        }

        String jarFileName = getAbsolutePathToSubfolderToSaveApps().resolve(getMvnCoordinates().getJarFileName(versionToLaunch)).toAbsolutePath().toString();

        // throw exception if the jar can't be found for some unlikely reason
        if (!(new File(jarFileName)).exists()) {
            throw new FileNotFoundException(jarFileName);
        }

        // Set implicit exit = false if handlers are defined when the app exits
        Platform.setImplicitExit(!this.eventHandlersWhenLaunchedAppExitsAttached());

        int expectedLength = 5;
        if (startupArgs != null) {
            expectedLength += startupArgs.length;
        }
        List<String> finalCommand = new ArrayList<>(expectedLength);
        finalCommand.add("java");
        finalCommand.add("-jar");
        finalCommand.add(jarFileName);
        finalCommand.add("disableUpdateChecks");
        finalCommand.add("locale=" + Locale.getDefault().getLanguage());
        if (startupArgs != null) {
            finalCommand.addAll(Arrays.asList(startupArgs));
        }

        FOKLogger.info(App.class.getName(), "Launching app using the command: " + String.join(" ", finalCommand));

        ProcessBuilder pb = new ProcessBuilder(finalCommand.toArray(new String[0])).inheritIO();
        // MainWindow.getMetricsRegistry().counter("foklauncher.applaunches." + getMavenCoordinateString(versionToLaunch)).inc();

        FOKLogger.info(App.class.getName(), "Launching application " + getMvnCoordinates().getJarFileName(versionToLaunch));

        FOKLogger.info(getClass().getName(), "------------------------------------------------------------------");
        FOKLogger.info(getClass().getName(), "The following output is coming from " + getMvnCoordinates().getJarFileName(versionToLaunch));
        FOKLogger.info(getClass().getName(), "------------------------------------------------------------------");

        if (gui != null) {
            gui.hide();
        }
        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
        }

        // Check the status code of the process
        if (process.exitValue() != 0) {
            // Something went wrong
            FOKLogger.log(App.class.getName(), Level.SEVERE, "The java process returned with exit code " + process.exitValue());
            if (gui != null) {
                gui.showErrorMessage(
                        "Something happened while launching the selected app. Try to launch it again and if this error occurs again, try to delete the app and download it again.");
            }
        }

        fireLaunchedAppExits();
    }

    /**
     * Downloads this artifact. Does not take snapshots into account.
     *
     * @return {@code true} if the download finished successfully, {@code false}
     * if the download was cancelled using
     * {@link #cancelDownloadAndLaunch()}
     * @throws IOException   If the version info cannot be read
     * @throws JDOMException If the version xml is malformed
     */
    @SuppressWarnings("unused")
    public boolean download() throws IOException, JDOMException {
        return download(null);
    }

    /**
     * Downloads this artifact. Does not take snapshots into account.
     *
     * @param gui The {@link HidableUpdateProgressDialog} that represents the
     *            gui to inform the user about the progress.
     * @return {@code true} if the download finished successfully, {@code false}
     * if the download was cancelled using
     * {@link #cancelDownloadAndLaunch()}
     * @throws IOException   If the version info cannot be read
     * @throws JDOMException If the version xml is malformed
     */
    public boolean download(@Nullable HidableUpdateProgressDialog gui) throws IOException, JDOMException {
        return download(this.getLatestOnlineVersion(), gui);
    }

    /**
     * Downloads this artifact. Only takes snapshots into account.
     *
     * @return {@code true} if the download finished successfully, {@code false}
     * if the download was cancelled using
     * {@link #cancelDownloadAndLaunch()}
     * @throws IOException   If the version info cannot be read
     * @throws JDOMException If the version xml is malformed
     */
    @SuppressWarnings("unused")
    public boolean downloadSnapshot() throws IOException, JDOMException {
        return downloadSnapshot(null);
    }

    /**
     * Downloads this artifact. Only takes snapshots into account.
     *
     * @param gui The {@link HidableUpdateProgressDialog} that represents the
     *            gui to inform the user about the progress.
     * @return {@code true} if the download finished successfully, {@code false}
     * if the download was cancelled using
     * {@link #cancelDownloadAndLaunch()}
     * @throws IOException   If the version info cannot be read
     * @throws JDOMException If the version xml is malformed
     */
    public boolean downloadSnapshot(@Nullable HidableUpdateProgressDialog gui) throws IOException, JDOMException {
        return download(this.getLatestOnlineSnapshotVersion(), gui);
    }

    /**
     * Downloads this artifact.
     *
     * @param versionToDownload The {@link Version} to be downloaded.
     * @param gui               The {@link HidableUpdateProgressDialog} that represents the
     *                          gui to inform the user about the progress.
     * @return {@code true} if the download finished successfully, {@code false}
     * if the download was cancelled using
     * {@link #cancelDownloadAndLaunch()}
     * @throws IOException If the version info cannot be read
     */
    public boolean download(Version versionToDownload, @Nullable HidableUpdateProgressDialog gui)
            throws IOException {
        if (gui != null) {
            gui.preparePhaseStarted();
        }

        URL repoBaseURL;
        URL artifactURL;

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return false;
        }

        if (versionToDownload.isSnapshot()) {
            // Snapshot
            repoBaseURL = getMvnCoordinates().getSnapshotRepoBaseURL();
        } else {
            // Not a snapshot
            repoBaseURL = getMvnCoordinates().getRepoBaseURL();
        }

        // Construct the download url
        StringBuilder artifactURLBuilder = new StringBuilder(repoBaseURL.toString())
                .append("/")
                .append(getMvnCoordinates().getGroupId().replace('.', '/'))
                .append("/")
                .append(getMvnCoordinates().getArtifactId())
                .append("/")
                .append(versionToDownload.getVersion())
                .append("/")
                .append(getMvnCoordinates().getArtifactId())
                .append("-")
                .append(versionToDownload.toString());
        if (getMvnCoordinates().getClassifier() != null) {
            artifactURLBuilder.append("-")
                    .append(getMvnCoordinates().getClassifier());
        }

        artifactURLBuilder.append(".jar");
        artifactURL = new URL(artifactURLBuilder.toString());

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return false;
        }

        // Create empty file
        File outputFile = getAbsolutePathToSubfolderToSaveApps().resolve(getMvnCoordinates().getJarFileName(versionToDownload)).toFile();

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return false;
        }

        // Download
        if (gui != null) {
            gui.downloadStarted();
        }

        FOKLogger.info(App.class.getName(), "Downloading artifact from " + artifactURL.toString() + "...");
        FOKLogger.info(App.class.getName(), "Downloading to: " + outputFile.getAbsolutePath());

        HttpURLConnection httpConnection = (HttpURLConnection) (artifactURL.openConnection());
        long completeFileSize = httpConnection.getContentLength();
        //noinspection ResultOfMethodCallIgnored
        outputFile.getParentFile().mkdirs();

        try (BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream())) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 1024)) {
                    byte[] data = new byte[1024];
                    long downloadedFileSize = 0;
                    int x;
                    while ((x = in.read(data, 0, 1024)) >= 0) {
                        downloadedFileSize += x;

                        // update progress bar
                        if (gui != null) {
                            gui.downloadProgressChanged(downloadedFileSize / 1024.0,
                                    completeFileSize / 1024.0);
                        }

                        bufferedOutputStream.write(data, 0, x);

                        // Perform Cancel if requested
                        if (cancelDownloadAndLaunch) {
                            Files.delete(outputFile.toPath());
                            if (gui != null) {
                                gui.operationCanceled();
                            }
                            return false;
                        }
                    }
                }
            }
        }

        // download version info
        downloadVersionInfo(versionToDownload);

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return false;
        }

        // Perform install steps (none at the moment)
        if (gui != null) {
            gui.installStarted();
        }

        // MainWindow.getMetricsRegistry().counter("foklauncher.appdownloads." + getMavenCoordinateString(versionToDownload)).inc();
        return true;

    }

    /**
     * Cancels the download and launch process.
     */
    @SuppressWarnings("unused")
    public void cancelDownloadAndLaunch() {
        cancelDownloadAndLaunch(null);
    }

    /**
     * Cancels the download and launch process.
     *
     * @param gui The {@link HidableUpdateProgressDialog} that represents the
     *            gui to inform the user about the progress.
     */
    public void cancelDownloadAndLaunch(HidableUpdateProgressDialog gui) {
        cancelDownloadAndLaunch = true;

        if (gui != null) {
            gui.cancelRequested();
        }
    }

    /**
     * Deletes the specified artifact version.
     *
     * @param versionToDelete The version to be deleted.
     * @return {@code true} if the artifact was successfully deleted, {@code false} otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean delete(Version versionToDelete) throws IOException {
        if (localMetadataFile == null && !loadLocalMetadataFile()) {
            // something went wrong, exception already logged
            return false;
        }

        localMetadataFile.getVersionList().remove(versionToDelete);
        localMetadataFile.saveFile(getLocationOfLocalMetadataFile());

        // Delete the file
        String appFileName;
        if (getMvnCoordinates().getClassifier() == null) {
            // No classifier
            appFileName = getMvnCoordinates().getArtifactId() + "-" + versionToDelete.toString() + ".jar";
        } else {
            appFileName = getMvnCoordinates().getArtifactId() + "-" + versionToDelete.toString() + "-" + getMvnCoordinates().getClassifier()
                    + ".jar";
        }

        Files.delete(getAbsolutePathToSubfolderToSaveApps().resolve(appFileName));
        return true;
    }

    /**
     * Adds a handler for the event that the launched app exits again.
     *
     * @param handler The handler to add.
     */
    public void addEventHandlerWhenLaunchedAppExits(Runnable handler) {
        // Only add handler if it was not already added
        if (!this.isEventHandlerWhenLaunchedAppExitsAttached(handler)) {
            eventHandlersWhenLaunchedAppExits.add(handler);
        }
    }

    /**
     * Removes a handler for the event that the launched app exits again.
     *
     * @param handler The handler to remove.
     */
    public void removeEventHandlerWhenLaunchedAppExits(Runnable handler) {
        // Only remove handler if it was already added
        if (this.isEventHandlerWhenLaunchedAppExitsAttached(handler)) {
            eventHandlersWhenLaunchedAppExits.remove(handler);
        }
    }

    /**
     * checks if the specified handler is already attached to the event that the
     * launched app exits again.
     *
     * @param handler The handler to be checked.
     * @return {@code true} if the handler is already attached, {@code false}
     * otherwise
     */
    public boolean isEventHandlerWhenLaunchedAppExitsAttached(Runnable handler) {
        return eventHandlersWhenLaunchedAppExits.contains(handler);
    }

    /**
     * Checks if any handler is attached to the event that the launched app
     * exits again.
     *
     * @return {@code true} if any event handler is attached, {@code false}, if
     * no event handler is attached.
     */
    public boolean eventHandlersWhenLaunchedAppExitsAttached() {
        return !eventHandlersWhenLaunchedAppExits.isEmpty();
    }

    /**
     * Fires all handlers registered for the launchedAppExits event.
     */
    private void fireLaunchedAppExits() {
        FOKLogger.info(App.class.getName(), "The launched app exited and the LaunchedAppExits event is now fired.");
        for (Runnable handler : eventHandlersWhenLaunchedAppExits) {
            Platform.runLater(handler);
        }
    }

    /**
     * Exports the info of this app to a *.foklauncher file
     *
     * @param fileToWrite The {@link File} to be written to. If the file already exists
     *                    on the disk, it will be overwritten.
     * @throws IOException If something happens while saving the file
     */
    public void exportInfo(File fileToWrite) throws IOException {
        FoklauncherFile foklauncherFile = new FoklauncherFile(fileToWrite);

        foklauncherFile.setValue(FoklauncherFile.Property.NAME, this.getName());
        foklauncherFile.setValue(FoklauncherFile.Property.REPO_BASE_URL, getMvnCoordinates().getRepoBaseURL().toString());
        foklauncherFile.setValue(FoklauncherFile.Property.SNAPSHOT_BASE_URL, getMvnCoordinates().getSnapshotRepoBaseURL().toString());
        foklauncherFile.setValue(FoklauncherFile.Property.GROUP_ID, getMvnCoordinates().getGroupId());
        foklauncherFile.setValue(FoklauncherFile.Property.ARTIFACT_ID, getMvnCoordinates().getArtifactId());
        foklauncherFile.setValue(FoklauncherFile.Property.CLASSIFIER, getMvnCoordinates().getClassifier());
        if (this.getAdditionalInfoURL() != null) {
            foklauncherFile.setValue(FoklauncherFile.Property.ADDITIONAL_INFO_URL, this.getAdditionalInfoURL().toString());
        }
        if (this.getChangelogURL() != null) {
            foklauncherFile.setValue(FoklauncherFile.Property.CHANGELOG_URL, this.getChangelogURL().toString());
        }

        foklauncherFile.save();
    }

    /**
     * Imports the info of this app from a *.foklauncher file.
     *
     * @param fileToImport The {@link File} to be read from.
     * @throws IOException If the specified file is not a file (but a directory) or if
     *                     the launcher has no permission to read the file.
     */
    private void importInfo(File fileToImport) throws IOException {
        this.importFile = fileToImport;
        FoklauncherFile foklauncherFile = new FoklauncherFile(fileToImport);

        this.setName(foklauncherFile.getValue(FoklauncherFile.Property.NAME));
        this.setMvnCoordinates(new MVNCoordinates());
        getMvnCoordinates().setRepoBaseURL(new URL(foklauncherFile.getValue(FoklauncherFile.Property.REPO_BASE_URL)));
        getMvnCoordinates().setSnapshotRepoBaseURL(new URL(foklauncherFile.getValue(FoklauncherFile.Property.SNAPSHOT_BASE_URL)));
        getMvnCoordinates().setGroupId(foklauncherFile.getValue(FoklauncherFile.Property.GROUP_ID));
        getMvnCoordinates().setArtifactId(foklauncherFile.getValue(FoklauncherFile.Property.ARTIFACT_ID));
        getMvnCoordinates().setClassifier(foklauncherFile.getValue(FoklauncherFile.Property.CLASSIFIER, null));

        if (!foklauncherFile.getValue(FoklauncherFile.Property.ADDITIONAL_INFO_URL, "").equals("")) {
            this.setAdditionalInfoURL(new URL(foklauncherFile.getValue(FoklauncherFile.Property.ADDITIONAL_INFO_URL)));
        }
        if (!foklauncherFile.getValue(FoklauncherFile.Property.CHANGELOG_URL, "").equals("")) {
            this.setChangelogURL(new URL(foklauncherFile.getValue(FoklauncherFile.Property.CHANGELOG_URL)));
        }
    }

    /**
     * Removes this app from the imported apps list
     *
     * @throws IOException If the imported apps list cannot be read or written
     * @see #addImportedApp(File)
     */
    public void removeFromImportedAppList() throws IOException {
        ImportedAppListFile importedAppListFile = new ImportedAppListFile();
        AppList finalList = new AppList(importedAppListFile.getAppList().size());
        for (App app : importedAppListFile.getAppList()) {
            if (!app.getImportFile().equals(getImportFile())) {
                finalList.add(app);
            }
        }
        importedAppListFile.setAppList(finalList);
        importedAppListFile.saveFile();
    }

    public void createShortCut(File shortcutFile, String quickInfoText) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            ShellLink sl = ShellLink.createLink(new File(Common.getInstance().getPathAndNameOfCurrentJar()).toPath().toString());

            StringBuilder cmdArgs = new StringBuilder("-")
                    .append(EntryClass.getAutoLaunchWindowModeOption().getOpt())
                    .append(" -")
                    .append(EntryClass.getAutoLaunchRepoUrlOption().getOpt())
                    .append(" ")
                    .append(getMvnCoordinates().getRepoBaseURL().toString())
                    .append(" -")
                    .append(EntryClass.getAutoLaunchSnapshotRepoUrlOption().getOpt())
                    .append(" ")
                    .append(getMvnCoordinates().getSnapshotRepoBaseURL().toString())
                    .append(" -")
                    .append(EntryClass.getGroupIdOption().getOpt())
                    .append(" ")
                    .append(getMvnCoordinates().getGroupId())
                    .append(" -")
                    .append(EntryClass.getArtifactIdOption().getOpt())
                    .append(" ")
                    .append(getMvnCoordinates().getArtifactId());
            if (getMvnCoordinates().getClassifier() != null) {
                cmdArgs.append(" -")
                        .append(EntryClass.getClassifierOption().getOpt())
                        .append(" ")
                        .append(getMvnCoordinates().getClassifier());
            }

            sl.setCMDArgs(cmdArgs.toString());
            sl.setName(quickInfoText.replace("%s", this.getName()));

            if (Common.getInstance().getPackaging().equals("exe")) {
                sl.setIconLocation(new File(Common.getInstance().getPathAndNameOfCurrentJar()).toPath().toString());
            } else {
                URL inputUrl = MainWindow.class.getResource("icon.ico");
                File dest = new File(Common.getInstance().getAndCreateAppDataPath() + "icon.ico");
                FileUtils.copyURLToFile(inputUrl, dest);
                sl.setIconLocation(dest.getAbsolutePath());
            }

            sl.saveTo(shortcutFile.toPath().toString());

        } else {
            // Actually does not create a shortcut but a bash script
            throw new NotImplementedException();
        }
    }

    /**
     * Returns {@link MVNCoordinates#toString()} if maven coordinates have been specified or the name of the app otherwise
     *
     * @return {@link MVNCoordinates#toString()} if maven coordinates have been specified or the name of the app otherwise
     */
    @Override
    public String toString() {
        if (this.getName() != null) {
            return this.getName();
        } else if (getMvnCoordinates() != null) {
            return getMvnCoordinates().toString();
        } else {
            return super.toString();
        }
    }

    /**
     * Returns a JavaFX context menu that gives the user more detailed possibilities to interact with the app.
     * The context menu is only created once and then cached (much like a singleton).
     * Use {@link #clearCache()} if you wish to create a new instance of the context menu.
     *
     * @return A JavaFX context menu that gives the user more detailed possibilities to interact with the app.
     */
    public ContextMenu getContextMenu() {
        if (contextMenuCache == null) {
            contextMenuCache = this.generateContextMenu();
        }

        return contextMenuCache;
    }

    /**
     * Generates a new JavaFX context menu for this app (ignores the cached context menu instance). <b>FOR INTERNAL USE ONLY</b>
     * Use {@link #getContextMenu()} instead.
     *
     * @return A new JavaFX context menu for this app
     * @see #getContextMenu()
     */
    private ContextMenu generateContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        if (getChangelogURL() != null) {
            contextMenu.getItems().add(createChangelogMenuItem());
        }

        contextMenu.getItems().addAll(createLaunchSpecificVersionItem(), createDeleteItem(),
                createCreateShortcutOnDesktopMenuItem(), createCreateShortcutMenuItem(), createExportMenuItem());

        if (isImported()) {
            contextMenu.getItems().add(createRemoveImportedAppMenuItem());
        }
        return contextMenu;
    }

    private Menu createLaunchSpecificVersionItem() {
        LaunchSpecificVersionMenu launchSpecificVersionItem = new LaunchSpecificVersionMenu();
        launchSpecificVersionItem.setText(MainWindow.getBundle().getString("launchSpecificVersion").replace("%s", this.toString()));

        MenuItem dummyVersion = new MenuItem();
        dummyVersion.setText(MainWindow.getBundle().getString("waitForVersionList"));
        // show the dummy version while the actual version list is loading
        launchSpecificVersionItem.getItems().add(dummyVersion);
        launchSpecificVersionItem.setOnHiding(event2 -> launchSpecificVersionItem.setCancelled(true));
        launchSpecificVersionItem.setOnShown(event -> {
            launchSpecificVersionItem.setCancelled(false);
            Thread buildContextMenuThread = new Thread(() -> {
                FOKLogger.info(App.class.getName(), "Getting available online versions...");

                // Get available versions
                VersionList verList;
                if (!EntryClass.getControllerInstance().workOffline()) {
                    // Online mode enabled
                    try {
                        verList = getAllOnlineVersions();
                        verList.setEnsureNoDuplicates(true);
                        verList.addAll(getCurrentlyInstalledVersions());
                        if (EntryClass.getControllerInstance().snapshotsEnabled()) {
                            verList.add(getLatestOnlineSnapshotVersion());
                        }
                    } catch (Exception e) {
                        // Something happened, pretend
                        // offline mode
                        verList = getCurrentlyInstalledVersions();
                    }

                } else {
                    // Offline mode enabled
                    verList = getCurrentlyInstalledVersions();
                }

                // Sort the list
                Collections.sort(verList);

                // Clear previous list
                Platform.runLater(() -> launchSpecificVersionItem.getItems().clear());

                for (Version ver : verList) {
                    VersionMenuItem menuItem = new VersionMenuItem();
                    menuItem.setVersion(ver);
                    menuItem.setText(ver.toString(false));
                    menuItem.setOnAction(event2 -> EntryClass.getControllerInstance().launchAppFromGUI(this, menuItem.getVersion()));
                    Platform.runLater(() -> launchSpecificVersionItem.getItems().add(menuItem));
                }
                Platform.runLater(() -> {
                    if (!launchSpecificVersionItem.isCancelled()) {
                        launchSpecificVersionItem.hide();
                        launchSpecificVersionItem.show();
                    }
                });
            });

            if (!this.isSpecificVersionListLoaded()) {
                buildContextMenuThread.setName("buildContextMenuThread");
                buildContextMenuThread.start();
                this.setSpecificVersionListLoaded(true);
            }
        });
        return launchSpecificVersionItem;
    }

    private Menu createDeleteItem() {
        Menu deleteItem = new Menu();
        deleteItem.setText(MainWindow.getBundle().getString("deleteVersion").replace("%s", this.toString()));
        MenuItem dummyVersion2 = new MenuItem();
        dummyVersion2.setText(MainWindow.getBundle().getString("waitForVersionList"));
        deleteItem.getItems().add(dummyVersion2);

        deleteItem.setOnShown(event -> {
            if (!isDeletableVersionListLoaded()) {
                // Get deletable versions
                setDeletableVersionListLoaded(true);
                FOKLogger.info(App.class.getName(), "Getting deletable versions...");
                deleteItem.getItems().clear();

                VersionList verList;
                verList = getCurrentlyInstalledVersions();
                Collections.sort(verList);

                for (Version ver : verList) {
                    VersionMenuItem menuItem = new VersionMenuItem();
                    menuItem.setVersion(ver);
                    menuItem.setText(ver.toString(false));
                    menuItem.setOnAction(event2 -> {
                        // Delete the file
                        try {
                            delete(menuItem.getVersion());
                        } catch (IOException e) {
                            FOKLogger.log(getClass().getName(), Level.SEVERE, "Unable to delete the app " + getName(), e);
                        } finally {
                            EntryClass.getControllerInstance().updateLaunchButton();
                        }
                        // Update the list the next time the
                        // user opens it as it has changed
                        setDeletableVersionListLoaded(false);

                    });
                    Platform.runLater(() -> deleteItem.getItems().add(menuItem));
                }
                Platform.runLater(() -> {
                    deleteItem.hide();
                    deleteItem.show();
                });
            }
        });
        return deleteItem;
    }

    private MenuItem createChangelogMenuItem() {
        MenuItem changelogMenuItem = new MenuItem(MainWindow.getBundle().getString("openChangelog"));
        changelogMenuItem.setOnAction(event -> {
            FOKLogger.info(App.class.getName(), "Opening the changelog...");
            try {
                Desktop.getDesktop().browse(this.getChangelogURL().toURI());
            } catch (IOException | URISyntaxException e) {
                FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                EntryClass.getControllerInstance().showErrorMessage(e.toString());
            }
        });
        return changelogMenuItem;
    }

    public MenuItem createCreateShortcutOnDesktopMenuItem() {
        MenuItem createShortcutOnDesktopMenuItem = new MenuItem();
        createShortcutOnDesktopMenuItem.setText(MainWindow.getBundle().getString("createShortcutOnDesktop"));
        createShortcutOnDesktopMenuItem.setOnAction(event3 -> {
            FOKLogger.info(App.class.getName(), "Creating shortcut...");
            File file = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()
                    + File.separator + getName() + ".lnk");
            try {
                FOKLogger.info(App.class.getName(), "Creating shortcut for app " + getName()
                        + " at the following location: " + file.getAbsolutePath());
                createShortCut(file, MainWindow.getBundle().getString("shortcutQuickInfo"));
            } catch (Exception e) {
                FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                EntryClass.getControllerInstance().showErrorMessage(e.toString());
            }
        });
        return createShortcutOnDesktopMenuItem;
    }

    public MenuItem createCreateShortcutMenuItem() {
        MenuItem createShortcutMenuItem = new MenuItem();
        createShortcutMenuItem.setText(MainWindow.getBundle().getString("createShortcut"));
        createShortcutMenuItem.setOnAction(event3 -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters()
                    .addAll(new FileChooser.ExtensionFilter(MainWindow.getBundle().getString("shortcut"), "*.lnk"));
            fileChooser.setTitle(MainWindow.getBundle().getString("saveShortcut"));
            File file = fileChooser.showSaveDialog(EntryClass.getStage());
            if (file != null) {
                FOKLogger.info(App.class.getName(), "Creating shortcut...");

                try {
                    FOKLogger.info(App.class.getName(), "Creating shortcut for app " + this.getName()
                            + " at the following location: " + file.getAbsolutePath());
                    this.createShortCut(file, MainWindow.getBundle().getString("shortcutQuickInfo"));
                } catch (Exception e) {
                    FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                    EntryClass.getControllerInstance().showErrorMessage(e.toString());
                }
            }
        });
        return createShortcutMenuItem;
    }

    public MenuItem createExportMenuItem() {
        MenuItem exportInfoItem = new MenuItem();
        exportInfoItem.setText(MainWindow.getBundle().getString("exportInfo"));
        exportInfoItem.setOnAction(event2 -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters()
                    .addAll(new FileChooser.ExtensionFilter("FOK-Launcher-File", "*.foklauncher"));
            fileChooser.setTitle("Save app info");
            // TODO Translation
            File file = fileChooser.showSaveDialog(EntryClass.getStage());
            if (file != null) {
                FOKLogger.info(App.class.getName(), "Exporting info...");
                try {
                    FOKLogger.info(App.class.getName(), "Exporting app info of app " + getName() + " to file: "
                            + file.getAbsolutePath());
                    exportInfo(file);
                } catch (IOException e) {
                    FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                    EntryClass.getControllerInstance().showErrorMessage(e.toString());
                }
            }
        });
        return exportInfoItem;
    }

    public MenuItem createRemoveImportedAppMenuItem() {
        MenuItem removeImportedApp = new MenuItem();
        if (isImported()) {
            removeImportedApp.setText(MainWindow.getBundle().getString("deleteImportedApp"));
            removeImportedApp.setOnAction(event3 -> {
                try {
                    removeFromImportedAppList();
                    EntryClass.getControllerInstance().loadAppList();
                } catch (IOException e) {
                    FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                    EntryClass.getControllerInstance().showErrorMessage(e.toString());
                }
            });
        }
        return removeImportedApp;
    }

    /**
     * Returns the string with the folder path to the folder where this app's jars and metadata shall be saved. The returned path is relative to the launcher's app data path.
     *
     * @return The string with the folder path to the folder where this app's jars and metadata shall be saved.
     */
    private String getSubfolderToSaveApps() {
        String res = AppConfig.getRemoteConfig().getValue("subfolderToSaveApps").replace("{FileSeparator}", File.separator)
                .replace("{groupId}", getMvnCoordinates().getGroupId()).replace("{artifactId}", getMvnCoordinates().getArtifactId());
        if (getMvnCoordinates().getClassifier() == null) {
            // No classifier
            return res.replace("{classifier}", "");
        } else {
            return res.replace("{classifier}", getMvnCoordinates().getClassifier());
        }
    }
}

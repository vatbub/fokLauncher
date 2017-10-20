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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.Nullable;
import view.EntryClass;
import view.MainWindow;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

import static view.MainWindow.*;

@SuppressWarnings("SameParameterValue")
public class App {
    /**
     * A list of event handlers that handle the event that this app was launched
     * and exited then
     */
    private final List<Runnable> eventHandlersWhenLaunchedAppExits = new ArrayList<>();
    /**
     * The latest snapshot version of the app that is available online
     */
    Version latestOnlineSnapshotVersion;
    MVNMetadataFile releaseRepoMetadataFile;
    MVNMetadataFile snapshotRepoMetadataFile;
    LocalMetadataFile localMetadataFile;
    /**
     * Specifies the file from which this app was imported from (if it was
     * imported)
     */
    private File importFile;
    /**
     * The name of the app
     */
    private String name;
    /**
     * {@code true} if the user requested to cancel the current action.
     */
    private boolean cancelDownloadAndLaunch;

    private MVNCoordinates mvnCoordinates;

    /**
     * A webpage where the user finds additional info for this app.
     */
    private URL additionalInfoURL;
    /**
     * A webpage where the user finds a changelog for this app.
     */
    private URL changelogURL;
    private boolean specificVersionListLoaded = false;
    private boolean deletableVersionListLoaded = false;
    private ContextMenu contextMenu;

    /**
     * Creates a new App with the specified name.
     *
     * @param name The name of the app.
     */
    public App(String name) {
        this(name, new MVNCoordinates());
    }

    /**
     * Creates a new App with the specified coordinates.
     *
     * @param name           The name of the app
     * @param mvnCoordinates The maven coordinates of the app
     */
    public App(String name, MVNCoordinates mvnCoordinates) {
        this(name, mvnCoordinates, null);
    }

    /**
     * Creates a new App with the specified coordinates.
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
     * Creates a new App with the specified coordinates.
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

    public static void addImportedApp(File infoFile) throws IOException {
        ImportedAppListFile importedAppListFile = new ImportedAppListFile();
        importedAppListFile.getAppList().addAndCheckForDuplicateImports(new App(infoFile));
        importedAppListFile.saveFile();
    }

    public URL getChangelogURL() {
        return changelogURL;
    }

    public void setChangelogURL(URL changelogURL) {
        this.changelogURL = changelogURL;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
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

    private Path getAbsolutePathToSubfolderToSaveApps() {
        return Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve(getSubfolderToSaveApps());
    }

    private File getLocationOfLocalMetadataFile() {
        return getAbsolutePathToSubfolderToSaveApps().resolve(AppConfig.getRemoteConfig().getValue("appMetadataFileName")).toFile();
    }

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
     * @return the mavenRepoBaseURL
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public URL getMavenRepoBaseURL() {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        return getMvnCoordinates().getRepoBaseURL();
    }

    /**
     * @param mavenRepoBaseURL the mavenRepoBaseURL to set
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public void setMavenRepoBaseURL(URL mavenRepoBaseURL) {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        getMvnCoordinates().setRepoBaseURL(mavenRepoBaseURL);
    }

    /**
     * @return the mavenSnapshotRepoBaseURL
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public URL getMavenSnapshotRepoBaseURL() {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        return getMvnCoordinates().getSnapshotRepoBaseURL();
    }

    /**
     * @param mavenSnapshotRepoBaseURL the mavenSnapshotRepoBaseURL to set
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public void setMavenSnapshotRepoBaseURL(URL mavenSnapshotRepoBaseURL) {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        getMvnCoordinates().setSnapshotRepoBaseURL(mavenSnapshotRepoBaseURL);
    }

    /**
     * @return the mavenGroupID
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public String getMavenGroupID() {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        return getMvnCoordinates().getGroupId();
    }

    /**
     * @param mavenGroupID the mavenGroupID to set
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public void setMavenGroupID(String mavenGroupID) {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        getMvnCoordinates().setGroupId(mavenGroupID);
    }

    /**
     * @return the mavenArtifactID
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public String getMavenArtifactID() {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        return getMvnCoordinates().getArtifactId();
    }

    /**
     * @param mavenArtifactID the mavenArtifactID to set
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public void setMavenArtifactID(String mavenArtifactID) {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        getMvnCoordinates().setArtifactId(mavenArtifactID);
    }

    /**
     * @return the mavenClassifier
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public String getMavenClassifier() {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        return getMvnCoordinates().getClassifier();
    }

    /**
     * @param mavenClassifier the mavenClassifier to set
     * @deprecated Use {@link #getMvnCoordinates()} instead
     */
    @Deprecated
    public void setMavenClassifier(String mavenClassifier) {
        if (getMvnCoordinates() == null) {
            setMvnCoordinates(new MVNCoordinates());
        }
        getMvnCoordinates().setClassifier(mavenClassifier);
    }

    public MVNCoordinates getMvnCoordinates() {
        return mvnCoordinates;
    }

    public void setMvnCoordinates(MVNCoordinates mvnCoordinates) {
        this.mvnCoordinates = mvnCoordinates;
    }

    /**
     * @return the imported
     */
    public boolean isImported() {
        return getImportFile() != null;
    }

    /**
     * @return the importFile
     */
    public File getImportFile() {
        return importFile;
    }

    /**
     * @return the specificVersionListLoaded
     */
    public boolean isSpecificVersionListLoaded() {
        return specificVersionListLoaded;
    }

    /**
     * @param specificVersionListLoaded the specificVersionListLoaded to set
     */
    public void setSpecificVersionListLoaded(boolean specificVersionListLoaded) {
        this.specificVersionListLoaded = specificVersionListLoaded;
    }

    /**
     * @return the deletableVersionListLoaded
     */
    public boolean isDeletableVersionListLoaded() {
        return deletableVersionListLoaded;
    }

    /**
     * @param deletableVersionListLoaded the deletableVersionListLoaded to set
     */
    public void setDeletableVersionListLoaded(boolean deletableVersionListLoaded) {
        this.deletableVersionListLoaded = deletableVersionListLoaded;
    }

    /**
     * @return the additionalInfoURL
     */
    public URL getAdditionalInfoURL() {
        return additionalInfoURL;
    }

    /**
     * @param additionalInfoURL the additionalInfoURL to set
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
        Path destFolder = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve(getSubfolderToSaveApps());
        String destFilename;

        if (!disableDownload) {
            // Continue by default, only cancel, when user cancelled
            boolean downloadPerformed = true;

            // download if necessary
            if (!this.isPresentOnHarddrive(versionToLaunch)) {
                // app not downloaded at all or needs to be updated
                if (!this.isPresentOnHarddrive()) {
                    // App was never downloaded
                    FOKLogger.info(App.class.getName(), "Downloading package because it was never downloaded before...");
                } else {
                    // App needs an update
                    FOKLogger.info(App.class.getName(), "Downloading package because an update is available...");
                }

                downloadPerformed = this.download(versionToLaunch, gui);
            }

            if (!downloadPerformed) {
                // Download was cancelled by the user so return
                return;
            }
        } else if (!this.isPresentOnHarddrive(versionToLaunch)) {
            // Download is disabled and app needs to be downloaded
            throw new IllegalStateException(
                    "The artifact needs to be downloaded prior to launch but download is disabled.");
        }

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return;
        }

        if (getMvnCoordinates().getClassifier() == null) {
            // No classifier
            destFilename = getMvnCoordinates().getArtifactId() + "-" + versionToLaunch.toString() + ".jar";
        } else {
            destFilename = getMvnCoordinates().getArtifactId() + "-" + versionToLaunch.toString() + "-"
                    + getMvnCoordinates().getClassifier() + ".jar";
        }

        if (gui != null) {
            gui.launchStarted();
        }

        String jarFileName = destFolder.resolve(destFilename).toAbsolutePath().toString();

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
        Process process;
        // MainWindow.getMetricsRegistry().counter("foklauncher.applaunches." + getMavenCoordinateString(versionToLaunch)).inc();

        FOKLogger.info(App.class.getName(), "Launching application " + destFilename);

        FOKLogger.info(getClass().getName(), "------------------------------------------------------------------");
        FOKLogger.info(getClass().getName(), "The following output is coming from " + destFilename);
        FOKLogger.info(getClass().getName(), "------------------------------------------------------------------");

        if (gui != null) {
            gui.hide();

            process = pb.start();
        } else {
            process = pb.start();
        }

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

        Path destFolder = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve(getSubfolderToSaveApps());
        String destFilename;
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
        if (getMvnCoordinates().getClassifier() == null) {
            artifactURL = new URL(repoBaseURL.toString() + "/" + getMvnCoordinates().getGroupId().replace('.', '/') + "/"
                    + getMvnCoordinates().getArtifactId() + "/" + versionToDownload.getVersion() + "/" + getMvnCoordinates().getArtifactId()
                    + "-" + versionToDownload.toString() + ".jar");
        } else {
            artifactURL = new URL(repoBaseURL.toString() + "/" + getMvnCoordinates().getGroupId().replace('.', '/') + "/"
                    + getMvnCoordinates().getArtifactId() + "/" + versionToDownload.getVersion() + "/" + getMvnCoordinates().getArtifactId()
                    + "-" + versionToDownload.toString() + "-" + getMvnCoordinates().getClassifier() + ".jar");
        }

        // Construct file name of output file
        if (getMvnCoordinates().getClassifier() == null) {
            // No classifier
            destFilename = getMvnCoordinates().getArtifactId() + "-" + versionToDownload.toString() + ".jar";
        } else {
            destFilename = getMvnCoordinates().getArtifactId() + "-" + versionToDownload.toString() + "-"
                    + getMvnCoordinates().getClassifier() + ".jar";
        }

        // Perform Cancel if requested
        if (cancelDownloadAndLaunch) {
            if (gui != null) {
                gui.operationCanceled();
            }
            return false;
        }

        // Create empty file
        File outputFile = new File(destFolder + File.separator + destFilename);

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

        java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
        //noinspection ResultOfMethodCallIgnored
        outputFile.getParentFile().mkdirs();
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
        java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
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

            bout.write(data, 0, x);

            // Perform Cancel if requested
            if (cancelDownloadAndLaunch) {
                bout.close();
                in.close();
                Files.delete(outputFile.toPath());
                if (gui != null) {
                    gui.operationCanceled();
                }
                return false;
            }
        }
        bout.close();
        in.close();

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
        return eventHandlersWhenLaunchedAppExits.size() > 0;
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

            System.out.println(cmdArgs.toString());
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

    public ContextMenu getContextMenu() {
        if (contextMenu == null) {
            contextMenu = this.generateContextMenu();
        }

        return contextMenu;
    }

    private ContextMenu generateContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        Menu launchSpecificVersionItem = new Menu();
        launchSpecificVersionItem.setText(MainWindow.getBundle().getString("launchSpecificVersion").replace("%s", this.toString()));

        MenuItem dummyVersion = new MenuItem();
        dummyVersion.setText(MainWindow.getBundle().getString("waitForVersionList"));
        launchSpecificVersionItem.getItems().add(dummyVersion);
        launchSpecificVersionItem.setOnHiding(event2 -> MainWindow.launchSpecificVersionMenuCanceled = true);
        App app = this;
        launchSpecificVersionItem.setOnShown(event -> {
            MainWindow.launchSpecificVersionMenuCanceled = false;
            Thread buildContextMenuThread = new Thread(() -> {
                FOKLogger.info(App.class.getName(), "Getting available online versions...");

                // Get available versions
                VersionList verList;
                if (!EntryClass.getControllerInstance().workOffline()) {
                    // Online mode enabled
                    try {
                        verList = app.getAllOnlineVersions();
                        VersionList additionalVersions = app.getCurrentlyInstalledVersions();
                        if (EntryClass.getControllerInstance().snapshotsEnabled()) {
                            additionalVersions.add(app.getLatestOnlineSnapshotVersion());
                        }

                        for (Version additionalVersion : additionalVersions) {
                            if (!verList.contains(additionalVersion)) {
                                verList.add(additionalVersion);
                            }
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

                // Sort the list
                Collections.sort(verList);

                // Clear previous list
                Platform.runLater(() -> launchSpecificVersionItem.getItems().clear());

                for (Version ver : verList) {
                    VersionMenuItem menuItem = new VersionMenuItem();
                    menuItem.setVersion(ver);
                    menuItem.setText(ver.toString(false));
                    menuItem.setOnAction(event2 -> {
                        // Launch the download
                        MainWindow.downloadAndLaunchThread = new Thread(() -> {
                            try {
                                // Attach the on app
                                // exit handler if
                                // required
                                if (EntryClass.getControllerInstance().launchLauncherAfterAppExitCheckbox.isSelected()) {
                                    app.addEventHandlerWhenLaunchedAppExits(showLauncherAgain);
                                } else {
                                    app.removeEventHandlerWhenLaunchedAppExits(
                                            showLauncherAgain);
                                }
                                app.downloadIfNecessaryAndLaunch(
                                        EntryClass.getControllerInstance(), menuItem.getVersion(),
                                        EntryClass.getControllerInstance().workOffline());
                            } catch (IOException e) {
                                EntryClass.getControllerInstance().showErrorMessage(
                                        "An error occurred: \n" + ExceptionUtils.getStackTrace(e));
                                FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                            }
                        });

                        downloadAndLaunchThread.setName("downloadAndLaunchThread");
                        downloadAndLaunchThread.start();
                    });
                    Platform.runLater(() -> launchSpecificVersionItem.getItems().add(menuItem));
                }
                Platform.runLater(() -> {
                    if (!launchSpecificVersionMenuCanceled) {
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

        Menu deleteItem = new Menu();
        deleteItem.setText(MainWindow.getBundle().getString("deleteVersion").replace("%s", this.toString()));
        MenuItem dummyVersion2 = new MenuItem();
        dummyVersion2.setText(MainWindow.getBundle().getString("waitForVersionList"));
        deleteItem.getItems().add(dummyVersion2);

        deleteItem.setOnShown(event -> {
            if (!app.isDeletableVersionListLoaded()) {
                // Get deletable versions
                app.setDeletableVersionListLoaded(true);
                FOKLogger.info(App.class.getName(), "Getting deletable versions...");
                deleteItem.getItems().clear();

                VersionList verList;
                verList = app.getCurrentlyInstalledVersions();
                Collections.sort(verList);

                for (Version ver : verList) {
                    VersionMenuItem menuItem = new VersionMenuItem();
                    menuItem.setVersion(ver);
                    menuItem.setText(ver.toString(false));
                    menuItem.setOnAction(event2 -> {
                        // Delete the file
                        try {
                            app.delete(menuItem.getVersion());
                        } catch (IOException e) {
                            FOKLogger.log(getClass().getName(), Level.SEVERE, "Unable to delete the app " + app.getName(), e);
                        } finally {
                            EntryClass.getControllerInstance().updateLaunchButton();
                        }
                        // Update the list the next time the
                        // user opens it as it has changed
                        app.setDeletableVersionListLoaded(false);

                    });
                    Platform.runLater(() -> deleteItem.getItems().add(menuItem));
                }
                Platform.runLater(() -> {
                    deleteItem.hide();
                    deleteItem.show();
                });
            }
        });

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

        if (this.getChangelogURL() != null) {
            contextMenu.getItems().add(changelogMenuItem);
        }

        MenuItem createShortcutOnDesktopMenuItem = new MenuItem();
        createShortcutOnDesktopMenuItem.setText(MainWindow.getBundle().getString("createShortcutOnDesktop"));
        createShortcutOnDesktopMenuItem.setOnAction(event3 -> {
            FOKLogger.info(App.class.getName(), "Creating shortcut...");
            File file = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()
                    + File.separator + app.getName() + ".lnk");
            try {
                FOKLogger.info(App.class.getName(), "Creating shortcut for app " + app.getName()
                        + " at the following location: " + file.getAbsolutePath());
                app.createShortCut(file, MainWindow.getBundle().getString("shortcutQuickInfo"));
            } catch (Exception e) {
                FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                EntryClass.getControllerInstance().showErrorMessage(e.toString());
            }
        });

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

        MenuItem exportInfoItem = new MenuItem();
        exportInfoItem.setText(MainWindow.getBundle().getString("exportInfo"));
        exportInfoItem.setOnAction(event2 -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters()
                    .addAll(new FileChooser.ExtensionFilter("FOK-Launcher-File", "*.foklauncher"));
            fileChooser.setTitle("Save Image");
            // TODO Translation
            File file = fileChooser.showSaveDialog(EntryClass.getStage());
            if (file != null) {
                FOKLogger.info(App.class.getName(), "Exporting info...");
                try {
                    FOKLogger.info(App.class.getName(), "Exporting app info of app " + app.getName() + " to file: "
                            + file.getAbsolutePath());
                    app.exportInfo(file);
                } catch (IOException e) {
                    FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                    EntryClass.getControllerInstance().showErrorMessage(e.toString());
                }
            }
        });

        contextMenu.getItems().addAll(launchSpecificVersionItem, deleteItem,
                createShortcutOnDesktopMenuItem, createShortcutMenuItem, exportInfoItem);

        MenuItem removeImportedApp = new MenuItem();
        contextMenu.setOnShowing(event5 -> {
            if (app.isImported()) {
                removeImportedApp.setText(MainWindow.getBundle().getString("deleteImportedApp"));
                removeImportedApp.setOnAction(event3 -> {
                    try {
                        app.removeFromImportedAppList();
                        EntryClass.getControllerInstance().loadAppList();
                    } catch (IOException e) {
                        FOKLogger.log(App.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                        EntryClass.getControllerInstance().showErrorMessage(e.toString());
                    }
                });

                contextMenu.getItems().add(removeImportedApp);
            }
        });

        contextMenu.setOnHidden(event5 -> {
            // Remove the removeImportedApp-Item again if it exists
            if (contextMenu.getItems().contains(removeImportedApp)) {
                contextMenu.getItems().remove(removeImportedApp);
            }
        });

        return contextMenu;
    }

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

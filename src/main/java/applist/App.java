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


import common.*;
import extended.VersionMenuItem;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import logging.FOKLogger;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import view.MainWindow;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;

import static view.MainWindow.*;

public class App {

	private static FOKLogger log = new FOKLogger(App.class.getName());

	/**
	 * Creates a new App with the specified name.
	 * 
	 * @param name
	 *            The name of the app.
	 */
	public App(String name) {
		this(name, null, null, "", "");
	}

	/**
	 * Creates a new App with the specified coordinates.
	 * 
	 * @param name
	 *            The name of the app
	 * @param mavenRepoBaseURL
	 *            Base URL of the maven repo where the artifact can be
	 *            downloaded from.
	 * @param mavenSnapshotRepoBaseURL
	 *            The URL of the maven repo where snapshots of the artifact can
	 *            be downloaded from.
	 * @param mavenGroupId
	 *            The artifacts group id.
	 * @param mavenArtifactId
	 *            The artifacts artifact id
	 */
	public App(String name, URL mavenRepoBaseURL, URL mavenSnapshotRepoBaseURL, String mavenGroupId,
			String mavenArtifactId) {
		this(name, mavenRepoBaseURL, mavenSnapshotRepoBaseURL, mavenGroupId, mavenArtifactId, "");
	}

	/**
	 * Creates a new App with the specified coordinates.
	 * 
	 * @param name
	 *            The name of the app
	 * @param mavenRepoBaseURL
	 *            Base URL of the maven repo where the artifact can be
	 *            downloaded from.
	 * @param mavenSnapshotRepoBaseURL
	 *            The URL of the maven repo where snapshots of the artifact can
	 *            be downloaded from.
	 * @param mavenGroupId
	 *            The artifacts group id.
	 * @param mavenArtifactId
	 *            The artifacts artifact id
	 * @param mavenClassifier
	 *            The artifacts classifier or {@code ""} if the default artifact
	 *            shall be used.
	 * 
	 */
	public App(String name, URL mavenRepoBaseURL, URL mavenSnapshotRepoBaseURL, String mavenGroupId,
			String mavenArtifactId, String mavenClassifier) {
		this(name, mavenRepoBaseURL, mavenSnapshotRepoBaseURL, mavenGroupId, mavenArtifactId, mavenClassifier, null);
	}

	/**
	 * Creates a new App with the specified coordinates.
	 * 
	 * @param name
	 *            The name of the app
	 * @param mavenRepoBaseURL
	 *            Base URL of the maven repo where the artifact can be
	 *            downloaded from.
	 * @param mavenSnapshotRepoBaseURL
	 *            The URL of the maven repo where snapshots of the artifact can
	 *            be downloaded from.
	 * @param mavenGroupId
	 *            The artifacts group id.
	 * @param mavenArtifactId
	 *            The artifacts artifact id
	 * @param mavenClassifier
	 *            The artifacts classifier or {@code ""} if the default artifact
	 *            shall be used.
	 * @param additionalInfoURL
	 *            The url to a webpage where the user finds additional info
	 *            about this app.
	 */
	public App(String name, URL mavenRepoBaseURL, URL mavenSnapshotRepoBaseURL, String mavenGroupId,
			String mavenArtifactId, String mavenClassifier, URL additionalInfoURL) {
		this.setName(name);
		this.setMavenRepoBaseURL(mavenRepoBaseURL);
		this.setMavenSnapshotRepoBaseURL(mavenSnapshotRepoBaseURL);
		this.setMavenGroupID(mavenGroupId);
		this.setMavenArtifactID(mavenArtifactId);
		this.setMavenClassifier(mavenClassifier);
		this.setAdditionalInfoURL(additionalInfoURL);
	}

	/**
	 * Creates a new app and imports its info from the specified file. If the
	 * specified file cannot be imported for some reason, this app will
	 * automatically be deleted from the imported apps list.
	 * 
	 * @param fileToImportFrom
	 *            The file to import the info from. Must be a *.foklauncher file
	 * @throws IOException
	 *             If the specified file is not a file (but a directory) or if
	 *             the launcher has no permission to read the file and this app
	 *             cannot be deleted from the imported app list for some erason.
	 * @see App#removeFromImportedAppList()
	 */
	public App(File fileToImportFrom) throws IOException {
		try {
			this.importInfo(fileToImportFrom);
		} catch (IOException e) {
			log.getLogger().log(Level.SEVERE,
					"The app that should be imported from " + fileToImportFrom.getAbsolutePath()
							+ " is automatically deleted from the imported apps list because something went wrong during the import. We probably didn't find the file to import.",
					e);
			this.removeFromImportedAppList();
			// Propagate the Exception
			throw e;
		}
	}

	/**
	 * Specifies if the info of this app was imported from a file or gatheerd
	 * from the remote server.
	 */
	private boolean imported;

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
	 * The latest version of the app that is available online
	 */
	Version latestOnlineVersion;

	/**
	 * A {@link List} of all available online Versions
	 */
	VersionList onlineVersionList;

	/**
	 * The latest snapshot version of the app that is available online
	 */
	Version latestOnlineSnapshotVersion;

	/**
	 * {@code true} if the user requested to cancel the current action.
	 */
	private boolean cancelDownloadAndLaunch;

	/**
	 * Base URL of the maven repo where the artifact can be downloaded from.
	 */
	private URL mavenRepoBaseURL;

	/**
	 * The URL of the maven repo where snapshots of the artifact can be
	 * downloaded from.
	 */
	private URL mavenSnapshotRepoBaseURL;

	/**
	 * The artifacts group id.
	 */
	private String mavenGroupID;

	/**
	 * The artifacts artifact id
	 */
	private String mavenArtifactID;

	/**
	 * The artifacts classifier or {@code ""} if the default artifact shall be
	 * used.
	 */
	private String mavenClassifier;

	/**
	 * A webpage where the user finds additional info for this app.
	 */
	private URL additionalInfoURL;

	private boolean specificVersionListLoaded = false;

	private boolean deletableVersionListLoaded = false;

	private ContextMenu contextMenu;

	/**
	 * A list of event handlers that handle the event that this app was launched
	 * and exited then
	 */
	private List<Runnable> eventHandlersWhenLaunchedAppExits = new ArrayList<>();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a list that contains all versions of the artifact available
	 * online
	 * 
	 * @return A list that contains all versions of the artifact available
	 *         online
	 * @throws IOException
	 *             If the maven metadata file can't be read or downloaded
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 */
	public VersionList getAllOnlineVersions() throws JDOMException, IOException {
		if (onlineVersionList != null) {
			return onlineVersionList.clone();
		} else {
			Document mavenMetadata = getMavenMetadata(false);

			VersionList res = new VersionList();
			List<Element> versions = mavenMetadata.getRootElement().getChild("versioning").getChild("versions")
					.getChildren("version");

			for (Element versionElement : versions) {
				Version version = new Version(versionElement.getValue());

				if (!version.isSnapshot()) {
					// Version is not a snapshot so add it to the list
					res.add(version);
				}
			}

			onlineVersionList = res.clone();
			return res;
		}
	}

	/**
	 * @return the latestOnlineVersion
	 * @throws IOException
	 *             If the maven metadata file can't be read or downloaded
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 */
	public Version getLatestOnlineVersion() throws JDOMException, IOException {
		if (latestOnlineVersion != null) {
			return latestOnlineVersion.clone();
		} else {
			Document mavenMetadata = getMavenMetadata(false);

			Version res = new Version(
					mavenMetadata.getRootElement().getChild("versioning").getChild("latest").getValue());

			if (res.isSnapshot()) {
				throw new IllegalStateException(
						"Latest version in this repository is a snapshot and not a release. This might happen if you host snapshots and releases in the same repository (which is not recommended). If you still need this case to be covered, please submit an issue at https://github.com/vatbub/fokLauncher/issues");
			}

			latestOnlineVersion = res.clone();
			return res;
		}
	}

	/**
	 * @return the latestOnlineSnapshotVersion
	 * @throws IOException
	 *             If the maven metadata file can't be read or downloaded
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 */
	public Version getLatestOnlineSnapshotVersion() throws JDOMException, IOException {
		if (latestOnlineSnapshotVersion != null) {
			return latestOnlineSnapshotVersion.clone();
		} else {
			Document mavenMetadata = getMavenMetadata(true);

			Version res = new Version(
					mavenMetadata.getRootElement().getChild("versioning").getChild("latest").getValue());

			Document snapshotMetadata = new SAXBuilder()
					.build(new URL(this.getMavenSnapshotRepoBaseURL().toString() + "/" + mavenGroupID.replace('.', '/')
							+ "/" + mavenArtifactID + "/" + res.getVersion() + "/maven-metadata.xml"));

			if (!res.isSnapshot()) {
				throw new IllegalStateException(
						"Latest version in this repository is a release and not a snapshot. This might happen if you host snapshots and releases in the same repository (which is not recommended). If you still need this case to be covered, please submit an issue at https://github.com/vatbub/fokLauncher/issues");
			}

			// get the buildnumber
			res.setBuildNumber(snapshotMetadata.getRootElement().getChild("versioning").getChild("snapshot")
					.getChild("buildNumber").getValue());

			// get the build timestamp
			res.setTimestamp(snapshotMetadata.getRootElement().getChild("versioning").getChild("snapshot")
					.getChild("timestamp").getValue());

			latestOnlineSnapshotVersion = res.clone();
			return res;
		}
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
	 * @param snapshotsEnabled
	 *            If {@code false}, snapshots will be removed from this list.
	 * @return the currentlyInstalledVersion
	 * @see #isPresentOnHarddrive()
	 */
	public Version getCurrentlyInstalledVersion(boolean snapshotsEnabled) {
		try {
			VersionList vers = getCurrentlyInstalledVersions();
			if (!snapshotsEnabled) {
				vers.removeSnapshots();
			}
			return Collections.max(vers);
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
		VersionList res = new VersionList();

		// Load the metadata.xml file
		String destFolder = Common.getAndCreateAppDataPath() + getSubfolderToSaveApps();
		String fileName = destFolder + File.separator + AppConfig.appMetadataFileName;
		Document versionDoc;

		try {
			versionDoc = new SAXBuilder().build(fileName);
		} catch (JDOMException | IOException e) {
			System.err.println("Cannot retreive currently installed version of app " + this.getName()
					+ ", probably because it is not installed.");
			// only info level as exceptions can happen if the app was never
			// installed on this machine before
			log.getLogger().log(Level.INFO, "An error occured!", e);
			return null;
		}

		for (Element versionEl : versionDoc.getRootElement().getChild("versions").getChildren()) {
			Version tempVer = new Version(versionEl.getChild("version").getValue(),
					versionEl.getChild("buildNumber").getValue(), versionEl.getChild("timestamp").getValue());
			res.add(tempVer);
		}

		return res;
	}

	/**
	 * @return the mavenRepoBaseURL
	 */
	public URL getMavenRepoBaseURL() {
		return mavenRepoBaseURL;
	}

	/**
	 * @param mavenRepoBaseURL
	 *            the mavenRepoBaseURL to set
	 */
	public void setMavenRepoBaseURL(URL mavenRepoBaseURL) {
		this.mavenRepoBaseURL = mavenRepoBaseURL;
	}

	/**
	 * @return the mavenSnapshotRepoBaseURL
	 */
	public URL getMavenSnapshotRepoBaseURL() {
		return mavenSnapshotRepoBaseURL;
	}

	/**
	 * @param mavenSnapshotRepoBaseURL
	 *            the mavenSnapshotRepoBaseURL to set
	 */
	public void setMavenSnapshotRepoBaseURL(URL mavenSnapshotRepoBaseURL) {
		this.mavenSnapshotRepoBaseURL = mavenSnapshotRepoBaseURL;
	}

	/**
	 * @return the mavenGroupID
	 */
	public String getMavenGroupID() {
		return mavenGroupID;
	}

	/**
	 * @param mavenGroupID
	 *            the mavenGroupID to set
	 */
	public void setMavenGroupID(String mavenGroupID) {
		this.mavenGroupID = mavenGroupID;
	}

	/**
	 * @return the mavenArtifactID
	 */
	public String getMavenArtifactID() {
		return mavenArtifactID;
	}

	/**
	 * @param mavenArtifactID
	 *            the mavenArtifactID to set
	 */
	public void setMavenArtifactID(String mavenArtifactID) {
		this.mavenArtifactID = mavenArtifactID;
	}

	/**
	 * @return the mavenClassifier
	 */
	public String getMavenClassifier() {
		return mavenClassifier;
	}

	/**
	 * @param mavenClassifier
	 *            the mavenClassifier to set
	 */
	public void setMavenClassifier(String mavenClassifier) {
		this.mavenClassifier = mavenClassifier;
	}

	/**
	 * @return the imported
	 */
	public boolean isImported() {
		return imported;
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
	 * @param specificVersionListLoaded
	 *            the specificVersionListLoaded to set
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
	 * @param deletableVersionListLoaded
	 *            the deletableVersionListLoaded to set
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
	 * @param additionalInfoURL
	 *            the additionalInfoURL to set
	 */
	public void setAdditionalInfoURL(URL additionalInfoURL) {
		this.additionalInfoURL = additionalInfoURL;
	}

	/**
	 * Checks if any version of this app is already downloaded
	 * 
	 * @return {@code true} if any version of this app is already downloaded,
	 *         {@code false} otherwise.
	 */
	public boolean isPresentOnHarddrive() {
		// Check if metadata file is present
		return this.getCurrentlyInstalledVersion() != null;
	}

	/**
	 * Checks if the specified version of this app is already downloaded
	 * 
	 * @param ver
	 *            The version to check
	 * @return {@code true} if the specified version of this app is already
	 *         downloaded, {@code false} otherwise.
	 */
	public boolean isPresentOnHarddrive(Version ver) {
		String destFolder = Common.getAndCreateAppDataPath() + getSubfolderToSaveApps();
		String fileName = destFolder + File.separator + AppConfig.appMetadataFileName;

		Element root;
		Document versionDoc;
		Element artifactId;
		Element groupId;
		Element versions;

		try {
			versionDoc = new SAXBuilder().build(fileName);

			root = versionDoc.getRootElement();

			artifactId = root.getChild("artifactId");
			groupId = root.getChild("groupId");
			versions = root.getChild("versions");

			// Check if one of those elements is not defined
			if (artifactId == null) {
				throw new NullPointerException("artifactId is null");
			} else if (groupId == null) {
				throw new NullPointerException("groupId is null");
			} else if (versions == null) {
				throw new NullPointerException("versions is null");
			}

			// Go through all versions
			for (Element version : versions.getChildren()) {
				if (ver.equals(new Version(version.getChild("version").getValue(),
						version.getChild("buildNumber").getValue(), version.getChild("timestamp").getValue()))) {
					// Match found
					return true;
				}
			}

			// No match found, return false
			return false;

		} catch (JDOMException | IOException e) {
			log.getLogger().log(Level.SEVERE, "An error occured!", e);
			return false;
		}
	}

	/**
	 * Downloads the version info and saves it as a xml file in the app folder.
	 * 
	 * @param versionToGet
	 *            The version to get the info for
	 * @param destFolder
	 *            The folder where apps should be saved into
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 * @throws IOException
	 *             If the maven metadata file cannot be downloaded
	 */
	private void downloadVersionInfo(Version versionToGet, String destFolder)
			throws JDOMException, IOException {
		String fileName = destFolder + File.separator + AppConfig.appMetadataFileName;

		Element root;
		Document versionDoc;
		Element artifactId;
		Element groupId;
		Element versions;

		try {
			versionDoc = new SAXBuilder().build(fileName);
			root = versionDoc.getRootElement();

			artifactId = root.getChild("artifactId");
			groupId = root.getChild("groupId");
			versions = root.getChild("versions");

			// Check if one of those elements is not defined
			if (artifactId == null) {
				throw new NullPointerException("artifactId is null");
			} else if (groupId == null) {
				throw new NullPointerException("groupId is null");
			} else if (versions == null) {
				throw new NullPointerException("versions is null");
			}
		} catch (JDOMException | IOException | NullPointerException e) {
			// Could not read document for some reason so generate a new one
			root = new Element("artifactInfo");
			versionDoc = new Document(root);

			artifactId = new Element("artifactId");
			groupId = new Element("groupId");
			versions = new Element("versions");

			root.addContent(artifactId);
			root.addContent(groupId);
			root.addContent(versions);
		}

		artifactId.setText(this.getMavenArtifactID());
		groupId.setText(this.getMavenGroupID());

		boolean versionFound = false;

		for (Element el : versions.getChildren()) {
			Version ver = new Version(el.getChild("version").getValue(), el.getChild("buildNumber").getValue(),
					el.getChild("timestamp").getValue());
			if (ver.equals(versionToGet)) {
				versionFound = true;
			}
		}

		// Check if the specified version is already present
		if (!versionFound) {
			Element version = new Element("version");
			Element versionNumber = new Element("version");
			Element buildNumber = new Element("buildNumber");
			Element timestamp = new Element("timestamp");
			versionNumber.setText(versionToGet.getVersion());

			if (versionToGet.isSnapshot()) {
				buildNumber.setText(versionToGet.getBuildNumber());
				timestamp.setText(versionToGet.getTimestamp());
			}

			version.addContent(versionNumber);
			version.addContent(buildNumber);
			version.addContent(timestamp);

			versions.addContent(version);
		}

		// Write xml-File
		// Create directories if necessary
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		// Create empty file on disk if necessary
		(new XMLOutputter(Format.getPrettyFormat())).output(versionDoc, new FileOutputStream(fileName));

	}

	/**
	 * Gets the Maven Metadata file and converts it into a
	 * {@code JDOM-}{@link Document}
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @return A {@link Document} representation of the maven Metadata file
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 * @throws IOException
	 *             If the maven metadata file cannot be downloaded
	 */
	private Document getMavenMetadata(boolean snapshotsEnabled)
			throws JDOMException, IOException {

		Document mavenMetadata;

		if (snapshotsEnabled) {
			// Snapshots enabled
			mavenMetadata = new SAXBuilder().build(new URL(
					this.getMavenSnapshotRepoBaseURL().toString() + "/" + this.getMavenGroupID().replace('.', '/') + "/"
							+ this.getMavenArtifactID() + "/maven-metadata.xml"));
		} else {
			// Snapshots disabled
			mavenMetadata = new SAXBuilder().build(new URL(this.getMavenRepoBaseURL().toString() + "/"
					+ this.getMavenGroupID().replace('.', '/') + "/" + mavenArtifactID + "/maven-metadata.xml"));
		}

		return mavenMetadata;

	}

	/**
	 * Checks if this artifact needs to be downloaded proir to launching it.
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @return {@code true} if this artifact needs to be downloaded prior to
	 *         launching it, {@code false} otherwise
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 * @throws IOException
	 *             If the maven metadata file cannot be downloaded
	 */
	public boolean downloadRequired(boolean snapshotsEnabled) throws JDOMException, IOException {
		if (this.isPresentOnHarddrive() && (!snapshotsEnabled)
				&& !this.getCurrentlyInstalledVersions().containsRelease()) {
			// App is downloaded, most current version on harddrive is a
			// snapshot but snapshots are disabled, so download is required
			return true;
		} else if (this.isPresentOnHarddrive() && (!snapshotsEnabled)) {
			// App is downloaded, most current version on harddrive is not a
			// snapshot and snapshots are disabled, so no download is required
			return false;
		} else return !(this.isPresentOnHarddrive() && snapshotsEnabled);
	}

	/**
	 * Checks if an update is available for this artifact.
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @return {@code true} if an update is available, {@code false} otherwise
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 * @throws IOException
	 *             If the maven metadata file cannot be downloaded
	 */
	public boolean updateAvailable(boolean snapshotsEnabled) throws JDOMException, IOException {
		Version onlineVersion;

		if (snapshotsEnabled) {
			onlineVersion = this.getLatestOnlineSnapshotVersion();
		} else {
			onlineVersion = this.getLatestOnlineVersion();
		}

		return onlineVersion.compareTo(this.getCurrentlyInstalledVersion(snapshotsEnabled)) == 1;
	}

	/**
	 * Checks if an update is available for the specified artifact version.
	 * 
	 * @param versionToCheck
	 *            The version to be checked.
	 * @return {@code true} if an update is available, {@code false} otherwise
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 * @throws IOException
	 *             If the maven metadata file cannot be downloaded
	 */
	public boolean updateAvailable(Version versionToCheck) throws JDOMException, IOException {
		return versionToCheck.compareTo(this.getCurrentlyInstalledVersion()) == 1;
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards. Does not
	 * take snapshots into account
	 * 
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 */
	public void downloadIfNecessaryAndLaunch() throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(null);
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards. Does not
	 * take snapshots into account
	 * 
	 * @param gui
	 *            A reference to a gui that shows the update and launch
	 *            progress.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 */
	public void downloadIfNecessaryAndLaunch(HidableUpdateProgressDialog gui) throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(false, gui);
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards. Only
	 * takes snapshots into account
	 * 
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 */
	public void downloadSnapshotIfNecessaryAndLaunch() throws IOException, JDOMException {
		downloadSnapshotIfNecessaryAndLaunch(null);
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards. Only
	 * takes snapshots into account
	 * 
	 * @param gui
	 *            A reference to a gui that shows the update and launch
	 *            progress.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 */
	public void downloadSnapshotIfNecessaryAndLaunch(HidableUpdateProgressDialog gui)
			throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(true, gui);
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @param gui
	 *            A reference to a gui that shows the update and launch
	 *            progress.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 */
	public void downloadIfNecessaryAndLaunch(boolean snapshotsEnabled, HidableUpdateProgressDialog gui)
			throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(snapshotsEnabled, gui, false);
	}

	/**
	 * Launches the artifact and forces offline mode. Does not take snapshots
	 * into account.
	 * 
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 * @throws IllegalStateException
	 *             If {@code this.downloadRequired()==true} too
	 */
	public void launchWithoutDownload() throws IOException, JDOMException, IllegalStateException {
		launchWithoutDownload(false);
	}

	/**
	 * Launches the artifact and forces offline mode. Only launches snapshots.
	 * 
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 * @throws IllegalStateException
	 *             If {@code this.downloadRequired()==true} too
	 */
	public void launchSnapshotWithoutDownload() throws IOException, JDOMException, IllegalStateException {
		launchWithoutDownload(true);
	}

	/**
	 * Launches the artifact and forces offline mode
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 * @throws IllegalStateException
	 *             If {@code this.downloadRequired()==true} too
	 */
	public void launchWithoutDownload(boolean snapshotsEnabled)
			throws IOException, JDOMException, IllegalStateException {
		launchWithoutDownload(snapshotsEnabled, null);
	}

	/**
	 * Launches the artifact and forces offline mode
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @param gui
	 *            A reference to a gui that shows the update and launch
	 *            progress.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 * @throws IllegalStateException
	 *             If {@code this.downloadRequired()==true} too
	 */
	public void launchWithoutDownload(boolean snapshotsEnabled, HidableUpdateProgressDialog gui)
			throws IOException, JDOMException, IllegalStateException {
		downloadIfNecessaryAndLaunch(snapshotsEnabled, gui, true);
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards
	 * 
	 * @param snapshotsEnabled
	 *            {@code true} if snapshots shall be taken into account.
	 * @param gui
	 *            A reference to a gui that shows the update and launch
	 *            progress.
	 * @param disableDownload
	 *            If {@code true}, the method will be forced to work offline.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 * @throws IllegalStateException
	 *             If {@code disableDownload==true} but
	 *             {@code this.downloadRequired()==true} too
	 */
	public void downloadIfNecessaryAndLaunch(boolean snapshotsEnabled, HidableUpdateProgressDialog gui,
			boolean disableDownload) throws IOException, JDOMException, IllegalStateException {
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

		downloadIfNecessaryAndLaunch(gui, versionToLaunch, disableDownload);
	}

	/**
	 * Downloads the artifact if necessary and launches it afterwards
	 * 
	 * @param gui
	 *            A reference to a gui that shows the update and launch
	 *            progress.
	 * 
	 * @param versionToLaunch
	 *            The version of the app to be downloaded and launched.
	 * @param disableDownload
	 *            If {@code true}, the method will be forced to work offline.
	 * @throws IOException
	 *             If the maven metadata cannot be downloaded
	 * @throws JDOMException
	 *             If the maven metadata fale is malformed
	 * @throws IllegalStateException
	 *             If {@code disableDownload==true} but
	 *             {@code this.downloadRequired()==true} too
	 */
	public void downloadIfNecessaryAndLaunch(HidableUpdateProgressDialog gui, Version versionToLaunch,
			boolean disableDownload) throws IOException, JDOMException, IllegalStateException {
		cancelDownloadAndLaunch = false;
		String destFolder = Common.getAndCreateAppDataPath() + getSubfolderToSaveApps();
		String destFilename;

		if (!disableDownload) {
			// Continue by default, only cancel, when user cancelled
			boolean downloadPerformed = true;

			// download if necessary
			if (!this.isPresentOnHarddrive(versionToLaunch)) {
				// app not downloaded at all or needs to be updated
				if (!this.isPresentOnHarddrive()) {
					// App was never downloaded
					log.getLogger().info("Downloading package because it was never downloaded before...");
				} else {
					// App needs an update
					log.getLogger().info("Downloading package because an update is available...");
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

		if (this.getMavenClassifier().equals("")) {
			// No classifier
			destFilename = this.getMavenArtifactID() + "-" + versionToLaunch.toString() + ".jar";
		} else {
			destFilename = this.getMavenArtifactID() + "-" + versionToLaunch.toString() + "-"
					+ this.getMavenClassifier() + ".jar";
		}

		if (gui != null) {
			gui.launchStarted();
		}

		String jarFileName = destFolder + File.separator + destFilename;

		// throw exception if the jar can't be found for some unlikely reason
		if (!(new File(jarFileName)).exists()) {
			throw new FileNotFoundException(jarFileName);
		}

		// Set implicit exit = false if handlers are defined when the app exits
		Platform.setImplicitExit(!this.eventHandlersWhenLaunchedAppExitsAttached());

		log.getLogger().info("Launching app using the command: java -jar " + jarFileName + " disableUpdateChecks");
		ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFileName, "disableUpdateChecks",
				"locale=" + Locale.getDefault().getLanguage()).inheritIO();
		Process process;

		if (gui != null) {
			gui.hide();

			log.getLogger().info("Launching application " + destFilename);

			System.out.println("------------------------------------------------------------------");
			System.out.println("The following output is coming from " + destFilename);
			System.out.println("------------------------------------------------------------------");

			process = pb.start();
		} else {
			process = pb.start();
		}

		try {
			process.waitFor();
		} catch (InterruptedException e) {
			log.getLogger().log(Level.SEVERE, "An error occurred", e);
		}

		// Check the status code of the process
		if (process.exitValue() != 0) {
			// Something went wrong
			log.getLogger().log(Level.SEVERE, "The java process returned with exit code " + process.exitValue());
			if (gui != null) {
				gui.showErrorMessage(
						"Something happened while launching the selected app. Try to launch it again and if this error occurs again, try to delete the app and download it again.");
			}
		}

		fireLaunchedAppExits();

	}

	/**
	 * Downloads this artifact to the location specified in the
	 * {@link AdditionalConfig}. Does not take snapshots into account.
	 * 
	 * @return {@code true} if the download finished successfully, {@code false}
	 *         if the download was cancelled using
	 *         {@link #cancelDownloadAndLaunch()}
	 * @throws IOException
	 *             If the version info cannot be read
	 * @throws JDOMException
	 *             If the version xml is malformed
	 * 
	 */
	public boolean download() throws IOException, JDOMException {
		HidableUpdateProgressDialog gui = null;
		return download(gui);
	}

	/**
	 * Downloads this artifact to the location specified in the
	 * {@link AdditionalConfig}. Does not take snapshots into account.
	 * 
	 * @param gui
	 *            The {@link HidableUpdateProgressDialog} that represents the
	 *            gui to inform the user about the progress.
	 * @return {@code true} if the download finished successfully, {@code false}
	 *         if the download was cancelled using
	 *         {@link #cancelDownloadAndLaunch()}
	 * @throws IOException
	 *             If the version info cannot be read
	 * @throws JDOMException
	 *             If the version xml is malformed
	 * 
	 */
	public boolean download(HidableUpdateProgressDialog gui) throws IOException, JDOMException {
		return download(this.getLatestOnlineVersion(), gui);
	}

	/**
	 * Downloads this artifact to the location specified in the
	 * {@link AdditionalConfig}. Only takes snapshots into account.
	 * 
	 * @return {@code true} if the download finished successfully, {@code false}
	 *         if the download was cancelled using
	 *         {@link #cancelDownloadAndLaunch()}
	 * @throws IOException
	 *             If the version info cannot be read
	 * @throws JDOMException
	 *             If the version xml is malformed
	 * 
	 */
	public boolean downloadSnapshot() throws IOException, JDOMException {
		HidableUpdateProgressDialog gui = null;
		return downloadSnapshot(gui);
	}

	/**
	 * Downloads this artifact to the location specified in the
	 * {@link AdditionalConfig}. Only takes snapshots into account.
	 * 
	 * @param gui
	 *            The {@link HidableUpdateProgressDialog} that represents the
	 *            gui to inform the user about the progress.
	 * @return {@code true} if the download finished successfully, {@code false}
	 *         if the download was cancelled using
	 *         {@link #cancelDownloadAndLaunch()}
	 * @throws IOException
	 *             If the version info cannot be read
	 * @throws JDOMException
	 *             If the version xml is malformed
	 * 
	 */
	public boolean downloadSnapshot(HidableUpdateProgressDialog gui) throws IOException, JDOMException {
		return download(this.getLatestOnlineSnapshotVersion(), gui);
	}

	/**
	 * Downloads this artifact to the location specified in the
	 * {@link AdditionalConfig}.
	 * 
	 * @param versionToDownload
	 *            The {@link Version} to be downloaded.
	 * @param gui
	 *            The {@link HidableUpdateProgressDialog} that represents the
	 *            gui to inform the user about the progress.
	 * @return {@code true} if the download finished successfully, {@code false}
	 *         if the download was cancelled using
	 *         {@link #cancelDownloadAndLaunch()}
	 * @throws MalformedURLException
	 *             If the specified repository {@link URL} is malformed
	 * @throws IOException
	 *             If the version info cannot be read
	 * @throws JDOMException
	 *             If the version xml is malformed
	 * 
	 */
	public boolean download(Version versionToDownload, HidableUpdateProgressDialog gui)
			throws IOException, JDOMException {
		if (gui != null) {
			gui.preparePhaseStarted();
		}

		String destFolder = Common.getAndCreateAppDataPath() +getSubfolderToSaveApps();
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
			repoBaseURL = this.getMavenSnapshotRepoBaseURL();
		} else {
			// Not a snapshot
			repoBaseURL = this.getMavenRepoBaseURL();
		}

		// Construct the download url
		if (this.getMavenClassifier().equals("")) {
			artifactURL = new URL(repoBaseURL.toString() + "/" + this.mavenGroupID.replace('.', '/') + "/"
					+ this.getMavenArtifactID() + "/" + versionToDownload.getVersion() + "/" + this.getMavenArtifactID()
					+ "-" + versionToDownload.toString() + ".jar");
		} else {
			artifactURL = new URL(repoBaseURL.toString() + "/" + this.getMavenGroupID().replace('.', '/') + "/"
					+ this.getMavenArtifactID() + "/" + versionToDownload.getVersion() + "/" + this.getMavenArtifactID()
					+ "-" + versionToDownload.toString() + "-" + this.getMavenClassifier() + ".jar");
		}

		// Construct file name of output file
		if (this.getMavenClassifier().equals("")) {
			// No classifier
			destFilename = this.getMavenArtifactID() + "-" + versionToDownload.toString() + ".jar";
		} else {
			destFilename = this.getMavenArtifactID() + "-" + versionToDownload.toString() + "-"
					+ this.getMavenClassifier() + ".jar";
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

		log.getLogger().info("Downloading artifact from " + artifactURL.toString() + "...");
		log.getLogger().info("Downloading to: " + outputFile.getAbsolutePath());
		// FileUtils.copyURLToFile(artifactURL, outputFile);

		HttpURLConnection httpConnection = (HttpURLConnection) (artifactURL.openConnection());
		long completeFileSize = httpConnection.getContentLength();

		java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
		outputFile.getParentFile().mkdirs();
		java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
		java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
		byte[] data = new byte[1024];
		long downloadedFileSize = 0;
		int x = 0;
		while ((x = in.read(data, 0, 1024)) >= 0) {
			downloadedFileSize += x;

			// update progress bar
			if (gui != null) {
				gui.downloadProgressChanged((double) (downloadedFileSize / 1024.0),
						(double) (completeFileSize / 1024.0));
			}

			bout.write(data, 0, x);

			// Perform Cancel if requested
			if (cancelDownloadAndLaunch) {
				bout.close();
				in.close();
				outputFile.delete();
				if (gui != null) {
					gui.operationCanceled();
				}
				return false;
			}
		}
		bout.close();
		in.close();

		// download version info
		downloadVersionInfo(versionToDownload, destFolder);

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

		return true;

	}

	/**
	 * Cancels the download and launch process.
	 */
	public void cancelDownloadAndLaunch() {
		cancelDownloadAndLaunch(null);
	}

	/**
	 * Cancels the download and launch process.
	 * 
	 * @param gui
	 *            The {@link HidableUpdateProgressDialog} that represents the
	 *            gui to inform the user about the progress.
	 */
	public void cancelDownloadAndLaunch(HidableUpdateProgressDialog gui) {
		cancelDownloadAndLaunch = true;

		if (gui != null) {
			gui.cancelRequested();
		}
	}

	/**
	 * Returns a combined list of imported apps and apps from the server.
	 * 
	 * @return A combined list of imported apps and apps from the server.
	 * @throws MalformedURLException
	 *             If the maven repo base url of an app is malformed. The base
	 *             urls are downloaded from the server.
	 * @throws JDOMException
	 *             If the xml-app list on the server or the xml file containing
	 *             info about the imported apps is malformed
	 * @throws IOException
	 *             If the app list or metadata of some apps cannot be downloaded
	 *             or if the xml file containing the info about the imported
	 *             apps cannot be read.
	 * @see App#getOnlineAppList()
	 * @see App#getImportedAppList()
	 */
	public static AppList getAppList() throws JDOMException, IOException {
		AppList res = getOnlineAppList();
		try {
			res.addAll(getImportedAppList());
		} catch (JDOMException | IOException e) {
			log.getLogger().log(Level.SEVERE, "An error occurred", e);
		}
		return res;
	}

	/**
	 * Get a {@link List} of available apps from the server.
	 * 
	 * @return A {@link List} of available apps from the server.
	 * @throws MalformedURLException
	 *             If the maven repo base url of an app is malformed or if an
	 *             app specifies a malformed additionalInfoURL. The base urls
	 *             are downloaded from the server.
	 * 
	 * @throws JDOMException
	 *             If the xml-app list on the server is malformed
	 * @throws IOException
	 *             If the app list or metadata of some apps cannot be
	 *             downloaded.
	 */
	public static AppList getOnlineAppList() throws JDOMException, IOException {
		Document doc = null;
		String fileName = Common.getAndCreateAppDataPath() + File.separator + AppConfig.appListCacheFileName;
		try {
			doc = new SAXBuilder().build(AppConfig.getAppListXMLURL());

			(new XMLOutputter(Format.getPrettyFormat())).output(doc, new FileOutputStream(fileName));
		} catch (UnknownHostException e) {
			try {
				doc = new SAXBuilder().build(new File(fileName));
			} catch (FileNotFoundException e1) {
				throw new UnknownHostException("Could not connect to " + AppConfig.getAppListXMLURL().toString()
						+ " and app list cache not found. \nPlease ensure a stable internet connection.");
			}
		}
		Element fokLauncherEl = doc.getRootElement();
		String modelVersion = fokLauncherEl.getChild("modelVersion").getValue();

		// Check for unsupported modelVersion
		if (!AppConfig.getSupportedFOKConfigModelVersion().contains(modelVersion)) {
			throw new IllegalStateException(
					"The modelVersion of the fokprojectsOnLauncher.xml file is not supported! (modelVersion is "
							+ modelVersion + ")");
		}

		AppList res = new AppList();

		for (Element app : fokLauncherEl.getChild("apps").getChildren("app")) {
			App newApp = new App(app.getChild("name").getValue(), new URL(app.getChild("repoBaseURL").getValue()),
					new URL(app.getChild("snapshotRepoBaseURL").getValue()), app.getChild("groupId").getValue(),
					app.getChild("artifactId").getValue());

			// Add classifier only if one is defined
			if (app.getChild("classifier") != null) {
				newApp.setMavenClassifier(app.getChild("classifier").getValue());
			}

			if (app.getChild("additionalInfoURL") != null) {
				newApp.setAdditionalInfoURL(new URL(app.getChild("additionalInfoURL").getValue()));
			}

			res.add(newApp);
		}

		return res;
	}

	/**
	 * Returns the list of apps that were imported by the user. If any app
	 * cannot be imported, it will be removed from the list permanently
	 * 
	 * @return The list of apps that were imported by the user.
	 * @throws IOException
	 *             If the xml list cannot be read
	 * @throws JDOMException
	 *             If the xml list is malformed
	 */
	public static AppList getImportedAppList() throws JDOMException, IOException {
		String fileName = Common.getAndCreateAppDataPath() + AppConfig.importedAppListFileName;

		try {
			AppList res = new AppList();

			Document appsDoc = new SAXBuilder().build(fileName);
			Element root = appsDoc.getRootElement();

			@SuppressWarnings("unused")
			Element modelVersion = root.getChild("modelVersion");
			Element appsElement = root.getChild("importedApps");

			for (Element app : appsElement.getChildren()) {
				// import the info of every app
				try {
					App a = new App(new File(app.getChild("fileName").getValue()));
					res.add(a);
				} catch (IOException e) {
					// Exception is already logged by the constructor
				}
			}

			return res;
		} catch (FileNotFoundException e) {
			return new AppList();
		}
	}

	/**
	 * Deletes the specified artifact version.
	 * 
	 * @param versionToDelete
	 *            The version to be deleted.
	 * @return {@code true} if the artifact was successfully deleted,
	 *         {@code false} otherwise
	 */
	public boolean delete(Version versionToDelete) {

		// Delete from metadata
		String destFolder = Common.getAndCreateAppDataPath() + getSubfolderToSaveApps();
		String fileName = destFolder + File.separator + AppConfig.appMetadataFileName;
		Document versionDoc;
		Element versions;

		try {
			versionDoc = new SAXBuilder().build(fileName);
			versions = versionDoc.getRootElement().getChild("versions");
		} catch (JDOMException | IOException e) {
			System.err.println("Cannot retreive currently installed version of app " + this.getName()
					+ ", probably because it is not installed.");
			log.getLogger().log(Level.SEVERE, "An error occured!", e);
			return false;
		}

		Element elementToDelete = null;

		// Find the version node to be deleted
		for (Element el : versions.getChildren()) {
			Version ver = new Version(el.getChild("version").getValue(), el.getChild("buildNumber").getValue(),
					el.getChild("timestamp").getValue());
			if (ver.equals(versionToDelete)) {
				elementToDelete = el;
			}
		}

		// Delete the node
		if (elementToDelete != null) {
			elementToDelete.detach();
			try {
				(new XMLOutputter(Format.getPrettyFormat())).output(versionDoc, new FileOutputStream(fileName));
			} catch (IOException e) {
				log.getLogger().log(Level.SEVERE, "An error occured!", e);
			}
		}

		// Delete the file
		String appFileName;
		if (this.getMavenClassifier().equals("")) {
			// No classifier
			appFileName = this.getMavenArtifactID() + "-" + versionToDelete.toString() + ".jar";
		} else {
			appFileName = this.getMavenArtifactID() + "-" + versionToDelete.toString() + "-" + this.getMavenClassifier()
					+ ".jar";
		}

		File appFile = new File(destFolder + File.separator + appFileName);

		return appFile.delete();
	}

	/**
	 * Adds a handler for the event that the launched app exits again.
	 * 
	 * @param handler
	 *            The handler to add.
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
	 * @param handler
	 *            The handler to remove.
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
	 * @param handler
	 *            The handler to be checked.
	 * @return {@code true} if the handler is already attached, {@code false}
	 *         otherwise
	 */
	public boolean isEventHandlerWhenLaunchedAppExitsAttached(Runnable handler) {
		return eventHandlersWhenLaunchedAppExits.contains(handler);
	}

	/**
	 * Checks if any handler is attached to the event that the launched app
	 * exits again.
	 * 
	 * @return {@code true} if any event handler is attached, {@code false}, if
	 *         no event handler is attached.
	 */
	public boolean eventHandlersWhenLaunchedAppExitsAttached() {
		return eventHandlersWhenLaunchedAppExits.size() > 0;
	}

	/**
	 * Fires all handlers registered for the launchedAppExits event.
	 */
	private void fireLaunchedAppExits() {
		log.getLogger().info("The launched app exited and the LaunchedAppExits event is now fired.");
		for (Runnable handler : eventHandlersWhenLaunchedAppExits) {
			Platform.runLater(handler);
		}
	}

	/**
	 * Exports the info of this app to a *.foklauncher file
	 * 
	 * @param fileToWrite
	 *            The {@link File} to be written to. If the file already exists
	 *            on the disk, it will be overwritten.
	 * @throws IOException
	 *             If something happens while saving the file
	 */
	public void exportInfo(File fileToWrite) throws IOException {
		if (!fileToWrite.exists()) {
			// Create a new file
			fileToWrite.createNewFile();
		}

		// We either have an empty file or a file that needs to be overwritten
		Properties props = new Properties();

		props.setProperty("name", this.getName());
		props.setProperty("repoBaseURL", this.getMavenRepoBaseURL().toString());
		props.setProperty("snapshotRepoBaseURL", this.getMavenSnapshotRepoBaseURL().toString());
		props.setProperty("groupId", this.getMavenGroupID());
		props.setProperty("artifactId", this.getMavenArtifactID());
		props.setProperty("classifier", this.getMavenClassifier());
		if (this.getAdditionalInfoURL() != null) {
			props.setProperty("additionalInfoURL", this.getAdditionalInfoURL().toString());
		}

		FileOutputStream out = new FileOutputStream(fileToWrite);
		props.store(out, "This file stores info about a java app. To open this file, get the foklauncher");
		out.close();
	}

	/**
	 * Imports the info of this app from a *.foklauncher file.
	 * 
	 * @param fileToImport
	 *            The {@link File} to be read from.
	 * @throws IOException
	 *             If the specified file is not a file (but a directory) or if
	 *             the launcher has no permission to read the file.
	 */
	private void importInfo(File fileToImport) throws IOException {
		this.imported = true;
		this.importFile = fileToImport;
		FileReader fileReader = null;
		if (!fileToImport.isFile()) {
			// Not a file
			throw new IOException("The specified file is not a file");
		} else if (!fileToImport.canRead()) {
			// Cannot write to file
			throw new IOException("The specified file is read-only");
		}

		Properties props = new Properties();
		if (fileToImport.exists()) {
			// Load the properties
			fileReader = new FileReader(fileToImport);
			props.load(fileReader);
		}

		this.setName(props.getProperty("name"));
		this.setMavenRepoBaseURL(new URL(props.getProperty("repoBaseURL")));
		this.setMavenSnapshotRepoBaseURL(new URL(props.getProperty("snapshotRepoBaseURL")));
		this.setMavenGroupID(props.getProperty("groupId"));
		this.setMavenArtifactID(props.getProperty("artifactId"));
		this.setMavenClassifier(props.getProperty("classifier"));

		if (!props.getProperty("additionalInfoURL", "").equals("")) {
			this.setAdditionalInfoURL(new URL(props.getProperty("additionalInfoURL")));
		}

		fileReader.close();
	}

	public static void addImportedApp(File infoFile) throws IOException {
		String fileName = Common.getAndCreateAppDataPath() + AppConfig.importedAppListFileName;

		Element root;
		Document appsDoc;
		Element modelVersion;
		Element appsElement;

		try {
			appsDoc = new SAXBuilder().build(fileName);
			root = appsDoc.getRootElement();

			modelVersion = root.getChild("modelVersion");
			appsElement = root.getChild("importedApps");

			// Check if one of those elements is not defined
			if (modelVersion == null) {
				throw new NullPointerException("modelVersion is null");
			} else if (appsElement == null) {
				throw new NullPointerException("appsElement is null");
			}
		} catch (JDOMException | IOException | NullPointerException e) {
			// Could not read document for some reason so generate a new one
			root = new Element("fokLauncher");
			appsDoc = new Document(root);

			modelVersion = new Element("modelVersion");
			appsElement = new Element("importedApps");

			root.addContent(modelVersion);
			root.addContent(appsElement);
		}

		modelVersion.setText("0.0.1");

		boolean fileFound = false;

		for (Element app : appsElement.getChildren()) {
			if (app.getChild("fileName").getValue().equals(infoFile.getAbsolutePath())) {
				fileFound = true;
			}
		}

		// Check if the specified version is already present
		if (!fileFound) {
			Element app = new Element("app");
			Element fileNameElement = new Element("fileName");

			fileNameElement.setText(infoFile.getAbsolutePath());

			app.addContent(fileNameElement);

			appsElement.addContent(app);
		}

		// Write xml-File
		// Create directories if necessary
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		// Create empty file on disk if necessary
		(new XMLOutputter(Format.getPrettyFormat())).output(appsDoc, new FileOutputStream(fileName));
	}

	public void removeFromImportedAppList() throws IOException {
		String fileName = Common.getAndCreateAppDataPath() + AppConfig.importedAppListFileName;

		Element root;
		Document appsDoc;
		Element modelVersion;
		Element appsElement;

		try {
			appsDoc = new SAXBuilder().build(fileName);
			root = appsDoc.getRootElement();

			modelVersion = root.getChild("modelVersion");
			appsElement = root.getChild("importedApps");

			// Check if one of those elements is not defined
			if (modelVersion == null) {
				throw new NullPointerException("modelVersion is null");
			} else if (appsElement == null) {
				throw new NullPointerException("appsElement is null");
			}
		} catch (JDOMException | IOException | NullPointerException e) {
			// Could not read document for some reason so generate a new one
			root = new Element("fokLauncher");
			appsDoc = new Document(root);

			modelVersion = new Element("modelVersion");
			appsElement = new Element("importedApps");

			root.addContent(modelVersion);
			root.addContent(appsElement);
		}

		modelVersion.setText("0.0.1");

		List<Element> appsToDetach = new ArrayList<Element>();

		for (Element app : appsElement.getChildren()) {
			if (app.getChild("fileName").getValue().equals(this.getImportFile().getAbsolutePath())) {
				// Collect elements to be detached
				appsToDetach.add(app);
			}
		}

		// Detach them
		for (Element app : appsToDetach) {
			app.detach();
		}

		// Write xml-File
		// Create directories if necessary
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		// Create empty file on disk if necessary
		(new XMLOutputter(Format.getPrettyFormat())).output(appsDoc, new FileOutputStream(fileName));
	}

	public void createShortCut(File shortcutFile, String quickInfoText) throws IOException {
		if (SystemUtils.IS_OS_WINDOWS) {
			ShellLink sl = ShellLink.createLink(new File(Common.getPathAndNameOfCurrentJar()).toPath().toString());

			if (this.getMavenClassifier().equals("")) {
				// no classifier set
				sl.setCMDArgs("launch autolaunchrepourl=" + this.getMavenRepoBaseURL().toString()
						+ " autolaunchsnapshotrepourl=" + this.getMavenSnapshotRepoBaseURL().toString()
						+ " autolaunchgroupid=" + this.getMavenGroupID() + " autolaunchartifactid="
						+ this.getMavenArtifactID());
			} else {
				sl.setCMDArgs("launch autolaunchrepourl=" + this.getMavenRepoBaseURL().toString()
						+ " autolaunchsnapshotrepourl=" + this.getMavenSnapshotRepoBaseURL().toString()
						+ " autolaunchgroupid=" + this.getMavenGroupID() + " autolaunchartifactid="
						+ this.getMavenArtifactID() + " autolaunchclassifier=" + this.getMavenClassifier());
			}
			
			sl.setName(quickInfoText.replace("%s", this.getName()));

			if (common.Common.getPackaging().equals("exe")) {
				sl.setIconLocation(new File(Common.getPathAndNameOfCurrentJar()).toPath().toString());
			} else {
				URL inputUrl = MainWindow.class.getResource("icon.ico");
				File dest = new File(Common.getAndCreateAppDataPath() + "icon.ico");
				FileUtils.copyURLToFile(inputUrl, dest);
				sl.setIconLocation(dest.getAbsolutePath());
			}

			sl.saveTo(shortcutFile.toPath().toString());
			
		} else {
			// Actually does not create a shortcut but a bash script
			System.out.println(Common.getPathAndNameOfCurrentJar());
		}
		// Files.createLink(shortcutFile.toPath(), new
		// File(Common.getPathAndNameOfCurrentJar()).toPath());
	}

	@Override
	public String toString() {
		if (this.getName() != null) {
			return this.getName();
		} else {
			return "";
		}
	}

	public ContextMenu getContextMenu(){
		if (contextMenu==null){
			contextMenu = this.generateContextMenu();
		}

		return contextMenu;
	}

	private ContextMenu generateContextMenu(){
		ContextMenu contextMenu = new ContextMenu();

		Menu launchSpecificVersionItem = new Menu();
		// launchSpecificVersionItem.textProperty().bind(Bindings.format(bundle.getString("launchSpecificVersion"), cell.itemProperty()));
		launchSpecificVersionItem.setText(bundle.getString("launchSpecificVersion").replace("%s", this.toString()));

		MenuItem dummyVersion = new MenuItem();
		dummyVersion.setText(bundle.getString("waitForVersionList"));
		launchSpecificVersionItem.getItems().add(dummyVersion);
		launchSpecificVersionItem.setOnHiding(event2 -> {
			MainWindow.launchSpecificVersionMenuCanceled = true;
		});
		App app = this;
		launchSpecificVersionItem.setOnShown(event -> {
			MainWindow.launchSpecificVersionMenuCanceled = false;
			Thread buildContextMenuThread = new Thread() {
				@Override
				public void run() {
					log.getLogger().info("Getting available online versions...");

					// Get available versions
					VersionList verList = new VersionList();
					if (!MainWindow.currentMainWindowInstance.workOffline()) {
						// Online mode enabled
						try {
							verList = app.getAllOnlineVersions();
							if (MainWindow.currentMainWindowInstance.snapshotsEnabled()) {
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

					// Sort the list
					Collections.sort(verList);

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
							// Launch the download
							MainWindow.downloadAndLaunchThread = new Thread() {
								@Override
								public void run() {
									try {
										// Attach the on app
										// exit handler if
										// required
										if (MainWindow.currentMainWindowInstance.launchLauncherAfterAppExitCheckbox.isSelected()) {
											app
													.addEventHandlerWhenLaunchedAppExits(showLauncherAgain);
										} else {
											app.removeEventHandlerWhenLaunchedAppExits(
													showLauncherAgain);
										}
										app.downloadIfNecessaryAndLaunch(
												currentMainWindowInstance, menuItem.getVersion(),
												MainWindow.currentMainWindowInstance.workOffline());
									} catch (IOException | JDOMException e) {
										currentMainWindowInstance.showErrorMessage(
												"An error occurred: \n" + ExceptionUtils.getStackTrace(e));
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

			if (!this.isSpecificVersionListLoaded()) {
				buildContextMenuThread.setName("buildContextMenuThread");
				buildContextMenuThread.start();
				this.setSpecificVersionListLoaded(true);
			}
		});

		Menu deleteItem = new Menu();
		//deleteItem.textProperty().bind(Bindings.format(bundle.getString("deleteVersion"), cell.itemProperty()));
		deleteItem.setText(bundle.getString("deleteVersion").replace("%s", this.toString()));
		MenuItem dummyVersion2 = new MenuItem();
		dummyVersion2.setText(bundle.getString("waitForVersionList"));
		deleteItem.getItems().add(dummyVersion2);

		deleteItem.setOnShown(event -> {
			// App app = apps.get(cell.getIndex());

			if (!app.isDeletableVersionListLoaded()) {
				// Get deletable versions
				app.setDeletableVersionListLoaded(true);
				log.getLogger().info("Getting deletable versions...");
				deleteItem.getItems().clear();

				VersionList verList = new VersionList();
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
						} finally {
							MainWindow.currentMainWindowInstance.updateLaunchButton();
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

		MenuItem createShortcutOnDesktopMenuItem = new MenuItem();
		createShortcutOnDesktopMenuItem.setText(bundle.getString("createShortcutOnDesktop"));
		createShortcutOnDesktopMenuItem.setOnAction(event3 -> {
			log.getLogger().info("Creating shortcut...");
			File file = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()
					+ File.separator + app.getName() + ".lnk");
			try {
				log.getLogger().info("Creating shortcut for app " + app.getName()
						+ " at the following location: " + file.getAbsolutePath());
				app.createShortCut(file, bundle.getString("shortcutQuickInfo"));
			} catch (Exception e) {
				log.getLogger().log(Level.SEVERE, "An error occurred", e);
				currentMainWindowInstance.showErrorMessage(e.toString());
			}
		});

		MenuItem createShortcutMenuItem = new MenuItem();
		createShortcutMenuItem.setText(bundle.getString("createShortcut"));
		createShortcutMenuItem.setOnAction(event3 -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters()
					.addAll(new FileChooser.ExtensionFilter(bundle.getString("shortcut"), "*.lnk"));
			fileChooser.setTitle(bundle.getString("saveShortcut"));
			File file = fileChooser.showSaveDialog(stage);
			if (file != null) {
				log.getLogger().info("Creating shortcut...");

				try {
					log.getLogger().info("Creating shortcut for app " + this.getName()
							+ " at the following location: " + file.getAbsolutePath());
					this.createShortCut(file, bundle.getString("shortcutQuickInfo"));
				} catch (Exception e) {
					log.getLogger().log(Level.SEVERE, "An error occurred", e);
					currentMainWindowInstance.showErrorMessage(e.toString());
				}
			}
		});

		MenuItem exportInfoItem = new MenuItem();
		exportInfoItem.setText(bundle.getString("exportInfo"));
		exportInfoItem.setOnAction(event2 -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters()
					.addAll(new FileChooser.ExtensionFilter("FOK-Launcher-File", "*.foklauncher"));
			fileChooser.setTitle("Save Image");
			// TODO Translation
			File file = fileChooser.showSaveDialog(stage);
			if (file != null) {
				log.getLogger().info("Exporting info...");
				// App app = apps.get(cell.getIndex());

				try {
					log.getLogger().info("Exporting app info of app " + app.getName() + " to file: "
							+ file.getAbsolutePath());
					app.exportInfo(file);
				} catch (IOException e) {
					log.getLogger().log(Level.SEVERE, "An error occurred", e);
					currentMainWindowInstance.showErrorMessage(e.toString());
				}
			}
		});

		contextMenu.getItems().addAll(launchSpecificVersionItem, deleteItem,
				createShortcutOnDesktopMenuItem, createShortcutMenuItem, exportInfoItem);

		MenuItem removeImportedApp = new MenuItem();
		contextMenu.setOnShowing(event5 -> {
			if (app.isImported()) {
				removeImportedApp.setText(bundle.getString("deleteImportedApp"));
				removeImportedApp.setOnAction(event3 -> {
					try {
						app.removeFromImportedAppList();
						currentMainWindowInstance.loadAppList();
					} catch (IOException e) {
						log.getLogger().log(Level.SEVERE, "An error occurred", e);
						currentMainWindowInstance.showErrorMessage(e.toString());
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

	private String getSubfolderToSaveApps(){
	    if (this.getMavenClassifier().equals("")){
	        // No classifier
	        return AppConfig.subfolderToSaveApps
                    .replace("{groupId}", this.getMavenGroupID()).replace("{artifactId}", this.getMavenArtifactID());
        }else{
            return AppConfig.subfolderToSaveApps
                    .replace("{groupId}", this.getMavenGroupID()).replace("{artifactId}", this.getMavenArtifactID()).replace("{classifier}", this.getMavenClassifier());
        }
    }
}

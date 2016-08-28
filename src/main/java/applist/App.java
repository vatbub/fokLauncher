package applist;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import common.*;
import javafx.application.Platform;
import logging.FOKLogger;

public class App {

	FOKLogger log = new FOKLogger(App.class.getName());

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

	private boolean specificVersionListLoaded = false;

	private boolean deletableVersionListLoaded = false;

	/**
	 * A list of event handlers that handle the event that this app was launched
	 * and exited then
	 */
	private List<Runnable> eventHandlersWhenLaunchedAppExits = new ArrayList<Runnable>();

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
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 */
	public VersionList getAllOnlineVersions() throws MalformedURLException, JDOMException, IOException {
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
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 */
	public Version getLatestOnlineVersion() throws MalformedURLException, JDOMException, IOException {
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
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 */
	public Version getLatestOnlineSnapshotVersion() throws MalformedURLException, JDOMException, IOException {
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
		String destFolder = Common.getAndCreateAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String fileName = destFolder + File.separator + Config.appMetadataFileName;
		Document versionDoc;

		try {
			versionDoc = new SAXBuilder().build(fileName);
		} catch (JDOMException | IOException e) {
			System.err.println("Cannot retreive currently installed version of app " + this.getName()
					+ ", probably because it is not installed.");
			log.getLogger().log(Level.SEVERE, "An error occured!", e);
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
		String destFolder = Common.getAndCreateAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String fileName = destFolder + File.separator + Config.appMetadataFileName;

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
			throws MalformedURLException, JDOMException, IOException {
		String fileName = destFolder + File.separator + Config.appMetadataFileName;

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
		// f.createNewFile();
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
			throws MalformedURLException, JDOMException, IOException {

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
	public boolean downloadRequired(boolean snapshotsEnabled) throws MalformedURLException, JDOMException, IOException {
		if (this.isPresentOnHarddrive() && (!snapshotsEnabled)
				&& !this.getCurrentlyInstalledVersions().containsRelease()) {
			// App is downloaded, most current version on harddrive is a
			// snapshot but snapshots are disabled, so download is required
			return true;
		} else if (this.isPresentOnHarddrive() && (!snapshotsEnabled)) {
			// App is downloaded, most current version on harddrive is not a
			// snapshot and snapshots are disabled, so no download is required
			return false;
		} else if (this.isPresentOnHarddrive() && snapshotsEnabled) {
			// A version is available on the harddrive and snapshots are
			// enabled, so we don't matter if the downloaded version is a
			// snapshot or not.
			return false;
		} else {
			// App not downloaded at all
			return true;
		}
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
	public boolean updateAvailable(boolean snapshotsEnabled) throws MalformedURLException, JDOMException, IOException {
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
	 *            The version to be checked. * @return {@code true} if an update
	 *            is available, {@code false} otherwise
	 * @throws MalformedURLException
	 *             If the repo base url is malformed
	 * @throws JDOMException
	 *             If the maven metadata file is malformed
	 * @throws IOException
	 *             If the maven metadata file cannot be downloaded
	 */
	public boolean updateAvailable(Version versionToCheck) throws MalformedURLException, JDOMException, IOException {
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
		String destFolder = Common.getAndCreateAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String destFilename;

		if (!disableDownload) {
			// Continue by default, only cancel, when user cancelled
			boolean downloadPerformed = true;

			// download if necessary
			if (!this.isPresentOnHarddrive(versionToLaunch)) {
				// app not downloaded at all
				log.getLogger().info("Downloading package because it was never downloaded before...");
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

		log.getLogger().info("Launching app using the command: java -jar " + jarFileName + " disableUpdateChecks");
		ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFileName, "disableUpdateChecks").inheritIO();
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
		if (process.exitValue()!=0){
			// Something went wrong
			log.getLogger().log(Level.SEVERE, "The java process returned with exit code "  + process.exitValue());
			if (gui!=null){
				gui.showErrorMessage("Something happened while launching the selected app. Try to launch it again and if this error occurs again, try to delete the app and download it again.");
			}
		}

		fireLaunchedAppExits();

	}

	/**
	 * Downloads this artifact to the location specified in the {@link Config}.
	 * Does not take snapshots into account.
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
	 * Downloads this artifact to the location specified in the {@link Config}.
	 * Does not take snapshots into account.
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
	 * Downloads this artifact to the location specified in the {@link Config}.
	 * Only takes snapshots into account.
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
	 * Downloads this artifact to the location specified in the {@link Config}.
	 * Only takes snapshots into account.
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
	 * Downloads this artifact to the location specified in the {@link Config}.
	 * 
	 * @param isSnapshot
	 *            If {@code true}, the latest snapshot will be downloaded,
	 *            otherwise, the latest release will be downloaded.
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
			throws MalformedURLException, IOException, JDOMException {
		if (gui != null) {
			gui.preparePhaseStarted();
		}

		String destFolder = Common.getAndCreateAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
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

			// calculate progress
			// final int currentProgress = (int)
			// ((((double)downloadedFileSize) / ((double)completeFileSize))
			// * 100000d);

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
		log.getLogger().info("Requested to cancel the current operation, Cancel in progress...");

		if (gui != null) {
			gui.cancelRequested();
		}
	}

	/**
	 * Get a {@link List} of available apps from the server.
	 * 
	 * @return A {@link List} of available apps from the server.
	 * @throws MalformedURLException
	 *             If the maven repo base url of an app is malformed. The base
	 *             urls are downloaded from the server.
	 * @throws JDOMException
	 *             If the xml-app list on the server is malformed
	 * @throws IOException
	 *             If the app list or metadata of some apps cannot bedownloaded.
	 */
	public static AppList getAppList() throws MalformedURLException, JDOMException, IOException {
		Document doc = null;
		String fileName = Common.getAndCreateAppDataPath() + File.separator + Config.appListCacheFileName;
		try {
			doc = new SAXBuilder().build(Config.getAppListXMLURL());

			(new XMLOutputter(Format.getPrettyFormat())).output(doc, new FileOutputStream(fileName));
		} catch (UnknownHostException e) {
			try {
				doc = new SAXBuilder().build(new File(fileName));
			} catch (FileNotFoundException e1) {
				throw new UnknownHostException("Could not connect to " + Config.getAppListXMLURL().toString()
						+ " and app list cache not found. \nPlease ensure a stable internet connection.");
			}
		}
		Element fokLauncherEl = doc.getRootElement();
		String modelVersion = fokLauncherEl.getChild("modelVersion").getValue();

		// Check for unsupported modelVersion
		if (!Config.getSupportedFOKConfigModelVersion().contains(modelVersion)) {
			throw new IllegalStateException(
					"The modelVersion of the fokprojectsOnLauncher.xml file is not supported! (modelVersion is "
							+ modelVersion + ")");
		}

		AppList res = new AppList();

		for (Element app : fokLauncherEl.getChild("apps").getChildren("app")) {
			App newApp = new App();

			newApp.setName(app.getChild("name").getValue());
			newApp.setMavenRepoBaseURL(new URL(app.getChild("repoBaseURL").getValue()));
			newApp.setMavenSnapshotRepoBaseURL(new URL(app.getChild("snapshotRepoBaseURL").getValue()));
			newApp.setMavenGroupID(app.getChild("groupId").getValue());
			newApp.setMavenArtifactID(app.getChild("artifactId").getValue());

			// Add classifier only if one is defined
			if (app.getChild("classifier") != null) {
				newApp.setMavenClassifier(app.getChild("classifier").getValue());
			}

			res.add(newApp);
		}

		return res;
	}

	/**
	 * Deletes the specified artifact version.
	 * 
	 * @param versionToDelete
	 *            The version to be deleted.
	 * @return {@code true} if the artifact was successfully deleted,
	 *         {@code false} otherwise
	 * @throws IOException
	 */
	public boolean delete(Version versionToDelete) throws IOException {

		// Delete from metadata
		String destFolder = Common.getAndCreateAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String fileName = destFolder + File.separator + Config.appMetadataFileName;
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
	 * Fires all handlers registered for the launchedAppExits event.
	 */
	private void fireLaunchedAppExits() {
		log.getLogger().info("The launched app exited and the LaunchedAppExits event is now fired.");
		for (Runnable handler : eventHandlersWhenLaunchedAppExits) {
			Platform.runLater(handler);
		}
	}
}

package applist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import common.*;

public class App {

	/**
	 * The name of the app
	 */
	private String name;

	/**
	 * The latest version of the app that is available online
	 */
	private Version latestOnlineVersion;

	private Version latestOnlineSnapshotVersion;

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
	 * @return the latestOnlineVersion
	 * @throws IOException
	 * @throws JDOMException
	 * @throws MalformedURLException
	 */
	public Version getLatestOnlineVersion() throws MalformedURLException, JDOMException, IOException {
		if (latestOnlineVersion != null) {
			return latestOnlineVersion;
		} else {
			Document mavenMetadata = getMavenMetadata(false);

			Version res = new Version(
					mavenMetadata.getRootElement().getChild("versioning").getChild("latest").getValue());

			if (res.isSnapshot()) {
				throw new IllegalStateException(
						"Latest version in this repository is a snapshot and not a release. This might happen if you host snapshots and releases in the same repository (which is not recommended). If you still need this case to be covered, please submit an issue at https://github.com/vatbub/fokLauncher/issues");
			}

			latestOnlineVersion = res;
			return res;
		}
	}

	/**
	 * @return the latestOnlineSnapshotVersion
	 * @throws IOException
	 * @throws JDOMException
	 * @throws MalformedURLException
	 */
	public Version getLatestOnlineSnapshotVersion() throws MalformedURLException, JDOMException, IOException {
		if (latestOnlineSnapshotVersion != null) {
			return latestOnlineSnapshotVersion;
		} else {
			Document mavenMetadata = getMavenMetadata(true);

			Version res = new Version(
					mavenMetadata.getRootElement().getChild("versioning").getChild("latest").getValue());

			Document snapshotMetadata = new SAXBuilder().build(new URL(this.getMavenSnapshotRepoBaseURL().toString()
					+ "/" + mavenGroupID + "/" + mavenArtifactID + "/" + res.getVersion() + "/maven-metadata.xml"));

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

			latestOnlineSnapshotVersion = res;
			return res;
		}
	}

	/**
	 * Returns the currently installed version of the app or {@code null} if the
	 * app is not yet installed locally.
	 * 
	 * @return the currentlyInstalledVersion
	 * @see #isPresentOnHarddrive()
	 */
	public Version getCurrentlyInstalledVersion() {
		Version res = null;

		// Load the metadata.xml file
		String destFolder = Common.getAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String fileName = destFolder + File.separator + Config.appMetadataFileName;
		Document versionDoc;

		try {
			versionDoc = new SAXBuilder().build(fileName);
		} catch (JDOMException | IOException e) {
			System.err.println("Cannot retreive currently installed version of app " + this.getName()
					+ ", probably because it is not installed.");
			e.printStackTrace();
			return null;
		}

		res = new Version(versionDoc.getRootElement().getChild("version").getValue());
		res.setBuildNumber(versionDoc.getRootElement().getChild("buildNumber").getValue());
		res.setTimestamp(versionDoc.getRootElement().getChild("timestamp").getValue());

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
	 * Checks if this app is already downloaded
	 * 
	 * @return {@code true} if the app is already downloaded, {@code false}
	 *         otherwise.
	 */
	public boolean isPresentOnHarddrive() {
		// Check if metadata file is present
		String destFolder = Common.getAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String fileName = destFolder + File.separator + Config.appMetadataFileName;
		File metadata = new File(fileName);
		return metadata.exists();
	}

	private void downloadVersionInfo(boolean snapshotsEnabled, String destFolder)
			throws MalformedURLException, JDOMException, IOException {
		Version onlineVersion;

		if (snapshotsEnabled) {
			onlineVersion = this.getLatestOnlineSnapshotVersion();
		} else {
			onlineVersion = this.getLatestOnlineVersion();
		}

		Element root = new Element("artifactInfo");
		Document versionDoc = new Document(root);

		Element artifactId = new Element("artifactId");
		Element groupId = new Element("groupId");
		Element version = new Element("version");
		Element buildNumber = new Element("buildNumber");
		Element timestamp = new Element("timestamp");

		artifactId.setText(this.getMavenArtifactID());
		groupId.setText(this.getMavenGroupID());
		version.setText(onlineVersion.getVersion());

		if (snapshotsEnabled) {
			buildNumber.setText(onlineVersion.getBuildNumber());
			timestamp.setText(onlineVersion.getTimestamp());
		}

		root.addContent(artifactId);
		root.addContent(groupId);
		root.addContent(version);
		root.addContent(buildNumber);
		root.addContent(timestamp);

		// Write xml-File
		String fileName = destFolder + File.separator + Config.appMetadataFileName;
		(new XMLOutputter(Format.getPrettyFormat())).output(versionDoc, new FileOutputStream(fileName));

	}

	private Document getMavenMetadata(boolean snapshotsEnabled)
			throws MalformedURLException, JDOMException, IOException {

		Document mavenMetadata;

		if (snapshotsEnabled) {
			// Snapshots enabled
			mavenMetadata = new SAXBuilder().build(new URL(this.getMavenSnapshotRepoBaseURL().toString() + "/"
					+ this.getMavenGroupID() + "/" + this.getMavenArtifactID() + "/maven-metadata.xml"));
		} else {
			// Snapshots disabled
			mavenMetadata = new SAXBuilder().build(new URL(this.getMavenRepoBaseURL().toString() + "/" + mavenGroupID
					+ "/" + mavenArtifactID + "/maven-metadata.xml"));
		}

		return mavenMetadata;

	}

	public boolean downloadRequired(boolean snapshotsEnabled) throws MalformedURLException, JDOMException, IOException {
		if (this.isPresentOnHarddrive() && (!snapshotsEnabled) && this.getCurrentlyInstalledVersion().isSnapshot()) {
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

	public boolean updateAvailable(boolean snapshotsEnabled) throws MalformedURLException, JDOMException, IOException {
		Version onlineVersion;

		if (snapshotsEnabled) {
			onlineVersion = this.getLatestOnlineSnapshotVersion();
		} else {
			onlineVersion = this.getLatestOnlineVersion();
		}

		return onlineVersion.compareTo(this.getCurrentlyInstalledVersion()) == 1;
	}

	public void downloadIfNecessaryAndLaunch() throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(null);
	}

	public void downloadIfNecessaryAndLaunch(HidableUpdateProgressDialog gui) throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(false, gui);
	}

	public void downloadSnapshotIfNecessaryAndLaunch() throws IOException, JDOMException {
		downloadSnapshotIfNecessaryAndLaunch(null);
	}

	public void downloadSnapshotIfNecessaryAndLaunch(HidableUpdateProgressDialog gui)
			throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(true, gui);
	}

	public void downloadIfNecessaryAndLaunch(boolean snapshotsEnabled, HidableUpdateProgressDialog gui)
			throws IOException, JDOMException {
		downloadIfNecessaryAndLaunch(snapshotsEnabled, gui, false);
	}

	public void launchWithoutDownload() throws IOException, JDOMException, IllegalStateException {
		launchWithoutDownload(false);
	}

	public void launchSnapshotWithoutDownload() throws IOException, JDOMException, IllegalStateException {
		launchWithoutDownload(true);
	}

	public void launchWithoutDownload(boolean snapshotsEnabled)
			throws IOException, JDOMException, IllegalStateException {
		launchWithoutDownload(snapshotsEnabled, null);
	}

	public void launchWithoutDownload(boolean snapshotsEnabled, HidableUpdateProgressDialog gui)
			throws IOException, JDOMException, IllegalStateException {
		downloadIfNecessaryAndLaunch(snapshotsEnabled, gui, true);
	}

	public void downloadIfNecessaryAndLaunch(boolean snapshotsEnabled, HidableUpdateProgressDialog gui,
			boolean disableDownload) throws IOException, JDOMException {
		cancelDownloadAndLaunch = false;
		String destFolder = Common.getAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String destFilename;
		Version destVersion;

		if (!disableDownload) {
			// Continue by default, only cancel, when user cancelled
			boolean downloadPerformed = true;

			// download if necessary
			if (this.downloadRequired(snapshotsEnabled)) {
				// app not downloaded at all
				System.out.println("Downloading package because it was never downloaded before...");
				downloadPerformed = this.download(snapshotsEnabled, gui);
			} else if (this.updateAvailable(snapshotsEnabled)) {
				// newer version available
				System.out.println("Downloading package because a newer version is available...");
				downloadPerformed = this.download(snapshotsEnabled, gui);
			}

			if (!downloadPerformed) {
				// Download was cancelled by the user so return
				return;
			}
		} else if (this.downloadRequired(snapshotsEnabled)) {
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

		destVersion = this.getCurrentlyInstalledVersion();

		if (this.getMavenClassifier().equals("")) {
			// No classifier
			destFilename = this.getMavenArtifactID() + "-" + destVersion.toString() + ".jar";
		} else {
			destFilename = this.getMavenArtifactID() + "-" + destVersion.toString() + "-" + this.getMavenClassifier()
					+ ".jar";
		}

		if (gui != null) {
			gui.launchStarted();
		}

		System.out.println("Launching app using the command: java -jar " + destFolder + File.separator + destFilename
				+ " disableUpdateChecks");
		ProcessBuilder pb = new ProcessBuilder("java", "-jar", destFolder + File.separator + destFilename,
				"disableUpdateChecks").inheritIO();

		if (gui != null) {
			gui.hide();

			System.out.println("------------------------------------------------------------------");
			System.out.println("The following output is coming from " + destFilename);
			System.out.println("------------------------------------------------------------------");

			pb.start();
		} else {
			System.exit(0);
			pb.start();
		}
	}

	public boolean download() throws IOException, JDOMException {
		return download(null);
	}

	public boolean download(HidableUpdateProgressDialog gui) throws IOException, JDOMException {
		return download(false, gui);
	}

	public boolean downloadSnapshot() throws IOException, JDOMException {
		return downloadSnapshot(null);
	}

	public boolean downloadSnapshot(HidableUpdateProgressDialog gui) throws IOException, JDOMException {
		return download(true, gui);
	}

	/**
	 * Downloads this artifact to the location specified in the {@link Config}.
	 * TODO: Copy javadoc to other callers
	 * 
	 * @param isSnapshot
	 *            If {@code true}, the latest snapshot will be downloaded,
	 *            otherwise, the latest release will be downloaded.
	 * @param gui
	 *            The {@link HidableUpdateProgressDialog} that represents the
	 *            gui to inform the user about the progress.
	 * @return {@code true} if the download finished successfully, {@code false}
	 *         if the download was canelled using
	 *         {@link #cancelDownloadAndLaunch()}
	 * @throws MalformedURLException
	 *             If the specified repository {@link URL} is malformed
	 * @throws IOException
	 *             If the version info cannot be read
	 * @throws JDOMException
	 *             If the version xml is malformed
	 * 
	 */
	public boolean download(boolean isSnapshot, HidableUpdateProgressDialog gui)
			throws MalformedURLException, IOException, JDOMException {
		if (gui != null) {
			gui.preparePhaseStarted();
		}

		String destFolder = Common.getAppDataPath()
				+ Config.subfolderToSaveApps.replace("{appName}", this.getMavenArtifactID());
		String destFilename;
		Version destVersion;
		URL repoBaseURL;
		URL artifactURL;

		// Perform Cancel if requested
		if (cancelDownloadAndLaunch) {
			if (gui != null) {
				gui.operationCanceled();
			}
			return false;
		}

		if (isSnapshot) {
			// Snapshot
			destVersion = this.getLatestOnlineSnapshotVersion();
			repoBaseURL = this.getMavenSnapshotRepoBaseURL();
		} else {
			// Not a snapshot
			destVersion = this.getLatestOnlineVersion();
			repoBaseURL = this.getMavenRepoBaseURL();
		}

		// Construct the download url
		if (this.getMavenClassifier().equals("")) {
			artifactURL = new URL(repoBaseURL.toString() + "/" + this.mavenGroupID + "/" + this.getMavenArtifactID()
					+ "/" + destVersion.getVersion() + "/" + this.getMavenArtifactID() + "-" + destVersion.toString()
					+ ".jar");
		} else {
			artifactURL = new URL(repoBaseURL.toString() + "/" + this.getMavenGroupID() + "/"
					+ this.getMavenArtifactID() + "/" + destVersion.getVersion() + "/" + this.getMavenArtifactID() + "-"
					+ destVersion.toString() + "-" + this.getMavenClassifier() + ".jar");
		}

		// Construct file name of output file
		if (this.getMavenClassifier().equals("")) {
			// No classifier
			destFilename = this.getMavenArtifactID() + "-" + destVersion.toString() + ".jar";
		} else {
			destFilename = this.getMavenArtifactID() + "-" + destVersion.toString() + "-" + this.getMavenClassifier()
					+ ".jar";
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

		System.out.println("Downloading artifact from " + artifactURL.toString() + "...");
		System.out.println("Downloading to: " + outputFile.getAbsolutePath());
		FileUtils.copyURLToFile(artifactURL, outputFile);

		// download version info
		downloadVersionInfo(isSnapshot, destFolder);

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

	public void cancelDownloadAndLaunch() {
		cancelDownloadAndLaunch(null);
	}

	public void cancelDownloadAndLaunch(HidableUpdateProgressDialog gui) {
		cancelDownloadAndLaunch = true;
		System.out.println("Requested to cancel the current operation, Cancel in progress...");

		if (gui != null) {
			gui.cancelRequested();
		}
	}

	public static List<App> getAppList() throws MalformedURLException, JDOMException, IOException {
		Document doc = null;
		String fileName = Common.getAppDataPath() + File.separator + Config.appListCacheFileName;
		try {
			doc = new SAXBuilder().build(Config.getAppListXMLURL());
			
			(new XMLOutputter(Format.getPrettyFormat())).output(doc, new FileOutputStream(fileName));
		} catch (UnknownHostException e) {
			try {
				doc = new SAXBuilder().build(new File(fileName));
			} catch (FileNotFoundException e1) {
				throw new UnknownHostException("Could not connect to " + Config.getAppListXMLURL().toString() + " and app list cache not found. \nPlease ensure a stable internet connection.");
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

		List<App> res = new ArrayList<App>();

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
}

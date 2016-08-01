package applist;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import common.Config;
import common.Version;

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

	/**
	 * The version of the app that is currently installed
	 */
	private Version currentlyInstalledVersion;

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
			Document mavenMetadata = new SAXBuilder().build(new URL(this.getMavenRepoBaseURL().toString() + "/"
					+ mavenGroupID + "/" + mavenArtifactID + "/maven-metadata.xml"));

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
			Document mavenMetadata = new SAXBuilder().build(new URL(this.getMavenRepoBaseURL().toString() + "/"
					+ mavenGroupID + "/" + mavenArtifactID + "/maven-metadata.xml"));

			Version res = new Version(
					mavenMetadata.getRootElement().getChild("versioning").getChild("latest").getValue());

			Document snapshotMetadata = new SAXBuilder().build(new URL(this.getMavenSnapshotRepoBaseURL().toString()
					+ "/" + mavenGroupID + "/" + mavenArtifactID + res.getVersion() + "/maven-metadata.xml"));

			if (!res.isSnapshot()) {
				throw new IllegalStateException(
						"Latest version in this repository is a release and not a snapshot. This might happen if you host snapshots and releases in the same repository (which is not recommended). If you still need this case to be covered, please submit an issue at https://github.com/vatbub/fokLauncher/issues");
			}

			// get the buildnumber (its timestamp)
			res.setBuildNumber(snapshotMetadata.getRootElement().getChild("versioning").getChild("snapshot")
					.getChild("buildNumber").getValue());

			latestOnlineSnapshotVersion = res;
			return res;
		}
	}

	/**
	 * @return the currentlyInstalledVersion
	 */
	public Version getCurrentlyInstalledVersion() {
		if (currentlyInstalledVersion != null) {
			return currentlyInstalledVersion;
		} else {
			Version res = null;

			// TODO: Get the currently installed version once app download is
			// complete

			currentlyInstalledVersion = res;
			return res;
		}
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

	public static List<App> getAppList() throws MalformedURLException, JDOMException, IOException {
		Document doc = new SAXBuilder().build(new URL(Config.getAppListXMLURL().toString()));
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

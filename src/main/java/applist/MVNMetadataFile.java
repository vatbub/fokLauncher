package applist;

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


import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.VersionList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * In-memory representation of the metadata file of an app retrieved from the app's maven repository.
 */
public class MVNMetadataFile {
    private MVNCoordinates mvnCoordinates;
    private Version latest;
    private Version latestRelease;
    private VersionList versionList;
    private LocalDateTime lastUpdated;

    /**
     * Downloads the metadata for the given app.
     *
     * @param mvnCoordinates  The coordinates of the app to download the metadata for.
     * @param enableSnapshots If {@code true}, the corresponding snapshot metadata file will be downloaded instead of the main metadata file.
     * @throws JDOMException If the metadata file cannot be parsed.
     * @throws IOException   If the metadata file cannot be downloaded for any reason.
     */
    public MVNMetadataFile(MVNCoordinates mvnCoordinates, boolean enableSnapshots) throws JDOMException, IOException {
        setMvnCoordinates(mvnCoordinates);
        getFile(enableSnapshots);
    }

    private void getFile(boolean enableSnapshots) throws JDOMException, IOException {
        Document mavenMetadata = getMavenMetadata(enableSnapshots);

        Element versioningElement = mavenMetadata.getRootElement().getChild(FileFormat.VERSIONING_TAG_NAME);
        if (versioningElement.getChild(FileFormat.LATEST_VERSION_TAG_NAME) != null) {
            setLatest(new Version(versioningElement.getChild(FileFormat.LATEST_VERSION_TAG_NAME).getValue()));
            if (getLatest().isSnapshot()) {
                updateVersionWithSnapshotInfo(getLatest());
            }
        } /*else if (mavenMetadata.getRootElement().getChild(FileFormat.VERSION_TAG_NAME) != null) {
            setLatest(new Version(mavenMetadata.getRootElement().getChild(FileFormat.VERSION_TAG_NAME).getValue()));
            if (getLatest().isSnapshot()) {
                updateVersionWithSnapshotInfo(getLatest());
            }
        }*/
        if (versioningElement.getChild(FileFormat.LATEST_RELEASE_TAG_NAME) != null) {
            setLatestRelease(new Version(versioningElement.getChild(FileFormat.LATEST_RELEASE_TAG_NAME).getValue()));
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        setLastUpdated(LocalDateTime.from(f.parse(versioningElement.getChild(FileFormat.LAST_UPDATED_TAG_NAME).getValue())));

        VersionList res = new VersionList();
        List<Element> versions = versioningElement.getChild(FileFormat.VERSIONS_TAG_NAME)
                .getChildren(FileFormat.VERSION_TAG_NAME);

        for (Element versionElement : versions) {
            Version version = new Version(versionElement.getValue());
            if (enableSnapshots == version.isSnapshot()) {
                if (version.isSnapshot()) {
                    updateVersionWithSnapshotInfo(version);
                }
                res.add(version);
            }
        }

        setVersionList(res);
    }

    private void updateVersionWithSnapshotInfo(Version version) throws IOException, JDOMException {
        Document snapshotMetadata = new SAXBuilder()
                .build(new URL(this.getMvnCoordinates().getSnapshotRepoBaseURL().toString() + "/" + getMvnCoordinates().getGroupId().replace('.', '/')
                        + "/" + getMvnCoordinates().getArtifactId() + "/" + version.getVersion() + "/maven-metadata.xml"));
        Element snapshotVersioningElement = snapshotMetadata.getRootElement().getChild(SnapshotFileFormat.VERSIONING_TAG_NAME);
        Element latestSnapshot = snapshotVersioningElement.getChild(SnapshotFileFormat.LATEST_SNAPSHOT_TAG_NAME);
        version.setBuildNumber(latestSnapshot.getChild(SnapshotFileFormat.BUILD_NUMBER_TAG_NAME).getValue());
        version.setTimestamp(latestSnapshot.getChild(SnapshotFileFormat.TIMESTAMP_TAG_NAME).getValue());
    }

    /**
     * Gets the Maven Metadata file and converts it into a
     * {@code JDOM-}{@link Document}
     *
     * @param snapshotsEnabled {@code true} if snapshots shall be taken into account.
     * @return A {@link Document} representation of the maven Metadata file
     * @throws JDOMException If the maven metadata file is malformed
     * @throws IOException   If the maven metadata file cannot be downloaded
     */
    private Document getMavenMetadata(boolean snapshotsEnabled)
            throws JDOMException, IOException {
        String repoBaseURL;
        if (snapshotsEnabled) {
            repoBaseURL = getMvnCoordinates().getSnapshotRepoBaseURL().toString();
        } else {
            repoBaseURL = getMvnCoordinates().getRepoBaseURL().toString();
        }

        return new SAXBuilder().build(new URL(repoBaseURL + "/"
                + getMvnCoordinates().getGroupId().replace('.', '/') + "/" + getMvnCoordinates().getArtifactId() + "/maven-metadata.xml"));
    }

    /**
     * Returns the maven coordinates of this app.
     *
     * @return The maven coordinates of this app.
     */
    public MVNCoordinates getMvnCoordinates() {
        return mvnCoordinates;
    }

    /**
     * Sets the maven coordinates of this app.
     *
     * @param mvnCoordinates The maven coordinates to set
     */
    public void setMvnCoordinates(MVNCoordinates mvnCoordinates) {
        this.mvnCoordinates = mvnCoordinates;
    }

    /**
     * Returns the list of available online versions for this app.
     *
     * @return The list of available online versions for this app.
     */
    public VersionList getVersionList() {
        return versionList;
    }

    /**
     * Sets the list of available online versions for this app.
     *
     * @param versionList The list of available online versions to set
     */
    public void setVersionList(VersionList versionList) {
        this.versionList = versionList;
    }

    /**
     * Returns the latest available online version.
     *
     * @return The latest available online version.
     */
    public Version getLatest() {
        return latest;
    }

    /**
     * Sets the latest available online version.
     *
     * @param latest The latest available online version to set
     */
    public void setLatest(Version latest) {
        this.latest = latest;
    }

    /**
     * Returns the latest available online release version.
     *
     * @return The latest available online release version.
     */
    public Version getLatestRelease() {
        return latestRelease;
    }

    /**
     * Sets the latest available online release version.
     *
     * @param latestRelease The latest available online release version to set
     */
    public void setLatestRelease(Version latestRelease) {
        this.latestRelease = latestRelease;
    }

    /**
     * Returns the timestamp of the last app update
     *
     * @return The timestamp of the last app update
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the timestamp of the last app update
     *
     * @param lastUpdated The timestamp to set
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Describes the file format of a metadata file
     */
    public class FileFormat {
        public static final String VERSION_TAG_NAME = "version";
        public static final String VERSIONING_TAG_NAME = "versioning";
        public static final String LATEST_VERSION_TAG_NAME = "latest";
        public static final String LATEST_RELEASE_TAG_NAME = "release";
        public static final String VERSIONS_TAG_NAME = "versions";
        public static final String LAST_UPDATED_TAG_NAME = "lastUpdated";

        private FileFormat() {
            throw new IllegalStateException("Class may not be instantiated");
        }
    }

    /**
     * Describes the file format of a snapshot metadata file
     */
    public class SnapshotFileFormat {
        public static final String VERSIONING_TAG_NAME = "versioning";
        public static final String LATEST_SNAPSHOT_TAG_NAME = "snapshot";
        public static final String TIMESTAMP_TAG_NAME = "timestamp";
        public static final String BUILD_NUMBER_TAG_NAME = "buildNumber";

        private SnapshotFileFormat() {
            throw new IllegalStateException("Class may not be instantiated");
        }
    }
}

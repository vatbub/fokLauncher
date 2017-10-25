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

import java.net.URL;

/**
 * Represents the coordinates of a maven artifact (groupId, artifactId, classifier).
 * The artifacts version is not held in this class as it will be determined as required in the {@link App} class.
 */
public class MVNCoordinates {
    /**
     * Base URL of the maven repo where the artifact can be downloaded from.
     */
    private URL repoBaseURL;
    /**
     * The URL of the maven repo where snapshots of the artifact can be downloaded from.
     */
    private URL snapshotRepoBaseURL;
    /**
     * The artifacts group id.
     */
    private String groupId;
    /**
     * The artifacts artifact id
     */
    private String artifactId;
    /**
     * The artifacts classifier or {@code null} if the default artifact shall be used.
     */
    private String classifier;

    /**
     * Creates an empty instance.
     */
    public MVNCoordinates() {
        this(null, null);
    }

    public MVNCoordinates(String groupId, String artifactId) {
        this(null, null, groupId, artifactId);
    }

    /**
     * @param repoBaseURL         Base URL of the maven repo where the artifact can be downloaded from.
     * @param snapshotRepoBaseURL The URL of the maven repo where snapshots of the artifact can be downloaded from.
     * @param groupId             The artifacts group id.
     * @param artifactId          The artifacts artifact id
     */
    public MVNCoordinates(URL repoBaseURL, URL snapshotRepoBaseURL, String groupId, String artifactId) {
        this(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, null);
    }

    /**
     * @param repoBaseURL         Base URL of the maven repo where the artifact can be downloaded from.
     * @param snapshotRepoBaseURL The URL of the maven repo where snapshots of the artifact can be downloaded from.
     * @param groupId             The artifacts group id.
     * @param artifactId          The artifacts artifact id
     * @param classifier          The artifacts classifier or {@code null} if the default artifact shall be used.
     */
    public MVNCoordinates(URL repoBaseURL, URL snapshotRepoBaseURL, String groupId, String artifactId, String classifier) {
        setRepoBaseURL(repoBaseURL);
        setSnapshotRepoBaseURL(snapshotRepoBaseURL);
        setGroupId(groupId);
        setArtifactId(artifactId);
        setClassifier(classifier);
    }

    /**
     * Returns the base URL of the maven repo where the artifact can be downloaded from.
     *
     * @return The base URL of the maven repo where the artifact can be downloaded from.
     */
    public URL getRepoBaseURL() {
        return repoBaseURL;
    }

    /**
     * Sets the base URL of the maven repo where the artifact can be downloaded from.
     *
     * @param repoBaseURL The base URL of the maven repo where the artifact can be downloaded from.
     */
    public void setRepoBaseURL(URL repoBaseURL) {
        this.repoBaseURL = repoBaseURL;
    }

    /**
     * Returns the URL of the maven repo where snapshots of the artifact can be downloaded from.
     *
     * @return The URL of the maven repo where snapshots of the artifact can be downloaded from.
     */
    public URL getSnapshotRepoBaseURL() {
        return snapshotRepoBaseURL;
    }

    /**
     * Sets the URL of the maven repo where snapshots of the artifact can be downloaded from.
     *
     * @param snapshotRepoBaseURL The URL of the maven repo where snapshots of the artifact can be downloaded from.
     */
    public void setSnapshotRepoBaseURL(URL snapshotRepoBaseURL) {
        this.snapshotRepoBaseURL = snapshotRepoBaseURL;
    }

    /**
     * Returns the artifacts group id.
     *
     * @return The artifacts group id.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the artifacts group id.
     *
     * @param groupId The artifacts group id.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Returns the artifacts artifact id
     *
     * @return The artifacts artifact id
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Sets the artifacts artifact id
     *
     * @param artifactId The artifacts artifact id
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Returns the artifacts classifier or {@code null} if the default artifact shall be used.
     *
     * @return The artifacts classifier or {@code null} if the default artifact shall be used.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Sets the artifacts classifier or {@code null} if the default artifact shall be used.
     *
     * @param classifier The artifacts classifier or {@code null} if the default artifact shall be used.
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * Returns the name of the jar file for this artifact according to the standard maven naming convention.
     * If the artifact uses a different naming scheme (defined in the artifact's pom under {@code build -> finalName}, this method will not return the correct jar file name.
     *
     * @param version The version of the artifact to generate the name for.
     * @return The name of the jar file for this artifact according to the standard maven naming convention.
     */
    public String getJarFileName(Version version) {
        // Construct file name of output file
        StringBuilder destFilenameBuilder = new StringBuilder(getArtifactId())
                .append("-")
                .append(version.toString(false));
        if (getClassifier() != null) {
            destFilenameBuilder.append("-").append(getClassifier());
        }
        destFilenameBuilder.append(".jar");
        return destFilenameBuilder.toString();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(Version version) {
        String versionText = "<version>";
        if (version != null) {
            versionText = version.toString(false);
        }
        StringBuilder res = new StringBuilder(getGroupId()).append(":").append(getArtifactId()).append(":").append(versionText).append(":").append(":jar");
        if (getClassifier() != null) {
            res.append(":").append(getClassifier());
        }
        return res.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MVNCoordinates)) {
            return false;
        }

        MVNCoordinates that = (MVNCoordinates) obj;
        return this.getGroupId().equals(that.getGroupId()) &&
                this.getArtifactId().equals(that.getArtifactId()) &&
                this.getClassifier().equals(that.getClassifier()) &&
                this.getRepoBaseURL().equals(that.getRepoBaseURL()) &&
                this.getSnapshotRepoBaseURL().equals(that.getSnapshotRepoBaseURL());
    }
}

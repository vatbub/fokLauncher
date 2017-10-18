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


import java.net.URL;

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

    public MVNCoordinates() {
        this(null, null, null, null);
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

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(getGroupId()).append(":").append(getArtifactId()).append(":<version>:").append(":jar");
        if (getClassifier() != null) {
            res.append(":").append(getClassifier());
        }
        return res.toString();
    }
}

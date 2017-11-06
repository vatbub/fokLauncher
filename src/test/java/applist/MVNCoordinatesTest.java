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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class MVNCoordinatesTest {
    private static final String groupId = "com.vatbub.github";
    private static final String artifactId = "sampleArtifact";
    private static final String classifier = "jar-with-dependencies";
    private static final Version snapshotVersion = new Version("0.0.1-SNAPSHOT");
    private static final Version releaseVersion = new Version("0.0.2");
    private static URL repoBaseURL;
    private static URL snapshotRepoBaseURL;

    @BeforeClass
    public static void oneTimeSetUp() throws MalformedURLException {
        repoBaseURL = new URL("https://dl.bintray.com/vatbub/fokprojectsSnapshots");
        snapshotRepoBaseURL = new URL("https://oss.jfrog.org/artifactory/libs-snapshot");
    }

    @Test
    public void defaultConstructorTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates();
        Assert.assertNull(mvnCoordinates.getGroupId());
        Assert.assertNull(mvnCoordinates.getArtifactId());
    }

    @Test
    public void groupAndArtifactIdOnlyConstructorTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(groupId, artifactId);
        Assert.assertEquals(groupId, mvnCoordinates.getGroupId());
        Assert.assertEquals(artifactId, mvnCoordinates.getArtifactId());
        Assert.assertNull(mvnCoordinates.getRepoBaseURL());
        Assert.assertNull(mvnCoordinates.getSnapshotRepoBaseURL());
    }

    @Test
    public void noClassifierConstructorTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId);
        Assert.assertEquals(groupId, mvnCoordinates.getGroupId());
        Assert.assertEquals(artifactId, mvnCoordinates.getArtifactId());
        Assert.assertEquals(repoBaseURL, mvnCoordinates.getRepoBaseURL());
        Assert.assertEquals(snapshotRepoBaseURL, mvnCoordinates.getSnapshotRepoBaseURL());
    }

    @Test
    public void classifierConstructorTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        Assert.assertEquals(groupId, mvnCoordinates.getGroupId());
        Assert.assertEquals(artifactId, mvnCoordinates.getArtifactId());
        Assert.assertEquals(classifier, mvnCoordinates.getClassifier());
        Assert.assertEquals(repoBaseURL, mvnCoordinates.getRepoBaseURL());
        Assert.assertEquals(snapshotRepoBaseURL, mvnCoordinates.getSnapshotRepoBaseURL());
    }

    @Test
    public void jarFileNameNoClassifierTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(groupId, artifactId);
        Assert.assertEquals(artifactId + "-" + snapshotVersion.toString(false) + ".jar", mvnCoordinates.getJarFileName(snapshotVersion));
        Assert.assertEquals(artifactId + "-" + releaseVersion.toString(false) + ".jar", mvnCoordinates.getJarFileName(releaseVersion));
    }

    @Test
    public void jarFileNameWithClassifierTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        Assert.assertEquals(artifactId + "-" + snapshotVersion.toString(false) + "-" + classifier + ".jar", mvnCoordinates.getJarFileName(snapshotVersion));
        Assert.assertEquals(artifactId + "-" + releaseVersion.toString(false) + "-" + classifier + ".jar", mvnCoordinates.getJarFileName(releaseVersion));
    }

    @Test
    public void toStringNoClassifierNoVersionTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(groupId, artifactId);
        assertToString(mvnCoordinates.toString(), null, false);
    }

    @Test
    public void toStringNoClassifierWithVersionTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(groupId, artifactId);
        assertToString(mvnCoordinates.toString(snapshotVersion), snapshotVersion, false);
        assertToString(mvnCoordinates.toString(releaseVersion), releaseVersion, false);
    }

    @Test
    public void toStringWithClassifierNoVersionTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        assertToString(mvnCoordinates.toString(), null, false);
    }

    @Test
    public void toStringWithClassifierWithVersionTest() {
        MVNCoordinates mvnCoordinates = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        assertToString(mvnCoordinates.toString(snapshotVersion), snapshotVersion, false);
        assertToString(mvnCoordinates.toString(releaseVersion), releaseVersion, false);
    }

    @Test
    public void equalsTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        MVNCoordinates mvnCoordinates2 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        Assert.assertTrue(mvnCoordinates1.equals(mvnCoordinates2));
    }

    @Test
    public void equalsUnequalRepoBaseURLTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        MVNCoordinates mvnCoordinates2 = new MVNCoordinates(snapshotRepoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        Assert.assertFalse(mvnCoordinates1.equals(mvnCoordinates2));
    }

    @Test
    public void equalsUnequalSnapshotRepoBaseURLTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        MVNCoordinates mvnCoordinates2 = new MVNCoordinates(repoBaseURL, repoBaseURL, groupId, artifactId, classifier);
        Assert.assertFalse(mvnCoordinates1.equals(mvnCoordinates2));
    }

    @Test
    public void equalsUnequalGroupIdTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        MVNCoordinates mvnCoordinates2 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId + "ttt", artifactId, classifier);
        Assert.assertFalse(mvnCoordinates1.equals(mvnCoordinates2));
    }

    @Test
    public void equalsUnequalArtifactIdTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        MVNCoordinates mvnCoordinates2 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId + "ttt", classifier);
        Assert.assertFalse(mvnCoordinates1.equals(mvnCoordinates2));
    }

    @Test
    public void equalsUnequalClassifierTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        MVNCoordinates mvnCoordinates2 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId);
        Assert.assertFalse(mvnCoordinates1.equals(mvnCoordinates2));
    }

    @Test
    public void equalsWithDifferentClassTest() {
        MVNCoordinates mvnCoordinates1 = new MVNCoordinates(repoBaseURL, snapshotRepoBaseURL, groupId, artifactId, classifier);
        //noinspection EqualsBetweenInconvertibleTypes
        Assert.assertFalse(mvnCoordinates1.equals("testString"));
    }

    private void assertToString(String actual, Version version, boolean testClassifier) {
        String versionText = "<version>";
        if (version != null) {
            versionText = version.toString(false);
        }

        Assert.assertTrue(actual.contains(groupId));
        Assert.assertTrue(actual.contains(artifactId));
        Assert.assertTrue(actual.contains(versionText));
        Assert.assertTrue(actual.contains("jar"));

        if (testClassifier) {
            Assert.assertTrue(actual.contains(classifier));
        }
    }
}

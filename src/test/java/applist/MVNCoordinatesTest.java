package applist;

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

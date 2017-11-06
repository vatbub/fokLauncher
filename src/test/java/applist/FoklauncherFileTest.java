package applist;

import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static applist.FoklauncherFile.Property.*;

public class FoklauncherFileTest {
    private static final String name = "Zork (prerelease)";
    private static final String groupId = "com.github.vatbub";
    private static final String classifier = "jar-with-dependencies";
    private static final String artifactId = "zorkClone";
    private static URL changelogURL;
    private static URL repoBaseURL;
    private static URL additionalInfoURL;
    private static URL snapshotRepoBaseURL;
    private File testFile;

    private static String getFileContent(boolean includeClassifier) {
        String res = "#This file stores info about a java app. To open this file, get the foklauncher\n" +
                "#Fri Dec 02 01:12:18 CET 2016\n" +
                "changelogURL=" + changelogURL.toString().replace(":", "\\:") + "\n" +
                "name=" + name + "\n" +
                "groupId=" + groupId + "\n" +
                "repoBaseURL=" + repoBaseURL.toString().replace(":", "\\:") + "\n" +
                "additionalInfoURL=" + additionalInfoURL.toString().replace(":", "\\:") + "\n" +
                "snapshotRepoBaseURL=" + snapshotRepoBaseURL.toString().replace(":", "\\:") + "\n" +
                "artifactId=" + artifactId + "\n";

        if (includeClassifier) {
            res = res + "classifier=" + classifier + "\n";
        }

        return res;
    }

    @BeforeClass
    public static void initPrivateVars() throws MalformedURLException {
        Common.getInstance().setAppName("fokprojectUnitTests");
        changelogURL = new URL("https://github.com/vatbub/zorkClone/blob/master/CHANGELOG.md");
        repoBaseURL = new URL("https://dl.bintray.com/vatbub/fokprojectsReleases");
        additionalInfoURL = new URL("https://github.com/vatbub/zorkClone#zorkclone-");
        snapshotRepoBaseURL = new URL("https://oss.jfrog.org/artifactory/libs-snapshot");
    }

    @After
    public void cleanUp() throws IOException {
        if (testFile != null && testFile.exists()) {
            Files.delete(testFile.toPath());
            testFile = null;
        }
    }

    @Test
    public void readFileTest() throws IOException {
        testFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("testFile.foklauncher").toFile();
        FileUtils.writeStringToFile(testFile, getFileContent(true), Charset.forName("UTF-8"));
        FoklauncherFile foklauncherFile = new FoklauncherFile(testFile);
        Assert.assertEquals(testFile, foklauncherFile.getSourceFile());
        Assert.assertEquals(name, foklauncherFile.getValue(NAME));
        Assert.assertEquals(groupId, foklauncherFile.getValue(GROUP_ID));
        Assert.assertEquals(classifier, foklauncherFile.getValue(CLASSIFIER));
        Assert.assertEquals(artifactId, foklauncherFile.getValue(ARTIFACT_ID));
        Assert.assertEquals(changelogURL.toString(), foklauncherFile.getValue(CHANGELOG_URL));
        Assert.assertEquals(repoBaseURL.toString(), foklauncherFile.getValue(REPO_BASE_URL));
        Assert.assertEquals(additionalInfoURL.toString(), foklauncherFile.getValue(ADDITIONAL_INFO_URL));
        Assert.assertEquals(snapshotRepoBaseURL.toString(), foklauncherFile.getValue(SNAPSHOT_BASE_URL));
    }

    @Test
    public void readNonExistentValueTest() throws IOException {
        String defaultValue = "defaultValue";
        testFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("testFile.foklauncher").toFile();
        FileUtils.writeStringToFile(testFile, getFileContent(false), Charset.forName("UTF-8"));
        FoklauncherFile foklauncherFile = new FoklauncherFile(testFile);
        Assert.assertNull(foklauncherFile.getValue(CLASSIFIER));
        Assert.assertEquals(defaultValue, foklauncherFile.getValue(CLASSIFIER, defaultValue));
    }

    @Test
    public void readNonExistentFileTest() throws IOException {
        testFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("testFile.foklauncher").toFile();
        if (testFile.exists()) {
            Files.delete(testFile.toPath());
        }

        FoklauncherFile foklauncherFile = new FoklauncherFile(testFile);
        Assert.assertEquals(testFile, foklauncherFile.getSourceFile());
        Assert.assertNull(foklauncherFile.getValue(NAME));
        Assert.assertNull(foklauncherFile.getValue(GROUP_ID));
        Assert.assertNull(foklauncherFile.getValue(CLASSIFIER));
        Assert.assertNull(foklauncherFile.getValue(ARTIFACT_ID));
        Assert.assertNull(foklauncherFile.getValue(CHANGELOG_URL));
        Assert.assertNull(foklauncherFile.getValue(REPO_BASE_URL));
        Assert.assertNull(foklauncherFile.getValue(ADDITIONAL_INFO_URL));
        Assert.assertNull(foklauncherFile.getValue(SNAPSHOT_BASE_URL));
    }

    @Test
    public void readDirectoryTest() {
        testFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("testDirectory").toFile();
        if (!testFile.mkdir()) {
            throw new IllegalStateException("Unable to create the test directory");
        }
        if (!testFile.isDirectory()) {
            throw new IllegalStateException("Test directory is supposed to be a directory");
        }

        FoklauncherFile foklauncherFile = null;
        try {
            foklauncherFile = new FoklauncherFile(testFile);
            Assert.fail("IOException expected");
        } catch (IOException e) {
            Assert.assertNull(foklauncherFile);
        }
    }

    @Test
    public void saveFileTest() throws IOException {
        testFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("testFile.foklauncher").toFile();
        FoklauncherFile foklauncherFile = new FoklauncherFile(testFile);
        Map<FoklauncherFile.Property, String> values = new HashMap<>();
        values.put(NAME, name);
        values.put(GROUP_ID, groupId);
        values.put(CLASSIFIER, classifier);
        values.put(ARTIFACT_ID, artifactId);
        values.put(CHANGELOG_URL, changelogURL.toString());
        values.put(REPO_BASE_URL, repoBaseURL.toString());
        values.put(ADDITIONAL_INFO_URL, additionalInfoURL.toString());
        values.put(SNAPSHOT_BASE_URL, snapshotRepoBaseURL.toString());

        for (Map.Entry<FoklauncherFile.Property, String> value : values.entrySet()) {
            foklauncherFile.setValue(value.getKey(), value.getValue());
        }

        foklauncherFile.save();

        String result = FileUtils.readFileToString(testFile, "UTF-8");
        result = result.replace("\\", "");

        FOKLogger.info(FoklauncherFileTest.class.getName(), "Test file contents:\n" + result);

        for (Map.Entry<FoklauncherFile.Property, String> value : values.entrySet()) {
            FOKLogger.info(FoklauncherFileTest.class.getName(), "Testing: " + value.getKey().toString() + " -> " + value.getValue());
            Assert.assertTrue(result.contains(value.getKey().toString()));
            Assert.assertTrue(result.contains(value.getValue()));
        }
    }

    /*@Test
    public void noReadPermissionTest() throws IOException {
        testFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("readOnlyFile.foklauncher").toFile();
        // testFile = new File("C:\\Users\\Frederik\\Documents\\zorkclone_foklauncher_info\\readOnlyFile.foklauncher");
        FileUtils.writeStringToFile(testFile, getFileContent(true), Charset.forName("UTF-8"));
        if (!testFile.setReadable(false, false)){
            throw new IllegalStateException("Unable to make the test file write only");
        }

        FoklauncherFile foklauncherFile = null;
        try {
            foklauncherFile = new FoklauncherFile(testFile);
            Assert.fail("IOException expected");
        } catch (IOException e) {
            Assert.assertNull(foklauncherFile);
        }

        if (!testFile.setReadable(true, false)){
            throw new IllegalStateException("Unable to reallow reading of the test file");
        }
    }*/
}

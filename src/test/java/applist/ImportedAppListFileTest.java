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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ImportedAppListFileTest {
    @BeforeClass
    public static void oneTimeSetUp() {
        Common.getInstance().setAppName("fokprojectUnitTests");
    }

    @After
    public void cleanAppData() {
        if (Common.getInstance().getAppDataPathAsFile().exists()) {
            try {
                FileUtils.deleteDirectory(Common.getInstance().getAppDataPathAsFile());
            } catch (IOException e) {
                FOKLogger.log(ImportedAppListFileTest.class.getName(), Level.INFO, "Unable to delete the test folder, ignoring that...", e);
            }
        }
    }

    @Test
    public void readFileTest() throws IOException {
        List<MVNCoordinates> mvnCoordinatesList = new ArrayList<>(2);
        List<String> names = new ArrayList<>(2);
        List<URL> changelogURLs = new ArrayList<>(2);
        List<URL> additionalInfoURLs = new ArrayList<>(2);

        mvnCoordinatesList.add(new MVNCoordinates(new URL("https://dl.bintray.com/vatbub/fokprojectsReleases"), new URL("https://oss.jfrog.org/artifactory/libs-snapshot"), "com.github.vatbub", "zorkClone", "jar-with-dependencies"));
        names.add("Zork (prerelease)");
        changelogURLs.add(new URL("https://github.com/vatbub/zorkClone/blob/master/CHANGELOG.md"));
        additionalInfoURLs.add(new URL("https://github.com/vatbub/zorkClone#zorkclone-"));

        mvnCoordinatesList.add(new MVNCoordinates(new URL("https://dl.bintray.com/vatbub/fokprojectsReleases"), new URL("https://oss.jfrog.org/artifactory/libs-snapshot"), "com.github.vatbub", "zorkClone", "gameEditor"));
        names.add("Zork GameEditor (prerelease)");
        changelogURLs.add(new URL("https://github.com/vatbub/zorkClone/blob/master/CHANGELOG.md"));
        additionalInfoURLs.add(new URL("https://github.com/vatbub/zorkClone#zorkclone-"));

        List<FoklauncherFile> foklauncherFiles = generateFokLauncherFiles(mvnCoordinatesList, names, changelogURLs, additionalInfoURLs);
        AppList expectedApps = new AppList();
        for (FoklauncherFile foklauncherFile : foklauncherFiles) {
            expectedApps.add(new App(foklauncherFile.getSourceFile()));
        }

        File underlyingFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("importedAppsTestFile.xml").toFile();
        FileUtils.writeStringToFile(underlyingFile, getFileContent(foklauncherFiles), Charset.forName("UTF-8"));

        ImportedAppListFile importedAppListFile = new ImportedAppListFile(underlyingFile.getAbsolutePath());
        Assert.assertEquals(underlyingFile.getAbsolutePath(), importedAppListFile.getFileName());
        for (App app : importedAppListFile.getAppList()) {
            Assert.assertTrue(expectedApps.contains(app));
        }
    }

    private List<FoklauncherFile> generateFokLauncherFiles(List<MVNCoordinates> mvnCoordinatesList, List<String> names, List<URL> changelogURLs, List<URL> additionalInfoURLs) throws IOException {
        List<FoklauncherFile> res = new ArrayList<>(mvnCoordinatesList.size());
        for (int i = 0; i < mvnCoordinatesList.size(); i++) {
            String fileSuffix = "";
            if (mvnCoordinatesList.get(i).getClassifier()!=null){
                fileSuffix = "_" + mvnCoordinatesList.get(i).getClassifier();
            }
            FoklauncherFile foklauncherFile = new FoklauncherFile(Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve(mvnCoordinatesList.get(i).getGroupId() + "_" + mvnCoordinatesList.get(i).getArtifactId() + fileSuffix + ".foklauncher").toFile());
            foklauncherFile.setValue(FoklauncherFile.Property.ARTIFACT_ID, mvnCoordinatesList.get(i).getArtifactId());
            foklauncherFile.setValue(FoklauncherFile.Property.GROUP_ID, mvnCoordinatesList.get(i).getGroupId());
            foklauncherFile.setValue(FoklauncherFile.Property.NAME, names.get(i));
            if (changelogURLs.get(i) != null)
                foklauncherFile.setValue(FoklauncherFile.Property.CHANGELOG_URL, changelogURLs.get(i).toString());
            if (additionalInfoURLs.get(i) != null)
                foklauncherFile.setValue(FoklauncherFile.Property.ADDITIONAL_INFO_URL, additionalInfoURLs.get(i).toString());
            if (mvnCoordinatesList.get(i).getClassifier() != null)
                foklauncherFile.setValue(FoklauncherFile.Property.CLASSIFIER, mvnCoordinatesList.get(i).getClassifier());
            foklauncherFile.setValue(FoklauncherFile.Property.SNAPSHOT_BASE_URL, mvnCoordinatesList.get(i).getSnapshotRepoBaseURL().toString());
            foklauncherFile.setValue(FoklauncherFile.Property.REPO_BASE_URL, mvnCoordinatesList.get(i).getRepoBaseURL().toString());

            foklauncherFile.save();
            res.add(foklauncherFile);
        }

        return res;
    }

    private String getFileContent(List<FoklauncherFile> foklauncherFiles) {
        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<fokLauncher>\n")
                .append("  <modelVersion />\n")
                .append("  <importedApps>\n");

        for (FoklauncherFile foklauncherFile : foklauncherFiles) {
            res.append("    <app>\n")
                    .append("      <fileName>").append(foklauncherFile.getSourceFile().getAbsolutePath()).append("</fileName>\n")
                    .append("    </app>\n");
        }

        res.append("  </importedApps>\n")
                .append("</fokLauncher>\n");
        return res.toString();
    }
}

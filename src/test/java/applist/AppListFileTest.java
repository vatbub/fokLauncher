package applist;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.vatbub.common.core.Common;
import config.AppConfig;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class AppListFileTest {
    private final static String onlineModelVersion = "0.0.1_online";
    private final static String offlineModelVersion = "0.0.1_offline";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    private String offlineCacheFileName;
    private URL testOnlineURL;
    private AppList expectedApps;

    @Before
    public void setUp() throws IOException {
        Common.resetInstance();
        Common.getInstance().setAppName("fokprojectUnitTests");
        offlineCacheFileName = Common.getInstance().getAndCreateAppDataPath() + AppConfig.getRemoteConfig().getValue("appListCacheFileName");

        AppConfig.getSupportedFOKConfigModelVersion().add(onlineModelVersion);
        AppConfig.getSupportedFOKConfigModelVersion().add(offlineModelVersion);

        expectedApps = new AppList();
        expectedApps.add(new App("Hangman Solver", new MVNCoordinates(new URL("https://dl.bintray.com/vatbub/fokprojectsReleases"), new URL("https://oss.jfrog.org/artifactory/libs-snapshot"), "com.github.vatbub", "hangmanSolver", "jar-with-dependencies"), new URL("https://github.com/vatbub/hangman-solver#hangman-solver"), new URL("https://github.com/vatbub/hangman-solver/blob/master/CHANGELOG.md")));
        expectedApps.add(new App("Hangman Solver (Versions 0.0.14 and older)", new MVNCoordinates(new URL("https://dl.bintray.com/vatbub/fokprojectsSnapshots"), new URL("https://oss.jfrog.org/artifactory/libs-snapshot"), "fokprojects", "hangmanSolver", "jar-with-dependencies"), new URL("https://github.com/vatbub/hangman-solver#hangman-solver"), new URL("https://github.com/vatbub/hangman-solver/blob/master/CHANGELOG.md")));
        expectedApps.add(new App("Tic Tac Toe (old version)", new MVNCoordinates(new URL("https://dl.bintray.com/vatbub/fokprojectsSnapshots"), new URL("https://oss.jfrog.org/artifactory/libs-snapshot"), "fokprojects", "tictactoe", "jar-with-dependencies")));
        expectedApps.add(new App("Tic Tac Toe NEW VERSION (prerelease)", new MVNCoordinates(new URL("https://dl.bintray.com/vatbub/fokprojectsReleases"), new URL("https://oss.jfrog.org/artifactory/libs-snapshot"), "com.github.vatbub", "tictactoe", "jar-with-dependencies"), new URL("https://github.com/vatbub/tictactoe#tictactoe"), new URL("https://github.com/vatbub/tictactoe/blob/master/CHANGELOG.md")));

        FileUtils.writeStringToFile(new File(offlineCacheFileName), generateAppListContent(expectedApps, offlineModelVersion), Charset.forName("UTF-8"));

        stubFor(get(urlEqualTo("/fokprojectsOnLauncher.xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(generateAppListContent(expectedApps, onlineModelVersion))));
        testOnlineURL = new URL("http://localhost:8089/fokprojectsOnLauncher.xml");
    }

    private String generateAppListContent(AppList apps, String modelVersion) {
        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<fokLauncher>\n")
                .append("  <modelVersion>").append(modelVersion).append("</modelVersion>\n")
                .append("  <apps>\n");

        for (App app : apps) {
            res.append("    <app>\n")
                    .append("      <name>").append(app.getName()).append("</name>\n")
                    .append("      <repoBaseURL>").append(app.getMvnCoordinates().getRepoBaseURL().toString()).append("</repoBaseURL>\n")
                    .append("      <snapshotRepoBaseURL>").append(app.getMvnCoordinates().getSnapshotRepoBaseURL().toString()).append("</snapshotRepoBaseURL>\n")
                    .append("      <groupId>").append(app.getMvnCoordinates().getGroupId()).append("</groupId>\n")
                    .append("      <artifactId>").append(app.getMvnCoordinates().getArtifactId()).append("</artifactId>\n");
            if (app.getMvnCoordinates().getClassifier() != null) {
                res.append("      <classifier>").append(app.getMvnCoordinates().getClassifier()).append("</classifier>\n");
            }
            if (app.getAdditionalInfoURL() != null) {
                res.append("      <additionalInfoURL>").append(app.getAdditionalInfoURL().toString()).append("</additionalInfoURL>\n");
            }
            if (app.getChangelogURL() != null) {
                res.append("      <changelogURL>").append(app.getChangelogURL().toString()).append("</changelogURL>\n");
            }
            res.append("    </app>\n");
        }

        res.append("  </apps>\n")
                .append("</fokLauncher>\n");

        return res.toString();
    }

    @Test
    public void defaultConstructorOnlineTest() throws JDOMException, IOException {
        assertAppListFile(new AppListFile(testOnlineURL, offlineCacheFileName, false), onlineModelVersion);
    }

    private void assertAppListFile(AppListFile appListFile, String modelVersion) {
        for (App app : expectedApps) {
            Assert.assertTrue(appListFile.getAppList().contains(app));
        }
        for (App app : appListFile.getAppList()) {
            Assert.assertTrue(expectedApps.contains(app));
        }

        Assert.assertEquals(modelVersion, appListFile.getModelVersion());
    }

    @Test
    public void noInternetTest() throws JDOMException, IOException {
        wireMockRule.stop();
        assertAppListFile(new AppListFile(testOnlineURL, offlineCacheFileName, false), offlineModelVersion);
    }

    @Test
    public void defaultConstructorOfflineTest() throws JDOMException, IOException {
        assertAppListFile(new AppListFile(testOnlineURL, offlineCacheFileName, true), offlineModelVersion);
    }
}

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


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import config.AppConfig;
import config.TestSuperClass;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.logging.Level;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class AppListFileTest extends TestSuperClass {
    private final static String onlineModelVersion = "0.0.1_online";
    private final static String offlineModelVersion = "0.0.1_offline";
    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(8089);
    private String offlineCacheFileName;
    private URL testOnlineURL;
    private AppList expectedApps;

    private void setModelVersionsUp() {
        AppConfig.getInstance().getSupportedFOKConfigModelVersion().add(onlineModelVersion);
        AppConfig.getInstance().getSupportedFOKConfigModelVersion().add(offlineModelVersion);
    }

    @Before
    public void setURLsUp() throws MalformedURLException {
        offlineCacheFileName = Common.getInstance().getAndCreateAppDataPath() + AppConfig.getInstance().getRemoteConfig().getValue("appListCacheFileName");
        testOnlineURL = new URL("http://localhost:8089/fokprojectsOnLauncher.xml");
    }

    private void setFilesUp() throws IOException {
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
        setFilesUp();
        setModelVersionsUp();
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
        setFilesUp();
        wireMockRule.stop();
        setModelVersionsUp();
        assertAppListFile(new AppListFile(testOnlineURL, offlineCacheFileName, false), offlineModelVersion);
    }

    @Test
    public void defaultConstructorOfflineTest() throws JDOMException, IOException {
        setFilesUp();
        setModelVersionsUp();
        assertAppListFile(new AppListFile(testOnlineURL, offlineCacheFileName, true), offlineModelVersion);
    }

    @Test
    public void illegalModelVersionTest() throws IOException {
        setFilesUp();
        try {
            new AppListFile(testOnlineURL, offlineCacheFileName, false);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            FOKLogger.log(AppListFileTest.class.getName(), Level.INFO, "Expected exception", e);
        } catch (Exception e) {
            FOKLogger.log(AppListFileTest.class.getName(), Level.SEVERE, "Unexpected exception", e);
            Assert.fail("Unexpected exception: " + e.getClass().getName());
        }
    }

    @Test
    public void noInternetAndNoCacheFileTest() throws IOException {
        wireMockRule.stop();
        try {
            new AppListFile(testOnlineURL, offlineCacheFileName, false);
            Assert.fail("IllegalStateException expected");
        } catch (UnknownHostException e) {
            FOKLogger.log(AppListFileTest.class.getName(), Level.INFO, "Expected exception", e);
        } catch (Exception e) {
            FOKLogger.log(AppListFileTest.class.getName(), Level.SEVERE, "Unexpected exception", e);
            Assert.fail("Unexpected exception: " + e.getClass().getName());
        }
    }
}

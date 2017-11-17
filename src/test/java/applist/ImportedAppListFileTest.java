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


import com.github.vatbub.common.core.Common;
import config.AppConfig;
import config.TestSuperClass;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ImportedAppListFileTest extends TestSuperClass {

    @Test
    public void readFileTest() throws IOException {
        List<FoklauncherFile> foklauncherFiles = generateFokLauncherFiles();
        AppList expectedApps = getAppListFromFoklauncherFileList(foklauncherFiles);

        File underlyingFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("importedAppsTestFile.xml").toFile();
        FileUtils.writeStringToFile(underlyingFile, getFileContent(foklauncherFiles, false, false), Charset.forName("UTF-8"));

        ImportedAppListFile importedAppListFile = new ImportedAppListFile(underlyingFile.getAbsolutePath());
        Assert.assertEquals(underlyingFile.getAbsolutePath(), importedAppListFile.getFileName());
        for (App app : importedAppListFile.getAppList()) {
            Assert.assertTrue(expectedApps.contains(app));
        }
    }

    @Test
    public void saveTest() throws IOException {
        List<FoklauncherFile> foklauncherFiles = generateFokLauncherFiles();
        AppList expectedApps = getAppListFromFoklauncherFileList(foklauncherFiles);

        File underlyingFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("importedAppsTestFile.xml").toFile();
        ImportedAppListFile importedAppListFile = new ImportedAppListFile(underlyingFile.getAbsolutePath());
        importedAppListFile.setAppList(expectedApps);
        importedAppListFile.saveFile();

        String result = FileUtils.readFileToString(underlyingFile, Charset.forName("UTF-8"));
        for (FoklauncherFile foklauncherFile : foklauncherFiles) {
            Assert.assertThat(result, CoreMatchers.containsString(foklauncherFile.getSourceFile().getAbsolutePath()));
        }
    }

    @Test
    public void noModelVersionTest() throws IOException {
        List<FoklauncherFile> foklauncherFiles = generateFokLauncherFiles();

        File underlyingFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("importedAppsTestFile.xml").toFile();
        FileUtils.writeStringToFile(underlyingFile, getFileContent(foklauncherFiles, true, false), Charset.forName("UTF-8"));

        ImportedAppListFile importedAppListFile = new ImportedAppListFile(underlyingFile.getAbsolutePath());

        Assert.assertNotNull(importedAppListFile.getAppList());
        Assert.assertEquals(0, importedAppListFile.getAppList().size());
    }

    @Test
    public void noAppListTest() throws IOException {
        List<FoklauncherFile> foklauncherFiles = generateFokLauncherFiles();

        File underlyingFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve("importedAppsTestFile.xml").toFile();
        FileUtils.writeStringToFile(underlyingFile, getFileContent(foklauncherFiles, false, true), Charset.forName("UTF-8"));

        ImportedAppListFile importedAppListFile = new ImportedAppListFile(underlyingFile.getAbsolutePath());

        Assert.assertNotNull(importedAppListFile.getAppList());
        Assert.assertEquals(0, importedAppListFile.getAppList().size());
    }

    @Test
    public void defaultConstructorTest() throws IOException {
        // AppConfig.getRemoteConfig().getValue("importedAppListFileName")
        List<FoklauncherFile> foklauncherFiles = generateFokLauncherFiles();
        AppList expectedApps = getAppListFromFoklauncherFileList(foklauncherFiles);

        // points to the default location of the list file,
        // if you change it here, you must also change it in ImportedAppListFile
        File underlyingFile = Common.getInstance().getAndCreateAppDataPathAsFile().toPath().resolve(AppConfig.getInstance().getRemoteConfig().getValue("importedAppListFileName")).toFile();
        FileUtils.writeStringToFile(underlyingFile, getFileContent(foklauncherFiles, false, false), Charset.forName("UTF-8"));

        ImportedAppListFile importedAppListFile = new ImportedAppListFile();
        Assert.assertEquals(underlyingFile.getAbsolutePath(), importedAppListFile.getFileName());
        for (App app : importedAppListFile.getAppList()) {
            Assert.assertTrue(expectedApps.contains(app));
        }
    }

    private AppList getAppListFromFoklauncherFileList(List<FoklauncherFile> foklauncherFiles) throws IOException {
        AppList res = new AppList();
        for (FoklauncherFile foklauncherFile : foklauncherFiles) {
            res.add(new App(foklauncherFile.getSourceFile()));
        }
        return res;
    }

    private List<FoklauncherFile> generateFokLauncherFiles() throws IOException {
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

        return generateFokLauncherFiles(mvnCoordinatesList, names, changelogURLs, additionalInfoURLs);
    }

    private List<FoklauncherFile> generateFokLauncherFiles(List<MVNCoordinates> mvnCoordinatesList, List<String> names, List<URL> changelogURLs, List<URL> additionalInfoURLs) throws IOException {
        List<FoklauncherFile> res = new ArrayList<>(mvnCoordinatesList.size());
        for (int i = 0; i < mvnCoordinatesList.size(); i++) {
            String fileSuffix = "";
            if (mvnCoordinatesList.get(i).getClassifier() != null) {
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

    private String getFileContent(List<FoklauncherFile> foklauncherFiles, boolean skipModelVersion, boolean skipAppList) {
        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<fokLauncher>\n");

        if (!skipModelVersion)
            res.append("  <modelVersion />\n");

        if (!skipAppList) {
            res.append("  <importedApps>\n");

            for (FoklauncherFile foklauncherFile : foklauncherFiles) {
                res.append("    <app>\n")
                        .append("      <fileName>").append(foklauncherFile.getSourceFile().getAbsolutePath()).append("</fileName>\n")
                        .append("    </app>\n");
            }

            res.append("  </importedApps>\n");
        }
        res.append("</fokLauncher>\n");
        return res.toString();
    }
}

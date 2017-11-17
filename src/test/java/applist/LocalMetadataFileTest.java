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


import com.github.vatbub.common.core.logging.FOKLogger;
import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.VersionList;
import config.TestSuperClass;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class LocalMetadataFileTest extends TestSuperClass {
    private static final String groupId = "com.github.vatbub";
    private static final String artifactId = "testArtifact";
    private static final Version version0 = new Version("0.0.1");
    private static final Version version1 = new Version("0.0.2");
    private static final Version version2 = new Version("0.0.3");
    private static final Version snapshotVersion0 = new Version("0.0.1-SNAPSHOT", "1", "20171028.144750");
    private static final Version snapshotVersion1 = new Version("0.0.1-SNAPSHOT", "2", "20171028.154750");
    private static final Version snapshotVersion2 = new Version("0.0.1-SNAPSHOT", "3", "20171028.164750");

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void saveTest() throws IOException {
        VersionList versions = new VersionList();
        versions.add(version0);
        versions.add(version1);
        versions.add(version2);
        versions.add(snapshotVersion0);
        versions.add(snapshotVersion1);
        versions.add(snapshotVersion2);

        LocalMetadataFile localMetadataFile = new LocalMetadataFile();
        localMetadataFile.setMvnCoordinates(new MVNCoordinates(groupId, artifactId));
        localMetadataFile.setVersionList(versions);

        File testFile = temporaryFolder.newFile("localMetadataFileToSaveTo.xml");
        localMetadataFile.saveFile(testFile);

        String result = FileUtils.readFileToString(testFile, "UTF-8");
        FOKLogger.info(LocalMetadataFileTest.class.getName(), "Result is:\n" + result);

        Assert.assertTrue(result.contains(groupId));
        Assert.assertTrue(result.contains(artifactId));

        for (Version version : versions) {
            FOKLogger.info(LocalMetadataFileTest.class.getName(), "Tested version: " + version.getVersion());
            Assert.assertTrue(result.contains(version.getVersion()));
            if (version.isSnapshot()) {
                FOKLogger.info(LocalMetadataFileTest.class.getName(), "Build number: " + version.getBuildNumber() + ", Timestamp: " + version.getTimestamp());
                Assert.assertTrue(result.contains(version.getBuildNumber()));
                Assert.assertTrue(result.contains(version.getTimestamp()));
            }
        }
    }

    @Test
    public void readFileTest() throws IOException, JDOMException {
        VersionList versions = new VersionList();
        versions.add(version0);
        versions.add(version1);
        versions.add(version2);
        versions.add(snapshotVersion0);
        versions.add(snapshotVersion1);
        versions.add(snapshotVersion2);

        File testFile = temporaryFolder.newFile("localMetadataFileToRead.xml");
        FileUtils.writeStringToFile(testFile, getFileContent(versions), Charset.forName("UTF-8"));

        LocalMetadataFile localMetadataFile = new LocalMetadataFile(testFile);

        Assert.assertEquals(groupId, localMetadataFile.getMvnCoordinates().getGroupId());
        Assert.assertEquals(artifactId, localMetadataFile.getMvnCoordinates().getArtifactId());

        for (Version version : versions) {
            FOKLogger.info(LocalMetadataFileTest.class.getName(), "Tested version: " + version.getVersion());
            Assert.assertTrue(localMetadataFile.getVersionList().contains(version));
        }
    }

    private String getFileContent(VersionList versions) {
        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<artifactInfo>\n" +
                "  <groupId>" + groupId + "</groupId>\n" +
                "  <artifactId>" + artifactId + "</artifactId>\n" +
                "  <versions>\n");

        for (Version version : versions) {
            res.append("    <version>\n")
                    .append("      <version>").append(version.getVersion()).append("</version>\n")
                    .append("      <buildNumber>").append(version.getBuildNumber()).append("</buildNumber>\n")
                    .append("      <timestamp>").append(version.getTimestamp()).append("</timestamp>\n")
                    .append("    </version>\n");
        }

        res.append("  </versions>\n")
                .append("</artifactInfo>");
        return res.toString();
    }
}
